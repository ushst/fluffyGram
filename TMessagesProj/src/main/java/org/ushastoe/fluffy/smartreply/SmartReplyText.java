package org.ushastoe.fluffy.smartreply;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class SmartReplyText {

    static final Set<String> STOP = new HashSet<>();
    static final Set<String> WEAK = new HashSet<>();
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

    private SmartReplyText() {
    }

    static String clean(String text) {
        if (text == null) {
            return "";
        }
        String t = text.toLowerCase(Locale.ROOT).trim();
        t = t.replaceAll("https?://\\S+|www\\.\\S+", " ");
        t = t.replaceAll("[\\r\\n]+", " ");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }

    static int wordCount(String text) {
        String t = text.trim();
        return t.isEmpty() ? 0 : t.split("\\s+").length;
    }

    static Set<String> contentTokens(String text) {
        Set<String> tokens = new HashSet<>();
        for (String raw : text.split("[^\\p{L}\\p{N}]+")) {
            if (raw.length() <= 1 || STOP.contains(raw)) {
                continue;
            }
            tokens.add(raw);
        }
        return tokens;
    }

    static Set<String> charTrigrams(String text) {
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

    static float jaccard(Set<String> a, Set<String> b) {
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

    static boolean isStrong(String token) {
        return !WEAK.contains(token) && token.length() > 2;
    }

    static boolean allWeak(Set<String> tokens) {
        for (String t : tokens) {
            if (isStrong(t)) {
                return false;
            }
        }
        return true;
    }

    static int nonWeakCount(Set<String> tokens) {
        int n = 0;
        for (String t : tokens) {
            if (isStrong(t)) {
                n++;
            }
        }
        return Math.max(1, n);
    }

    static String normalizeReply(String reply) {
        return reply.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    static boolean isBlocked(String reply) {
        String n = normalizeReply(reply);
        if (BLOCKLIST.contains(n)) {
            return true;
        }
        return n.matches("[\\W\\d_]+") && !n.contains("👍") && !n.contains("😂") && !n.contains("🥰");
    }

    static String preview(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        if (oneLine.length() <= 48) {
            return oneLine;
        }
        return oneLine.substring(0, 48) + "…";
    }
}
