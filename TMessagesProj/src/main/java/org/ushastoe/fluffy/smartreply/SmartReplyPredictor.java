package org.ushastoe.fluffy.smartreply;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * On-device Smart Reply: intent bank first, then weighted pair retrieval.
 * Pair sources: curated (1.0) &gt; external (0.65) &gt; legacy (0.55).
 */
public final class SmartReplyPredictor {

    private static final String ASSET_PATH = "fluffy/smart_reply_db.json";
    private static final int TOP_K = 3;
    private static final float MIN_PAIR_SCORE = 0.42f;
    private static final float MIN_SINGLE_TOKEN_CHAR_SCORE = 0.18f;
    private static final float MIN_SINGLE_TOKEN_COMBINED = 0.62f;
    private static final int MAX_WORDS_FOR_PAIRS = 12;

    private static final float WEIGHT_CURATED = 1.0f;
    private static final float WEIGHT_EXTERNAL = 0.65f;
    private static final float WEIGHT_LEGACY = 0.55f;

    /** Shown when no intent/pair matches — keeps the bar useful for casual chat. */
    private static final String[][] FALLBACK_SETS = {
            {"Ок", "Ага", "Понял"},
            {"Ок", "Хорошо", "Ясно"},
            {"Ага", "Понял", "Ок"},
            {"Ок", "Ага", "Хорошо"},
    };

    private static final Set<String> STOP = new HashSet<>();
    private static final Set<String> WEAK = new HashSet<>();
    private static final Set<String> BLOCKLIST = new HashSet<>();

    static {
        Collections.addAll(STOP,
                "и", "в", "во", "на", "я", "ты", "он", "она", "мы", "вы",
                "то", "это", "а", "но", "да", "ну", "же", "ли", "бы", "к", "у",
                "с", "со", "о", "об", "от", "по", "из", "за", "для", "или",
                "если", "уже", "еще", "ещё", "мне", "тебе", "меня", "тебя",
                "мой", "моя", "просто", "очень", "про");
        Collections.addAll(WEAK,
                "лег", "лёг", "есть", "надо", "завтра", "сегодня", "тут", "там",
                "будет", "было", "сейчас", "щас", "счас", "хотела", "хотел",
                "написать", "думала", "думал");
        Collections.addAll(BLOCKLIST,
                "?", "!", ".", ",", "-", "*", "+", ")", "(", "ты", "я", "это",
                "пу пу пу", "пупупу", "бля", "блять");
    }

    private static volatile SmartReplyPredictor instance;

    private final List<IntentRule> intents = new ArrayList<>();
    private final List<Pair> pairs = new ArrayList<>();
    private final Map<String, List<Integer>> tokenIndex = new HashMap<>();
    private volatile boolean loaded;
    private volatile boolean loading;
    private final ArrayList<Runnable> readyListeners = new ArrayList<>();

    public static SmartReplyPredictor getInstance() {
        SmartReplyPredictor local = instance;
        if (local == null) {
            synchronized (SmartReplyPredictor.class) {
                local = instance;
                if (local == null) {
                    instance = local = new SmartReplyPredictor();
                }
            }
        }
        return local;
    }

    private SmartReplyPredictor() {
    }

    public boolean isReady() {
        return loaded;
    }

    /** Runs callback once the DB is ready (immediately if already loaded). */
    public void whenReady(Runnable callback) {
        if (callback == null) {
            return;
        }
        if (loaded) {
            callback.run();
            return;
        }
        synchronized (readyListeners) {
            if (loaded) {
                callback.run();
                return;
            }
            readyListeners.add(callback);
        }
    }

    public void preload(Context context) {
        if (loaded || loading) {
            return;
        }
        Context app = resolveAppContext(context);
        if (app == null) {
            return;
        }
        loading = true;
        Utilities.globalQueue.postRunnable(() -> {
            try {
                loadFromAssets(app);
            } finally {
                loading = false;
                notifyReadyListeners();
            }
        });
    }

    public synchronized void ensureLoaded(Context context) {
        if (loaded || loading) {
            return;
        }
        Context app = resolveAppContext(context);
        if (app == null) {
            return;
        }
        loadFromAssets(app);
        notifyReadyListeners();
    }

