package org.ushastoe.fluffy.hooks;

import java.util.ArrayList;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.patches.MessageTranslitMenuPatch;

public final class MessageTranslitMenuHook {

    public static final int OPTION_TRANSLIT = 9993;

    private MessageTranslitMenuHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (!MessageActionsHook.isMessageTranslitEnabled()) {
            return;
        }
        MessageTranslitMenuPatch.appendOptions(items, options, icons, selectedMessage, OPTION_TRANSLIT);
    }

    public static String getConvertedText(MessageObject selectedMessage) {
        return MessageTranslitMenuPatch.getConvertedText(selectedMessage);
    }

    public static boolean applyInlineTranslit(MessageObject selectedMessage) {
        return MessageTranslitMenuPatch.applyInlineTranslit(selectedMessage);
    }

    public static boolean toggleInlineTranslit(MessageObject selectedMessage) {
        return MessageTranslitMenuPatch.toggleInlineTranslit(selectedMessage);
    }

    public static boolean shouldDisplayInlineTranslit(MessageObject messageObject, TLRPC.TL_textWithEntities translatedText) {
        return MessageTranslitMenuPatch.shouldDisplayInlineTranslit(messageObject, translatedText);
    }
}
