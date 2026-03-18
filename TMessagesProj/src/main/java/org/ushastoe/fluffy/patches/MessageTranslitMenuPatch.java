package org.ushastoe.fluffy.patches;

import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class MessageTranslitMenuPatch {

    private static final String TRANSLIT_LANGUAGE = "fluffy-translit";

    private static final String EN_LAYOUT = "`qwertyuiop[]asdfghjkl;'zxcvbnm,./";
    private static final String RU_LAYOUT = "ёйцукенгшщзхъфывапролджэячсмитьбю.";

    private static final Map<Character, Character> EN_TO_RU = new HashMap<>();
    private static final Map<Character, Character> RU_TO_EN = new HashMap<>();

    static {
        int count = Math.min(EN_LAYOUT.length(), RU_LAYOUT.length());
        for (int i = 0; i < count; i++) {
            char en = EN_LAYOUT.charAt(i);
            char ru = RU_LAYOUT.charAt(i);
            addPair(en, ru);
            addPair(Character.toUpperCase(en), Character.toUpperCase(ru));
        }
    }

    private MessageTranslitMenuPatch() {
    }

    private static void addPair(char en, char ru) {
        EN_TO_RU.put(en, ru);
        RU_TO_EN.put(ru, en);
    }

    public static void appendOptions(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage, int translitOptionId) {
        if (items == null || options == null || icons == null) {
            return;
        }
        String source = getSourceText(selectedMessage);
        if (TextUtils.isEmpty(source)) {
            return;
        }
        items.add(LocaleController.getString(R.string.FluffyMessageTranslit));
        options.add(translitOptionId);
        icons.add(R.drawable.msg_translate);
    }

    public static String getConvertedText(MessageObject selectedMessage) {
        String source = getSourceText(selectedMessage);
        if (TextUtils.isEmpty(source)) {
            return null;
        }

        Candidate enToRu = remap(source, EN_TO_RU);
        Candidate ruToEn = remap(source, RU_TO_EN);

        if (enToRu.changed == 0 && ruToEn.changed == 0) {
            return null;
        }
        if (enToRu.changed == 0) {
            return ruToEn.text;
        }
        if (ruToEn.changed == 0) {
            return enToRu.text;
        }

        int sourceLatin = countLatin(source);
        int sourceCyrillic = countCyrillic(source);

        if (sourceLatin > sourceCyrillic) {
            return enToRu.text;
        }
        if (sourceCyrillic > sourceLatin) {
            return ruToEn.text;
        }

        int enToRuScore = enToRu.changed * 2 + countCyrillic(enToRu.text);
        int ruToEnScore = ruToEn.changed * 2 + countLatin(ruToEn.text);
        return enToRuScore >= ruToEnScore ? enToRu.text : ruToEn.text;
    }

    public static boolean applyInlineTranslit(MessageObject selectedMessage) {
        if (selectedMessage == null || selectedMessage.messageOwner == null) {
            return false;
        }
        String convertedText = getConvertedText(selectedMessage);
        if (TextUtils.isEmpty(convertedText)) {
            return false;
        }

        TLRPC.TL_textWithEntities textWithEntities = new TLRPC.TL_textWithEntities();
        textWithEntities.text = convertedText;
        textWithEntities.entities = new ArrayList<>();

        selectedMessage.messageOwner.translatedText = textWithEntities;
        selectedMessage.messageOwner.translatedToLanguage = TRANSLIT_LANGUAGE;
        return true;
    }

    public static boolean toggleInlineTranslit(MessageObject selectedMessage) {
        if (selectedMessage == null || selectedMessage.messageOwner == null) {
            return false;
        }
        if (TRANSLIT_LANGUAGE.equals(selectedMessage.messageOwner.translatedToLanguage)
                && selectedMessage.messageOwner.translatedText != null) {
            selectedMessage.messageOwner.translatedToLanguage = null;
            selectedMessage.messageOwner.translatedText = null;
            return true;
        }
        return applyInlineTranslit(selectedMessage);
    }

    public static boolean shouldDisplayInlineTranslit(MessageObject messageObject, TLRPC.TL_textWithEntities translatedText) {
        return messageObject != null
                && messageObject.messageOwner != null
                && translatedText != null
                && TRANSLIT_LANGUAGE.equals(messageObject.messageOwner.translatedToLanguage);
    }

    private static String getSourceText(MessageObject selectedMessage) {
        if (selectedMessage == null || selectedMessage.messageOwner == null) {
            return null;
        }
        if (!TextUtils.isEmpty(selectedMessage.messageOwner.message)) {
            return selectedMessage.messageOwner.message;
        }
        if (!TextUtils.isEmpty(selectedMessage.caption)) {
            return selectedMessage.caption.toString();
        }
        return null;
    }

    private static Candidate remap(String source, Map<Character, Character> map) {
        StringBuilder builder = new StringBuilder(source.length());
        int changed = 0;
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            Character mapped = map.get(ch);
            if (mapped != null) {
                builder.append(mapped.charValue());
                changed++;
            } else {
                builder.append(ch);
            }
        }
        return new Candidate(builder.toString(), changed);
    }

    private static int countLatin(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                count++;
            }
        }
        return count;
    }

    private static int countCyrillic(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if ((ch >= '\u0400' && ch <= '\u04FF') || (ch >= '\u0500' && ch <= '\u052F')) {
                count++;
            }
        }
        return count;
    }

    private static final class Candidate {
        final String text;
        final int changed;

        Candidate(String text, int changed) {
            this.text = text;
            this.changed = changed;
        }
    }
}
