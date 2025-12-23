package org.ushastoe.fluffy.helpers;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts text typed in the wrong keyboard layout (English <-> Russian) by mapping characters
 * based on their physical key positions.
 */
public final class TranslitHelper {

    private static final Map<Character, Character> EN_TO_RU = new HashMap<>();
    private static final Map<Character, Character> RU_TO_EN = new HashMap<>();

    static {
        final String en = "`qwertyuiop[]asdfghjkl;'zxcvbnm,./";
        final String enShift = "~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>?";
        final String ru = "\u0451\u0439\u0446\u0443\u043a\u0435\u043d\u0433\u0448\u0449\u0437\u0445\u044a\u0444\u044b\u0432\u0430\u043f\u0440\u043e\u043b\u0434\u0436\u044d\u044f\u0447\u0441\u043c\u0438\u0442\u044c\u0431\u044e.";
        final String ruShift = "\u0401\u0419\u0426\u0423\u041a\u0415\u041d\u0413\u0428\u0429\u0417\u0425\u042a\u0424\u042b\u0412\u0410\u041f\u0420\u041e\u041b\u0414\u0416\u042d\u042f\u0427\u0421\u041c\u0418\u0422\u042c\u0411\u042e.";

        for (int i = 0; i < en.length(); i++) {
            register(en.charAt(i), ru.charAt(i));
            register(enShift.charAt(i), ruShift.charAt(i));
        }
    }

    private TranslitHelper() {
        // no-op
    }

    private static void register(char enChar, char ruChar) {
        EN_TO_RU.put(enChar, ruChar);
        RU_TO_EN.put(ruChar, enChar);
    }

    public static String convert(CharSequence text) {
        if (TextUtils.isEmpty(text)) {
            return "";
        }
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            result.append(mapChar(text.charAt(i)));
        }
        return result.toString();
    }

    private static char mapChar(char ch) {
        Character mapped = EN_TO_RU.get(ch);
        if (mapped != null) {
            return mapped;
        }
        mapped = RU_TO_EN.get(ch);
        if (mapped != null) {
            return mapped;
        }

        char lower = Character.toLowerCase(ch);
        if (lower != ch) {
            mapped = EN_TO_RU.get(lower);
            if (mapped != null) {
                return Character.toUpperCase(mapped);
            }
            mapped = RU_TO_EN.get(lower);
            if (mapped != null) {
                return Character.toUpperCase(mapped);
            }
        }
        return ch;
    }
}
