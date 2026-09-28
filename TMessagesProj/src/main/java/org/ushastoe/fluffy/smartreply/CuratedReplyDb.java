package org.ushastoe.fluffy.smartreply;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Bundled {@code smart_reply_db.json}: intent bank + curated pairs.
 * Pair sources: curated (1.0) &gt; external (0.65) &gt; legacy (0.55).
 * Accessed only from the engine thread.
 */
final class CuratedReplyDb {

    private static final String ASSET_PATH = "fluffy/smart_reply_db.json";

    private static final float WEIGHT_CURATED = 1.0f;
    private static final float WEIGHT_EXTERNAL = 0.65f;
    private static final float WEIGHT_LEGACY = 0.55f;

    private static CuratedReplyDb instance;

    private final List<IntentRule> intents = new ArrayList<>();
    private final PairIndex pairs = new PairIndex("curated");
    private final List<Entry> entries = new ArrayList<>();
    private boolean loaded;
    private boolean failed;

    static CuratedReplyDb getInstance() {
        if (instance == null) {
            instance = new CuratedReplyDb();
        }
        return instance;
    }

    private CuratedReplyDb() {
    }

    boolean ensureLoaded(Context app) {
        if (!loaded && !failed) {
            load(app);
        }
        return loaded;
    }

    PairIndex pairs() {
        return pairs;
    }

    List<Entry> entries() {
        return entries;
    }

    private void load(Context app) {
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
                            if (!TextUtils.isEmpty(chip) && !SmartReplyText.isBlocked(chip)) {
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
                    String source = o.optString("source", "legacy");
                    String context = o.optString("context");
                    String reply = o.optString("reply").trim();
                    float weight = weightForSource(source);
                    pairs.add(context, reply, weight, source, 0);
                    String cleaned = SmartReplyText.clean(context);
                    if (!cleaned.isEmpty() && !reply.isEmpty() && !SmartReplyText.isBlocked(reply)) {
                        entries.add(new Entry(cleaned, reply, weight));
                    }
                }
            }
            loaded = true;
            SmartReplyLog.d("curated db loaded intents=" + intents.size() + " pairs=" + pairs.size());
        } catch (Throwable e) {
            failed = true;
            SmartReplyLog.e("failed to load curated db", e);
            FileLog.e("SmartReply failed to load db", e);
        }
    }

    /** @param msg already cleaned incoming text */
    List<String> matchIntents(String msg, int topK) {
        int words = SmartReplyText.wordCount(msg);
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (IntentRule rule : intents) {
            for (int i = 0; i < rule.patterns.size(); i++) {
                if (!rule.patterns.get(i).matcher(msg).find()) {
                    continue;
                }
                if (!intentAllowed(msg, rule.patternRaw.get(i), words)) {
                    continue;
                }
                for (String chip : rule.chips) {
                    out.putIfAbsent(SmartReplyText.normalizeReply(chip), chip);
                }
                break;
            }
            if (out.size() >= topK) {
                break;
            }
        }
        if (out.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>(out.values());
        return result.subList(0, Math.min(topK, result.size()));
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

    private static boolean intentAllowed(String msg, String raw, int words) {
        if (raw.startsWith("^")) {
            return true;
        }
        if (raw.contains(" ") || raw.contains("\\s") || raw.contains("\\d")) {
            return true;
        }
        return words <= 8 && msg.length() <= 64;
    }

    static final class Entry {
        /** Already cleaned. */
        final String context;
        final String reply;
        final float weight;

        Entry(String context, String reply, float weight) {
            this.context = context;
            this.reply = reply;
            this.weight = weight;
        }
    }

    private static final class IntentRule {
        final List<Pattern> patterns = new ArrayList<>();
        final List<String> patternRaw = new ArrayList<>();
        final List<String> chips = new ArrayList<>();
    }
}
