package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.LocalMessageFakeEditPatch;

import java.util.ArrayList;

public final class LocalMessageFakeEditHook {

    public static final int OPTION_LOCAL_FAKE_EDIT = LocalMessageFakeEditPatch.OPTION_LOCAL_FAKE_EDIT;
    public static final int OPTION_RESET_LOCAL_FAKE_EDIT = LocalMessageFakeEditPatch.OPTION_RESET_LOCAL_FAKE_EDIT;

    private LocalMessageFakeEditHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        LocalMessageFakeEditPatch.appendOption(items, options, icons, selectedMessage);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        return LocalMessageFakeEditPatch.handleSelectedOption(fragment, selectedMessage, option);
    }

    public static void applyStoredEdit(MessageObject messageObject) {
        LocalMessageFakeEditPatch.applyStoredEdit(messageObject);
    }

    public static boolean handleComposerDoneEditing(ChatActivity fragment, MessageObject editingMessageObject,
            CharSequence text, ArrayList<TLRPC.MessageEntity> entities) {
        return LocalMessageFakeEditPatch.handleComposerDoneEditing(fragment, editingMessageObject, text, entities);
    }

    public static void onEditingSessionChanged(ChatActivity fragment, MessageObject previousEditingMessageObject,
            MessageObject currentEditingMessageObject) {
        LocalMessageFakeEditPatch.onEditingSessionChanged(fragment, previousEditingMessageObject, currentEditingMessageObject);
    }
}
