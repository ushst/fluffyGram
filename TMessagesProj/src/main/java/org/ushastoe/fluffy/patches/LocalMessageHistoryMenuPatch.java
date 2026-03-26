package org.ushastoe.fluffy.patches;

import android.os.Bundle;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.hooks.MessageActionsHook;
import org.ushastoe.fluffy.ui.FluffyLocalMessageHistoryActivity;

import java.util.ArrayList;

public final class LocalMessageHistoryMenuPatch {

    public static final int OPTION_LOCAL_MESSAGE_HISTORY = 9996;

    private static final String ARG_DIALOG_ID = "fluffy_history_dialog_id";
    private static final String ARG_MESSAGE_ID = "fluffy_history_message_id";

    private LocalMessageHistoryMenuPatch() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (!MessageActionsHook.isLocalMessageHistoryEnabled() || !PremiumSettingsPatch.isLocalMessageHistoryEnabled()) {
            return;
        }
        if (items == null || options == null || icons == null || selectedMessage == null || selectedMessage.messageOwner == null) {
            return;
        }
        if (!LocalMessageArchivePatch.hasLocalHistory(selectedMessage)) {
            return;
        }
        items.add(LocaleController.getString(R.string.FluffyLocalMessageHistoryTitle));
        options.add(OPTION_LOCAL_MESSAGE_HISTORY);
        icons.add(R.drawable.msg_log);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        if (option != OPTION_LOCAL_MESSAGE_HISTORY || fragment == null || selectedMessage == null || selectedMessage.messageOwner == null) {
            return false;
        }
        Bundle args = new Bundle();
        args.putLong(ARG_DIALOG_ID, selectedMessage.messageOwner.dialog_id);
        args.putInt(ARG_MESSAGE_ID, selectedMessage.getId());
        fragment.presentFragment(new FluffyLocalMessageHistoryActivity(args, selectedMessage));
        return true;
    }
}