    private void notifyReadyListeners() {
        ArrayList<Runnable> copy;
        synchronized (readyListeners) {
            if (readyListeners.isEmpty()) {
                return;
            }
            copy = new ArrayList<>(readyListeners);
            readyListeners.clear();
        }
        for (int i = 0; i < copy.size(); i++) {
            try {
                copy.get(i).run();
            } catch (Throwable ignored) {
            }
        }
    }

    private static Context resolveAppContext(Context context) {
        return context != null ? context.getApplicationContext() : ApplicationLoader.applicationContext;
    }

    private synchronized void loadFromAssets(Context app) {
        if (loaded) {
            return;
        }
        try (InputStream in = app.getAssets().open(ASSET_PATH);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder(128 * 1024);
            char[] buf = new char[8192];
            int n;
            while ((n = reader.read(buf)) >= 0) {
                sb.append(buf, 0, n);
            }
            JSONObject root = new JSONObject(sb.toString());

            JSONArray intentsArr = root.optJSONArray("intents");
            if (intentsArr != null) {
                for (int i = 0; i < intentsArr.length(); i++) {
                    JSONObject o = intentsArr.getJSONObject(i);
                    IntentRule rule = new IntentRule();
                    rule.id = o.optString("id");
                    JSONArray patterns = o.optJSONArray("patterns");
                    if (patterns != null) {
                        for (int p = 0; p < patterns.length(); p++) {
                            String raw = patterns.getString(p);
                            try {
                                rule.patterns.add(compileIntentPattern(raw));
                                rule.patternRaw.add(raw);
                            } catch (Throwable ignored) {
                            }
                        }
                    }
                    JSONArray chips = o.optJSONArray("chips");
                    if (chips != null) {
                        for (int c = 0; c < chips.length(); c++) {
                            String chip = chips.optString(c);
                            if (!TextUtils.isEmpty(chip) && !isBlocked(chip)) {
                                rule.chips.add(chip);
                            }
                        }
                    }
                    if (!rule.patterns.isEmpty() && !rule.chips.isEmpty()) {
                        intents.add(rule);
                    }
                }
            }

            JSONArray pairsArr = root.optJSONArray("pairs");
            if (pairsArr != null) {
                for (int i = 0; i < pairsArr.length(); i++) {
                    JSONObject o = pairsArr.getJSONObject(i);
                    String ctx = clean(o.optString("context"));
                    String reply = o.optString("reply").trim();
                    if (TextUtils.isEmpty(ctx) || TextUtils.isEmpty(reply) || isBlocked(reply)) {
                        continue;
                    }
                    Pair pair = new Pair();
                    pair.context = ctx;
                    pair.reply = reply;
                    pair.source = o.optString("source", "legacy");
                    pair.weight = weightForSource(pair.source);
                    pair.tokens = contentTokens(ctx);
                    pair.trigrams = charTrigrams(ctx);
                    if (pair.tokens.isEmpty()) {
                        continue;
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
            }
            loaded = true;
            SmartReplyLog.d("loaded intents=" + intents.size() + " pairs=" + pairs.size());
            FileLog.d("SmartReply loaded intents=" + intents.size() + " pairs=" + pairs.size());
        } catch (Throwable e) {
            SmartReplyLog.e("failed to load db", e);
            FileLog.e("SmartReply failed to load db", e);
        }
    }

    public List<String> getSuggestions(String incoming) {
        if (TextUtils.isEmpty(incoming) || !loaded) {
            return Collections.emptyList();
        }
        String msg = clean(incoming);
        if (TextUtils.isEmpty(msg)) {
            return Collections.emptyList();
        }

        List<String> intentChips = matchIntents(msg);
        if (!intentChips.isEmpty()) {
            SmartReplyLog.d("intent hit for \"" + preview(msg) + "\" → " + intentChips);
            return intentChips;
        }

        int words = msg.split("\\s+").length;
        if (words > MAX_WORDS_FOR_PAIRS) {
            SmartReplyLog.d("pairs skipped: words=" + words + " → fallback");
            return fallbackChips(msg);
        }

        Set<String> qTokens = contentTokens(msg);
        if (qTokens.isEmpty() || allWeak(qTokens)) {
            SmartReplyLog.d("pairs skipped: weak/empty tokens → fallback");
            return fallbackChips(msg);
        }

        Set<Integer> candidates = new HashSet<>();
        for (String t : qTokens) {
            if (WEAK.contains(t)) {
                continue;
            }
            List<Integer> idxs = tokenIndex.get(t);
            if (idxs != null) {
                candidates.addAll(idxs);
            }
        }
        if (candidates.isEmpty()) {
            SmartReplyLog.d("pairs skipped: no candidates → fallback");
            return fallbackChips(msg);
        }

        Set<String> qTri = charTrigrams(msg);
        Map<String, Float> votes = new HashMap<>();
        Map<String, String> display = new HashMap<>();
        Map<String, String> bestSource = new HashMap<>();
        float best = 0f;
        String bestReply = null;
        String bestSrc = null;

        for (int idx : candidates) {
            Pair pair = pairs.get(idx);
            int overlap = 0;
            int strongOverlap = 0;
            for (String t : qTokens) {
                if (pair.tokens.contains(t)) {
                    overlap++;
                    if (!WEAK.contains(t) && t.length() > 2) {
                        strongOverlap++;
                    }
                }
            }
            if (overlap == 0 || strongOverlap == 0) {
                continue;
            }
            float wordScore = strongOverlap / (float) Math.max(1, nonWeakCount(qTokens));
            float charScore = jaccard(qTri, pair.trigrams);
            if (!overlapOk(qTokens, pair.tokens, wordScore, strongOverlap, charScore)) {
                continue;
            }
            float combined = (0.68f * wordScore + 0.32f * charScore) * pair.weight;
            if (combined < MIN_PAIR_SCORE) {
                continue;
            }
            String key = normalizeReply(pair.reply);
            float prev = votes.getOrDefault(key, 0f);
            float next = prev + combined;
            votes.put(key, next);
            display.putIfAbsent(key, pair.reply);
            if (!bestSource.containsKey(key) || pair.weight > weightForSource(bestSource.get(key))) {
                bestSource.put(key, pair.source);
            }
            if (combined > best) {
                best = combined;
                bestReply = pair.reply;
                bestSrc = pair.source;
            }
        }

        if (votes.isEmpty() || best < MIN_PAIR_SCORE) {
            SmartReplyLog.d("pairs no score for \"" + preview(msg) + "\" → fallback");
            return fallbackChips(msg);
        }

        List<Map.Entry<String, Float>> ranked = new ArrayList<>(votes.entrySet());
        ranked.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (Map.Entry<String, Float> e : ranked) {
            if (e.getValue() < MIN_PAIR_SCORE * 0.85f) {
                continue;
            }
            out.add(display.get(e.getKey()));
            if (out.size() >= TOP_K) {
                break;
            }
        }
        if (out.isEmpty()) {
            SmartReplyLog.d("pairs ranked empty for \"" + preview(msg) + "\" → fallback");
            return fallbackChips(msg);
        }
        SmartReplyLog.d("pair hit for \"" + preview(msg) + "\" best=\"" + preview(bestReply)
                + "\" src=" + bestSrc + " score=" + String.format(Locale.US, "%.2f", best)
                + " → " + out);
        return new ArrayList<>(out);
    }

    private static List<String> fallbackChips(String msg) {
        String[] set = FALLBACK_SETS[Math.floorMod(msg.hashCode(), FALLBACK_SETS.length)];
        return Arrays.asList(set[0], set[1], set[2]);
    }

    private static float weightForSource(String source) {
        if ("curated".equals(source)) {
            return WEIGHT_CURATED;
        }
        if ("external".equals(source)) {
            return WEIGHT_EXTERNAL;
        }
        return WEIGHT_LEGACY;
    }

    private static boolean allWeak(Set<String> tokens) {
        for (String t : tokens) {
            if (!WEAK.contains(t) && t.length() > 2) {
                return false;
            }
        }
        return true;
    }

    private static int nonWeakCount(Set<String> tokens) {
        int n = 0;
        for (String t : tokens) {
            if (!WEAK.contains(t) && t.length() > 2) {
                n++;
            }
        }
        return Math.max(1, n);
    }

    /** Java \\b does not work for Cyrillic — compile safe patterns. */
    private static Pattern compileIntentPattern(String raw) {
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        if (raw.startsWith("^") || raw.contains("(") || raw.contains("|") || raw.contains("\\d") || raw.contains("\\?")) {
            return Pattern.compile(raw.replace("\\b", ""), flags);
        }
        if (raw.contains(" ")) {
            String q = Pattern.quote(raw.trim());
            return Pattern.compile("(^|[\\s,.!?…])" + q + "($|[\\s,.!?…])", flags);
        }
        return Pattern.compile(Pattern.quote(raw), flags);
    }

    private List<String> matchIntents(String msg) {
        int words = msg.split("\\s+").length;
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (IntentRule rule : intents) {
            for (int i = 0; i < rule.patterns.size(); i++) {
                Pattern p = rule.patterns.get(i);
                String raw = rule.patternRaw.get(i);
                if (!p.matcher(msg).find()) {
                    continue;
                }
                if (!intentAllowed(msg, raw, words)) {
                    continue;
                }
                for (String chip : rule.chips) {
                    out.putIfAbsent(normalizeReply(chip), chip);
                }
                break;
            }
            if (out.size() >= TOP_K) {
                break;
            }
        }
        if (out.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(out.values()).subList(0, Math.min(TOP_K, out.size()));
    }

    private static boolean intentAllowed(String msg, String raw, int words) {
        if (raw.startsWith("^")) {
            return true;
        }
        if (raw.contains(" ") || raw.contains("\\s") || raw.contains("\\d")) {
            return true;
        }
        return words <= 8 && msg.length() <= 64;
    }

    private static boolean overlapOk(Set<String> q, Set<String> c, float ratio, int strongOverlap, float charScore) {
        int qStrong = nonWeakCount(q);
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
                if (c.contains(t) && !WEAK.contains(t) && t.length() > 3) {
                    return ratio >= MIN_SINGLE_TOKEN_COMBINED && charScore >= MIN_SINGLE_TOKEN_CHAR_SCORE;
                }
            }
            return false;
        }
        return ratio >= 0.55f && charScore >= 0.12f;
    }

    private static float jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0f;
        }
        int inter = 0;
        for (String s : a) {
            if (b.contains(s)) {
                inter++;
            }
        }
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0f : inter / (float) union;
    }

    private static Set<String> charTrigrams(String text) {
        Set<String> out = new HashSet<>();
        String s = " " + text + " ";
        if (s.length() < 4) {
            return out;
        }
        for (int i = 0; i <= s.length() - 3; i++) {
            out.add(s.substring(i, i + 3));
        }
        return out;
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        String t = text.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("https?://\\S+|www\\.\\S+", " ");
        t = t.replaceAll("[\\r\\n]+", " ");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    private static Set<String> contentTokens(String text) {
        Set<String> tokens = new HashSet<>();
        for (String raw : text.split("[^\\p{L}\\p{N}]+")) {
            if (raw.length() <= 1 || STOP.contains(raw)) {
                continue;
            }
            tokens.add(raw);
        }
        return tokens;
    }

    private static String normalizeReply(String reply) {
        return reply.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static boolean isBlocked(String reply) {
        String n = normalizeReply(reply);
        if (BLOCKLIST.contains(n)) {
            return true;
        }
        return n.matches("[\\W\\d_]+") && !n.contains("👍") && !n.contains("😂") && !n.contains("🥰");
    }

    private static String preview(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        if (oneLine.length() <= 48) {
            return oneLine;
        }
        return oneLine.substring(0, 48) + "…";
    }

    private static final class IntentRule {
        String id;
        final List<Pattern> patterns = new ArrayList<>();
        final List<String> patternRaw = new ArrayList<>();
        final List<String> chips = new ArrayList<>();
    }

    private static final class Pair {
        String context;
        String reply;
        String source;
        float weight;
        Set<String> tokens;
        Set<String> trigrams;
    }
}
