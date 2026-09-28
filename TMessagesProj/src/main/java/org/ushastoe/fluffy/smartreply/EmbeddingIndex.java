package org.ushastoe.fluffy.smartreply;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Nearest-neighbour search over context embeddings. rubert-tiny2 gives short phrases a
 * high baseline similarity, so a hit must be either near-identical or an outlier against
 * the curated corpus (z-score). Thresholds were calibrated on hand-picked queries.
 */
final class EmbeddingIndex {

    private static final float MIN_ABSOLUTE = 0.935f;
    private static final float MIN_Z = 4.1f;
    /** Other accepted candidates must stay this close to the best one. */
    private static final float MAX_GAP_FROM_BEST = 0.03f;

    private final String name;
    private final ArrayList<float[]> vectors = new ArrayList<>();
    private final ArrayList<String> replies = new ArrayList<>();
    private final ArrayList<Float> weights = new ArrayList<>();
    private final ArrayList<Long> dialogIds = new ArrayList<>();

    EmbeddingIndex(String name) {
        this.name = name;
    }

    void add(float[] vector, String reply, float weight, long dialogId) {
        vectors.add(vector);
        replies.add(reply);
        weights.add(weight);
        dialogIds.add(dialogId);
    }

    int size() {
        return vectors.size();
    }

    /** Similarity distribution of a query against this index, used as the z-score background. */
    Stats stats(float[] query) {
        int n = vectors.size();
        if (n < 2) {
            return null;
        }
        double sum = 0;
        double sumSq = 0;
        for (int i = 0; i < n; i++) {
            float s = TinyBertEncoder.dot(query, vectors.get(i));
            sum += s;
            sumSq += s * s;
        }
        double mean = sum / n;
        double std = Math.sqrt(Math.max(1e-9, sumSq / n - mean * mean));
        return new Stats((float) mean, (float) std);
    }

    List<String> query(float[] query, Stats background, int topK, long dialogId, float otherDialogFactor) {
        int n = vectors.size();
        if (n == 0 || background == null) {
            return Collections.emptyList();
        }
        float[] sims = new float[n];
        float best = -1f;
        int bestIdx = -1;
        for (int i = 0; i < n; i++) {
            sims[i] = TinyBertEncoder.dot(query, vectors.get(i));
            if (sims[i] > best) {
                best = sims[i];
                bestIdx = i;
            }
        }
        if (!accepted(best, background)) {
            return Collections.emptyList();
        }

        Map<String, Float> votes = new HashMap<>();
        Map<String, String> display = new HashMap<>();
        for (int i = 0; i < n; i++) {
            float s = sims[i];
            if (s < best - MAX_GAP_FROM_BEST || !accepted(s, background)) {
                continue;
            }
            float weight = weights.get(i);
            long pairDialog = dialogIds.get(i);
            if (pairDialog != 0 && pairDialog != dialogId) {
                weight *= otherDialogFactor;
            }
            String key = SmartReplyText.normalizeReply(replies.get(i));
            votes.put(key, votes.getOrDefault(key, 0f) + s * weight);
            display.putIfAbsent(key, replies.get(i));
        }
        List<Map.Entry<String, Float>> ranked = new ArrayList<>(votes.entrySet());
        ranked.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, Float> e : ranked) {
            out.add(display.get(e.getKey()));
            if (out.size() >= topK) {
                break;
            }
        }
        SmartReplyLog.d(String.format(Locale.US, "%s semantic best=%.3f z=%.2f \"%s\" → %s", name, best,
                background.z(best), SmartReplyText.preview(replies.get(bestIdx)), out));
        return new ArrayList<>(out);
    }

    private static boolean accepted(float sim, Stats background) {
        return sim >= MIN_ABSOLUTE || background.z(sim) >= MIN_Z;
    }

    static final class Stats {
        final float mean;
        final float std;

        Stats(float mean, float std) {
            this.mean = mean;
            this.std = std;
        }

        float z(float sim) {
            return (sim - mean) / std;
        }
    }
}
