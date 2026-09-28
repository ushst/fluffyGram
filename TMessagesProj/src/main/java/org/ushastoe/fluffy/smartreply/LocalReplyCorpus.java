package org.ushastoe.fluffy.smartreply;

import android.text.TextUtils;
import android.util.SparseArray;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

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
 * private chats. Kept in memory only; secret chats, bots, forwards and media are skipped.
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

    private final SparseArray<Snapshot> snapshots = new SparseArray<>();

    /** Returns null while the corpus is too small to be useful. */
    Snapshot get(int account) {
        Snapshot snapshot = snapshots.get(account);
        long now = System.currentTimeMillis();
        if (snapshot == null || now - snapshot.builtAt > REBUILD_INTERVAL_MS) {
            snapshot = build(account, now);
            snapshots.put(account, snapshot);
        }
        return snapshot.index.size() >= MIN_PAIRS ? snapshot : null;
    }

    void clear() {
        snapshots.clear();
    }

    private static Snapshot build(int account, long now) {
        long started = System.currentTimeMillis();
        List<Row> rows = loadRows(account);
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
                return Collections.emptyList();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        }
        return rows;
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
