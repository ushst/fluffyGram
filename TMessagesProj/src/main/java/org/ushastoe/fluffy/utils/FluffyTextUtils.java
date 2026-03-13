package org.ushastoe.fluffy.utils;

public final class FluffyTextUtils {

    private static final int MAX_TEXT_LENGTH = 7;
    private static final int MAX_PARAMETER_LENGTH = 7;

    private FluffyTextUtils() {
    }

    public static CharSequence truncateLongWords(CharSequence text) {
        if (text == null) {
            return "";
        }
        String source = text.toString();
        if (source.isEmpty() || source.length() <= MAX_TEXT_LENGTH) {
            return source;
        }
        return source.substring(0, MAX_TEXT_LENGTH);
    }

    public static CharSequence truncateParameterValue(CharSequence text) {
        if (text == null) {
            return "";
        }
        String source = text.toString();
        if (source.isEmpty() || source.length() <= MAX_PARAMETER_LENGTH) {
            return source;
        }
        return source.substring(0, MAX_PARAMETER_LENGTH) + "\u2026";
    }
}
