package org.ushastoe.fluffy.smartreply;

import android.content.Context;

import org.telegram.messenger.DispatchQueue;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Semantic retrieval with rubert-tiny2. Curated contexts come pre-embedded from assets;
 * local-history contexts are embedded lazily on a background thread (memory only).
 */
final class EmbeddingRetrievalProvider implements SmartReplyProvider {

    private static final int MAX_LOCAL_VECTORS = 1500;
    private static final int PUBLISH_EVERY = 150;
    private static final float OTHER_DIALOG_FACTOR = 0.8f;

    private final LocalReplyCorpus corpus;
    private final DispatchQueue embedQueue = new DispatchQueue("fluffySmartReplyEmbed");
    private final ConcurrentHashMap<String, float[]> localVectors = new ConcurrentHashMap<>();

    private TinyBertEncoder encoder;
    private EmbeddingIndex curated;
    private boolean failed;

    /** Bumped whenever queued background work must be abandoned. */
    private volatile int generation;
    private volatile EmbeddingIndex localIndex;
    private volatile int localIndexAccount = -1;
    private long embeddedSnapshotAt;
    private int embeddedAccount = -1;

    EmbeddingRetrievalProvider(LocalReplyCorpus corpus) {
        this.corpus = corpus;
    }

    @Override
    public String id() {
        return "semantic";
    }

    @Override
    public boolean isEnabled() {
        return AppearanceSettingsHook.isSmartReplySemanticEnabled() && EmbeddingModelStore.isReady();
    }

    @Override
    public boolean prepare(Context app, int account) {
        if (failed) {
            return false;
        }
        if (encoder == null) {
            try {
                long started = System.currentTimeMillis();
                if (!CuratedReplyDb.getInstance().ensureLoaded(app)) {
                    return false;
                }
                TinyBertEncoder enc = new TinyBertEncoder(EmbeddingModelStore.modelFile(), EmbeddingModelStore.vocabFile());
                curated = buildCuratedIndex(app);
                encoder = enc;
                SmartReplyLog.d("semantic ready curated=" + curated.size() + " in " + (System.currentTimeMillis() - started) + "ms");
            } catch (Throwable e) {
                failed = true;
                SmartReplyLog.e("semantic init failed", e);
                return false;
            }
        }
        if (AppearanceSettingsHook.isSmartReplyHistoryEnabled()) {
            LocalReplyCorpus.Snapshot snapshot = corpus.get(account);
            if (snapshot != null && (snapshot.builtAt != embeddedSnapshotAt || account != embeddedAccount)) {
                embeddedSnapshotAt = snapshot.builtAt;
                embeddedAccount = account;
                scheduleLocalEmbedding(account, snapshot.pairs);
            }
        }
        return true;
    }

    @Override
    public List<String> suggest(SmartReplyContext ctx, int limit) {
        float[] query = encode(ctx.cleanedIncoming);
        EmbeddingIndex.Stats background = curated.stats(query);
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        EmbeddingIndex local = localIndex;
        if (local != null && localIndexAccount == ctx.account && AppearanceSettingsHook.isSmartReplyHistoryEnabled()) {
            addAll(out, local.query(query, background, limit, ctx.dialogId, OTHER_DIALOG_FACTOR), limit);
        }
        if (out.size() < limit) {
            addAll(out, curated.query(query, background, limit, ctx.dialogId, 1f), limit);
        }
        return out.isEmpty() ? Collections.emptyList() : new ArrayList<>(out.values());
    }

    /** Engine thread. Drops mined vectors, e.g. after history consent is revoked. */
    void clearLocal() {
        generation++;
        localIndex = null;
        localIndexAccount = -1;
        embeddedSnapshotAt = 0;
        embeddedAccount = -1;
        localVectors.clear();
    }

    /** Engine thread. Frees the model after the feature is turned off. */
    void release() {
        clearLocal();
        synchronized (this) {
            encoder = null;
        }
        curated = null;
        failed = false;
    }

    private synchronized float[] encode(String text) {
        return encoder.encode(text);
    }

    private synchronized boolean hasEncoder() {
        return encoder != null;
    }

    private static void addAll(LinkedHashMap<String, String> out, List<String> chips, int limit) {
        for (String chip : chips) {
            if (out.size() >= limit) {
                return;
            }
            out.putIfAbsent(SmartReplyText.normalizeReply(chip), chip);
        }
    }

    private EmbeddingIndex buildCuratedIndex(Context app) throws java.io.IOException {
        HashMap<Long, float[]> vectors = CuratedEmbeddings.load(app);
        EmbeddingIndex index = new EmbeddingIndex("curated");
        int missing = 0;
        for (CuratedReplyDb.Entry entry : CuratedReplyDb.getInstance().entries()) {
            float[] v = vectors.get(CuratedEmbeddings.hash(entry.context));
            if (v == null) {
                missing++;
                continue;
            }
            index.add(v, entry.reply, entry.weight, 0);
        }
        if (missing > 0) {
            SmartReplyLog.w("semantic: " + missing + " curated contexts lack vectors — rerun build_smart_reply_embeddings.py");
        }
        return index;
    }

    private void scheduleLocalEmbedding(int account, List<LocalReplyCorpus.CorpusPair> pairs) {
        int gen = ++generation;
        List<LocalReplyCorpus.CorpusPair> todo = pairs.subList(0, Math.min(MAX_LOCAL_VECTORS, pairs.size()));
        embedQueue.postRunnable(() -> {
            long started = System.currentTimeMillis();
            int computed = 0;
            for (LocalReplyCorpus.CorpusPair pair : todo) {
                if (gen != generation || !hasEncoder()) {
                    return;
                }
                if (localVectors.containsKey(pair.context)) {
                    continue;
                }
                float[] v;
                synchronized (this) {
                    if (encoder == null) {
                        return;
                    }
                    v = encoder.encode(pair.context);
                }
                localVectors.put(pair.context, v);
                if (++computed % PUBLISH_EVERY == 0) {
                    publishLocal(gen, account, todo);
                }
            }
            publishLocal(gen, account, todo);
            SmartReplyLog.d("semantic local vectors +" + computed + " total=" + localVectors.size()
                    + " in " + (System.currentTimeMillis() - started) + "ms");
        });
    }

    private void publishLocal(int gen, int account, List<LocalReplyCorpus.CorpusPair> pairs) {
        EmbeddingIndex index = new EmbeddingIndex("local");
        for (LocalReplyCorpus.CorpusPair pair : pairs) {
            float[] v = localVectors.get(pair.context);
            if (v != null) {
                index.add(v, pair.reply, 1f, pair.dialogId);
            }
        }
        if (gen == generation) {
            localIndexAccount = account;
            localIndex = index;
        }
    }
}
