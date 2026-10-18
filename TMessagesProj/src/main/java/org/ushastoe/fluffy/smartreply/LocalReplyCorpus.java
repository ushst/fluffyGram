package org.ushastoe.fluffy.smartreply;

import android.text.TextUtils;
import android.util.SparseArray;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONObject;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * "Their message → my next reply" pairs mined from the locally cached history of
 * private chats. Cached in private, backup-excluded storage; secret chats, bots, forwards and media are skipped.
 * Accessed only from the engine thread.
 */
final class LocalReplyCorpus {

    private static final int SCAN_LIMIT = 6000;
    private static final int MAX_CONTEXT_CHARS = 200;
    private static final int MAX_REPLY_CHARS = 48;
    private static final int MAX_REPLY_WORDS = 6;
    private static final int MAX_GAP_SECONDS = 6 * 60 * 60;
    private static final long REBUILD_INTERVAL_MS = 20 * 60 * 1000L;
    private static final int MIN_PAIRS = 20;

    private static final int CACHE_VERSION = 1;
    private static final int MAX_CACHE_BYTES = 8 * 1024 * 1024;
    private static final String CACHE_PREFIX = "smart_reply_history_";
    private final SparseArray<Long> snapshotOwners = new SparseArray<>();

    private final SparseArray<Snapshot> snapshots = new SparseArray<>();

