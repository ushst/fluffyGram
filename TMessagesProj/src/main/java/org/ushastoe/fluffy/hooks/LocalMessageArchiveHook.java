package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.LocalMessageArchivePatch;

public final class LocalMessageArchiveHook {

    private LocalMessageArchiveHook() {
    }

    public static void captureServerEdit(TLRPC.Message oldMessage, TLRPC.Message newMessage) {
        LocalMessageArchivePatch.captureServerEdit(oldMessage, newMessage);
    }

    public static boolean preserveDeletedMessage(ChatActivity fragment, MessageObject messageObject) {
        return LocalMessageArchivePatch.preserveDeletedMessage(fragment, messageObject);
    }

    public static void captureLocalEdit(MessageObject messageObject, String previousText, boolean reset) {
        LocalMessageArchivePatch.captureLocalEdit(messageObject, previousText, reset);
    }
}
