package org.ushastoe.fluffy.smartreply;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Context → reply pairs scored by token overlap + char-trigram Jaccard. */
final class PairIndex {

    private static final float MIN_PAIR_SCORE = 0.42f;
    private static final float MIN_SINGLE_TOKEN_CHAR_SCORE = 0.18f;
    private static final float MIN_SINGLE_TOKEN_COMBINED = 0.62f;
    private static final int MAX_WORDS_FOR_PAIRS = 12;

    private final String name;
    private final List<Pair> pairs = new ArrayList<>();
    private final Map<String, List<Integer>> tokenIndex = new HashMap<>();

    PairIndex(String name) {
        this.name = name;
    }

    int size() {
        return pairs.size();
    }

    /** @param dialogId 0 for pairs not bound to a chat. */
    void add(String context, String reply, float weight, String source, long dialogId) {
        String ctx = SmartReplyText.clean(context);
        String rep = reply == null ? "" : reply.trim();
        if (TextUtils.isEmpty(ctx) || TextUtils.isEmpty(rep) || SmartReplyText.isBlocked(rep)) {
            return;
        }
        Pair pair = new Pair();
        pair.reply = rep;
        pair.source = source;
        pair.weight = weight;
        pair.dialogId = dialogId;
        pair.tokens = SmartReplyText.contentTokens(ctx);
        pair.trigrams = SmartReplyText.charTrigrams(ctx);
        if (pair.tokens.isEmpty()) {
            return;
        }
        int idx = pairs.size();
        pairs.add(pair);
        for (String tok : pair.tokens) {
            List<Integer> list = tokenIndex.get(tok);
            if (list == null) {
                list = new ArrayList<>();
                tokenIndex.put(tok, list);
            }
            list.add(idx);
        }
    }

    /**
     * @param msg            already {@link SmartReplyText#clean cleaned} incoming text
     * @param dialogId       current chat; pairs from other chats are scaled by {@code otherDialogFactor}
     */
    List<String> query(String msg, int topK, long dialogId, float otherDialogFactor) {
        if (pairs.isEmpty() || SmartReplyText.wordCount(msg) > MAX_WORDS_FOR_PAIRS) {
            return Collections.emptyList();
        }
        Set<String> qTokens = SmartReplyText.contentTokens(msg);
        if (qTokens.isEmpty() || SmartReplyText.allWeak(qTokens)) {
            return Collections.emptyList();
        }

        Set<Integer> candidates = new HashSet<>();
        for (String t : qTokens) {
            if (SmartReplyText.WEAK.contains(t)) {
                continue;
            }
            List<Integer> idxs = tokenIndex.get(t);
            if (idxs != null) {
                candidates.addAll(idxs);
            }
        }
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> qTri = SmartReplyText.charTrigrams(msg);
        int qStrong = SmartReplyText.nonWeakCount(qTokens);
        Map<String, Float> votes = new HashMap<>();
        Map<String, String> display = new HashMap<>();
        float best = 0f;
        Pair bestPair = null;

        for (int idx : candidates) {
            Pair pair = pairs.get(idx);
            int strongOverlap = 0;
            for (String t : qTokens) {
                if (pair.tokens.contains(t) && SmartReplyText.isStrong(t)) {
                    strongOverlap++;
                }
            }
            if (strongOverlap == 0) {
                continue;
            }
            float wordScore = strongOverlap / (float) qStrong;
            float charScore = SmartReplyText.jaccard(qTri, pair.trigrams);
            if (!overlapOk(qTokens, pair.tokens, wordScore, strongOverlap, charScore)) {
                continue;
            }
            float weight = pair.weight;
            if (pair.dialogId != 0 && pair.dialogId != dialogId) {
                weight *= otherDialogFactor;
            }
            float combined = (0.68f * wordScore + 0.32f * charScore) * weight;
            if (combined < MIN_PAIR_SCORE) {
                continue;
            }
            String key = SmartReplyText.normalizeReply(pair.reply);
            votes.put(key, votes.getOrDefault(key, 0f) + combined);
            display.putIfAbsent(key, pair.reply);
            if (combined > best) {
                best = combined;
                bestPair = pair;
            }
        }
        if (bestPair == null) {
            return Collections.emptyList();
        }

        List<Map.Entry<String, Float>> ranked = new ArrayList<>(votes.entrySet());
        ranked.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, Float> e : ranked) {
            if (e.getValue() < MIN_PAIR_SCORE * 0.85f) {
                continue;
            }
            out.add(display.get(e.getKey()));
            if (out.size() >= topK) {
                break;
            }
        }
        SmartReplyLog.d(name + " hit for \"" + SmartReplyText.preview(msg) + "\" best=\""
                + SmartReplyText.preview(bestPair.reply) + "\" src=" + bestPair.source
                + " score=" + String.format(Locale.US, "%.2f", best) + " → " + out);
        return new ArrayList<>(out);
    }

    private static boolean overlapOk(Set<String> q, Set<String> c, float ratio, int strongOverlap, float charScore) {
        int qStrong = SmartReplyText.nonWeakCount(q);
        if (qStrong >= 2) {
            int minOverlap = qStrong <= 2 ? 2 : Math.max(2, (qStrong + 1) / 2);
            if (strongOverlap < minOverlap) {
                return false;
            }
        }
        if (strongOverlap >= 2 && ratio >= 0.40f) {
            return true;
        }
        if (strongOverlap == 1) {
            if (qStrong >= 2) {
                return false;
            }
            for (String t : q) {
                if (c.contains(t) && !SmartReplyText.WEAK.contains(t) && t.length() > 3) {
                    return ratio >= MIN_SINGLE_TOKEN_COMBINED && charScore >= MIN_SINGLE_TOKEN_CHAR_SCORE;
                }
            }
            return false;
        }
        return ratio >= 0.55f && charScore >= 0.12f;
    }

    private static final class Pair {
        String reply;
        String source;
        float weight;
        long dialogId;
        Set<String> tokens;
        Set<String> trigrams;
    }
}
