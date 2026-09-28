package org.ushastoe.fluffy.smartreply;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Downloads cointegrated/rubert-tiny2 (MIT) from Hugging Face at a pinned revision into
 * private storage and verifies SHA-256. Kept in sync with scripts/build_smart_reply_embeddings.py.
 */
public final class EmbeddingModelStore {

    public enum State { NONE, DOWNLOADING, READY, FAILED }

    private static final String REVISION = "e8ed3b0c8bbf4fb6984c3de043bf7d2f4e5969ae";
    private static final String BASE_URL = "https://huggingface.co/cointegrated/rubert-tiny2/resolve/" + REVISION + "/";
    private static final String MODEL = "model.safetensors";
    private static final String VOCAB = "vocab.txt";
    private static final String MODEL_SHA256 = "26ebb6db2a68593c54c74902d7a74f332da66297693f965cc9f1b0af4abf3894";
    private static final String VOCAB_SHA256 = "f056a69b097422652053bf87565c35543e5d81540ca4b7dddd28de4157a969e0";
    private static final long MODEL_BYTES = 117_529_600L;
    private static final long VOCAB_BYTES = 1_080_667L;
    public static final int DOWNLOAD_MB = (int) ((MODEL_BYTES + VOCAB_BYTES) / (1024 * 1024));

    private static volatile State state;
    private static volatile int progressPercent;
    private static volatile Runnable listener;
    private static Thread worker;

    private EmbeddingModelStore() {
    }

    static File dir() {
        return new File(ApplicationLoader.applicationContext.getFilesDir(), "fluffy/rubert-tiny2-" + REVISION.substring(0, 7));
    }

    static File modelFile() {
        return new File(dir(), MODEL);
    }

    static File vocabFile() {
        return new File(dir(), VOCAB);
    }

    public static State getState() {
        State s = state;
        if (s == null) {
            s = modelFile().length() == MODEL_BYTES && vocabFile().length() == VOCAB_BYTES ? State.READY : State.NONE;
            state = s;
        }
        return s;
    }

    public static boolean isReady() {
        return getState() == State.READY;
    }

    public static int getProgressPercent() {
        return progressPercent;
    }

    /** Invoked on the UI thread whenever state or progress changes. */
    public static void setListener(Runnable r) {
        listener = r;
    }

    public static synchronized void download() {
        if (getState() == State.READY || worker != null) {
            return;
        }
        setState(State.DOWNLOADING, 0);
        worker = new Thread(() -> {
            boolean ok = false;
            try {
                File dir = dir();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IOException("cannot create " + dir);
                }
                if (dir.getUsableSpace() < MODEL_BYTES + VOCAB_BYTES + 32L * 1024 * 1024) {
                    throw new IOException("not enough free space");
                }
                fetch(VOCAB, VOCAB_SHA256, VOCAB_BYTES, 0, VOCAB_BYTES);
                fetch(MODEL, MODEL_SHA256, MODEL_BYTES, VOCAB_BYTES, MODEL_BYTES);
                ok = true;
            } catch (Throwable e) {
                SmartReplyLog.e("model download failed", e);
            }
            synchronized (EmbeddingModelStore.class) {
                worker = null;
            }
            setState(ok ? State.READY : State.FAILED, ok ? 100 : 0);
        }, "fluffyEmbeddingModel");
        worker.start();
    }

    /** Stops a running download (best effort) and removes model files. */
    public static synchronized void delete() {
        if (worker != null) {
            worker.interrupt();
        }
        File[] files = dir().listFiles();
        if (files != null) {
            for (File f : files) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        }
        setState(State.NONE, 0);
    }

    private static void fetch(String name, String sha256, long size, long doneBefore, long partTotal) throws Exception {
        File target = new File(dir(), name);
        if (target.length() == size && sha256.equals(sha256Of(target))) {
            return;
        }
        File part = new File(dir(), name + ".part");
        long total = MODEL_BYTES + VOCAB_BYTES;
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + name).openConnection();
        conn.setConnectTimeout(20_000);
        conn.setReadTimeout(30_000);
        conn.setInstanceFollowRedirects(true);
        try {
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code + " for " + name);
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long read = 0;
            try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(part)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                    out.write(buf, 0, n);
                    digest.update(buf, 0, n);
                    read += n;
                    if (read > partTotal) {
                        throw new IOException("unexpected size for " + name);
                    }
                    int percent = (int) ((doneBefore + read) * 100 / total);
                    if (percent != progressPercent) {
                        setState(State.DOWNLOADING, percent);
                    }
                }
            }
            if (read != size || !sha256.equals(hex(digest.digest()))) {
                throw new IOException("checksum mismatch for " + name);
            }
            if (!part.renameTo(target)) {
                throw new IOException("rename failed for " + name);
            }
        } finally {
            conn.disconnect();
            //noinspection ResultOfMethodCallIgnored
            part.delete();
        }
    }

    private static String sha256Of(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) >= 0) {
                digest.update(buf, 0, n);
            }
        }
        return hex(digest.digest());
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02x", b));
        }
        return sb.toString();
    }

    private static void setState(State s, int percent) {
        state = s;
        progressPercent = percent;
        Runnable l = listener;
        if (l != null) {
            AndroidUtilities.runOnUIThread(l);
        }
    }
}