    /** Returns null while the corpus is too small to be useful. */
    Snapshot get(int account) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        if (userId <= 0) {
            snapshots.remove(account);
            snapshotOwners.remove(account);
            return null;
        }
        Long owner = snapshotOwners.get(account);
        Snapshot snapshot = owner != null && owner == userId ? snapshots.get(account) : null;
        if (snapshot == null) {
            snapshot = load(account, userId);
        }
        long now = System.currentTimeMillis();
        if (snapshot == null || now < snapshot.builtAt || now - snapshot.builtAt > REBUILD_INTERVAL_MS) {
            Snapshot rebuilt = build(account, now);
            if (rebuilt != null) {
                snapshot = rebuilt;
                save(account, userId, snapshot);
            }
        }
        if (snapshot != null) {
            snapshots.put(account, snapshot);
            snapshotOwners.put(account, userId);
        }
        return snapshot != null && snapshot.index.size() >= MIN_PAIRS ? snapshot : null;
    }

    /** Read the existing corpus for inspection without mining or rebuilding history. */
    Snapshot peek(int account) {
        long userId = UserConfig.getInstance(account).getClientUserId();
        if (userId <= 0) {
            return null;
        }
        Long owner = snapshotOwners.get(account);
        Snapshot snapshot = owner != null && owner == userId ? snapshots.get(account) : null;
        if (snapshot == null) {
            snapshot = load(account, userId);
        }
        return snapshot;
    }

    void clear() {
        snapshots.clear();
        snapshotOwners.clear();
        File[] files = ApplicationLoader.applicationContext.getNoBackupFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().startsWith(CACHE_PREFIX) && !file.delete()) {
                    SmartReplyLog.w("could not delete local corpus cache");
                }
            }
        }
    }

    private static AtomicFile cacheFile(int account, long userId) {
        return new AtomicFile(new File(ApplicationLoader.applicationContext.getNoBackupFilesDir(),
                CACHE_PREFIX + account + "_" + userId + ".json"));
    }

    private static Snapshot load(int account, long userId) {
        AtomicFile file = cacheFile(account, userId);
        // AtomicFile.readFully also recovers an interrupted write from its backup.
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists()) {
            return null;
        }
        try {
            if (file.getBaseFile().length() > MAX_CACHE_BYTES) {
                throw new java.io.IOException("local corpus cache too large");
            }
            byte[] bytes = file.readFully();
            if (bytes.length > MAX_CACHE_BYTES) {
                throw new java.io.IOException("local corpus cache too large");
            }
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (root.getInt("version") != CACHE_VERSION || root.getLong("userId") != userId) {
                throw new java.io.IOException("local corpus cache version or owner mismatch");
            }
            long builtAt = root.getLong("builtAt");
            JSONArray entries = root.getJSONArray("pairs");
            if (builtAt <= 0 || entries.length() > SCAN_LIMIT) {
                throw new java.io.IOException("invalid local corpus cache");
            }
            PairIndex index = new PairIndex("local");
            ArrayList<CorpusPair> pairs = new ArrayList<>();
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.getJSONObject(i);
                String context = entry.getString("context");
                String reply = entry.getString("reply");
                long dialogId = entry.getLong("dialogId");
                int date = entry.getInt("date");
                if (context.isEmpty() || context.length() > MAX_CONTEXT_CHARS
                        || reply.isEmpty() || !isShortReply(reply)
                        || !DialogObject.isUserDialog(dialogId)) {
                    throw new java.io.IOException("invalid local corpus pair");
                }
                index.add(context, reply, 1f, "history", dialogId);
                pairs.add(new CorpusPair(context, reply, dialogId, date));
            }
            SmartReplyLog.d("local corpus restored account=" + account + " pairs=" + index.size());
            return new Snapshot(index, pairs, builtAt);
        } catch (Exception e) {
            SmartReplyLog.e("local corpus cache load failed; rebuilding", e);
            file.delete();
            return null;
        }
    }

    private static void save(int account, long userId, Snapshot snapshot) {
        AtomicFile file = cacheFile(account, userId);
        FileOutputStream stream = null;
        try {
            JSONObject root = new JSONObject();
            root.put("version", CACHE_VERSION);
            root.put("userId", userId);
            root.put("builtAt", snapshot.builtAt);
            JSONArray pairs = new JSONArray();
            for (CorpusPair pair : snapshot.pairs) {
                JSONObject entry = new JSONObject();
                entry.put("context", pair.context);
                entry.put("reply", pair.reply);
                entry.put("dialogId", pair.dialogId);
                entry.put("date", pair.date);
                pairs.put(entry);
            }
            root.put("pairs", pairs);
            stream = file.startWrite();
            stream.write(root.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
            SmartReplyLog.d("local corpus saved account=" + account + " pairs=" + snapshot.pairs.size());
        } catch (Exception e) {
            if (stream != null) {
                file.failWrite(stream);
            }
            SmartReplyLog.e("local corpus cache save failed", e);
        }
    }

    private static Snapshot build(int account, long now) {
        long started = System.currentTimeMillis();
        List<Row> rows = loadRows(account);
        if (rows == null) {
            return null; // Keep the previous snapshot if Telegram storage is unavailable.
        }
        Map<Long, List<Row>> byDialog = new HashMap<>();
        for (Row row : rows) {
            List<Row> list = byDialog.get(row.dialogId);
            if (list == null) {
                list = new ArrayList<>();
                byDialog.put(row.dialogId, list);
            }
            list.add(row);
        }

        MessagesController controller = MessagesController.getInstance(account);
        PairIndex index = new PairIndex("local");
        ArrayList<CorpusPair> pairs = new ArrayList<>();
        for (Map.Entry<Long, List<Row>> entry : byDialog.entrySet()) {
            TLRPC.User user = controller.getUser(entry.getKey());
            if (user != null && user.bot) {
                continue;
            }
            List<Row> list = entry.getValue();
            Collections.reverse(list);
            for (int i = 1; i < list.size(); i++) {
                Row prev = list.get(i - 1);
                Row cur = list.get(i);
                if (!cur.out || prev.out || cur.date - prev.date > MAX_GAP_SECONDS) {
                    continue;
                }
                if (!isShortReply(cur.text) || prev.text.length() > MAX_CONTEXT_CHARS) {
                    continue;
                }
                index.add(prev.text, cur.text, 1f, "history", entry.getKey());
                pairs.add(new CorpusPair(SmartReplyText.clean(prev.text), cur.text, entry.getKey(), cur.date));
            }
        }
        pairs.sort((a, b) -> Integer.compare(b.date, a.date));
        SmartReplyLog.d(String.format(Locale.US, "local corpus account=%d rows=%d dialogs=%d pairs=%d in %dms",
                account, rows.size(), byDialog.size(), index.size(), System.currentTimeMillis() - started));
        return new Snapshot(index, pairs, now);
    }

    private static boolean isShortReply(String text) {
        return text.length() <= MAX_REPLY_CHARS
                && SmartReplyText.wordCount(text) <= MAX_REPLY_WORDS
                && !text.startsWith("/");
    }

    /** Newest first. Runs the query on the storage queue and waits for it. */
    private static List<Row> loadRows(int account) {
        MessagesStorage storage = MessagesStorage.getInstance(account);
        long selfId = UserConfig.getInstance(account).getClientUserId();
        ArrayList<Row> rows = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        boolean[] success = {false};
        storage.getStorageQueue().postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = storage.getDatabase().queryFinalized(String.format(Locale.US,
                        "SELECT uid, out, date, data FROM messages_v2 WHERE uid > 0 AND uid != %d ORDER BY date DESC LIMIT %d",
                        selfId, SCAN_LIMIT));
                while (cursor.next()) {
                    long dialogId = cursor.longValue(0);
                    if (!DialogObject.isUserDialog(dialogId)) {
                        continue;
                    }
                    NativeByteBuffer data = cursor.byteBufferValue(3);
                    if (data == null) {
                        continue;
                    }
                    TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                    data.reuse();
                    String text = usableText(message);
                    if (text != null) {
                        rows.add(new Row(dialogId, cursor.intValue(1) != 0, cursor.intValue(2), text));
                    }
                }
                success[0] = true;
            } catch (Throwable e) {
                SmartReplyLog.e("local corpus query failed", e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
                latch.countDown();
            }
        });
        try {
            if (!latch.await(15, TimeUnit.SECONDS)) {
                SmartReplyLog.w("local corpus query timed out");
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return success[0] ? rows : null;
    }

    private static String usableText(TLRPC.Message message) {
        if (message == null || message instanceof TLRPC.TL_messageService
                || message.fwd_from != null || message.via_bot_id != 0) {
            return null;
        }
        if (message.media != null && !(message.media instanceof TLRPC.TL_messageMediaEmpty)
                && !(message.media instanceof TLRPC.TL_messageMediaWebPage)) {
            return null;
        }
        String text = message.message;
        return TextUtils.isEmpty(text) ? null : text.trim();
    }

    private static final class Row {
        final long dialogId;
        final boolean out;
        final int date;
        final String text;

        Row(long dialogId, boolean out, int date, String text) {
            this.dialogId = dialogId;
            this.out = out;
            this.date = date;
            this.text = text;
        }
    }

    static final class Snapshot {
        final PairIndex index;
        /** Newest first. */
        final List<CorpusPair> pairs;
        final long builtAt;

        Snapshot(PairIndex index, List<CorpusPair> pairs, long builtAt) {
            this.index = index;
            this.pairs = pairs;
            this.builtAt = builtAt;
        }
    }

    static final class CorpusPair {
        /** Already cleaned. */
        final String context;
        final String reply;
        final long dialogId;
        final int date;

        CorpusPair(String context, String reply, long dialogId, int date) {
            this.context = context;
            this.reply = reply;
            this.dialogId = dialogId;
            this.date = date;
        }
    }
}
