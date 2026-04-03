package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.PendingSendMessageEditPatch;

import java.util.ArrayList;

public final class PendingSendMessageEditHook {

    public static final int OPTION_EDIT_PENDING_SEND = PendingSendMessageEditPatch.OPTION_EDIT_PENDING_SEND;

    private PendingSendMessageEditHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        PendingSendMessageEditPatch.appendOption(items, options, icons, selectedMessage);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        return PendingSendMessageEditPatch.handleSelectedOption(fragment, selectedMessage, option);
    }

    public static boolean handleComposerDoneEditing(ChatActivity fragment, MessageObject editingMessageObject,
            CharSequence text, ArrayList<TLRPC.MessageEntity> entities) {
        return PendingSendMessageEditPatch.handleComposerDoneEditing(fragment, editingMessageObject, text, entities);
    }

    public static void onEditingSessionChanged(ChatActivity fragment, MessageObject previousEditingMessageObject,
            MessageObject currentEditingMessageObject) {
        PendingSendMessageEditPatch.onEditingSessionChanged(fragment, previousEditingMessageObject, currentEditingMessageObject);
    }
}
