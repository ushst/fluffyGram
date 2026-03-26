package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.LocalMessageArchivePatch;

import java.util.ArrayList;

public final class LocalMessageArchiveHook {

    private LocalMessageArchiveHook() {
    }

    public static void captureServerEdit(TLRPC.Message oldMessage, TLRPC.Message newMessage) {
        LocalMessageArchivePatch.captureServerEdit(oldMessage, newMessage);
    }

    public static boolean shouldCaptureDeletedMessages() {
        return LocalMessageArchivePatch.shouldCaptureDeletedMessages();
    }

    public static boolean preserveDeletedMessage(ChatActivity fragment, MessageObject messageObject) {
        return LocalMessageArchivePatch.preserveDeletedMessage(fragment, messageObject);
    }

    public static void captureDeletedMessage(TLRPC.Message message, long topicId) {
        LocalMessageArchivePatch.captureDeletedMessage(message, topicId);
    }

    public static void restoreDeletedMessages(long dialogId, long topicId, int mode, ArrayList<TLRPC.Message> messages) {
        LocalMessageArchivePatch.restoreDeletedMessages(dialogId, topicId, mode, messages);
    }

    public static void captureLocalEdit(MessageObject messageObject, String previousText, boolean reset) {
        LocalMessageArchivePatch.captureLocalEdit(messageObject, previousText, reset);
    }
}
