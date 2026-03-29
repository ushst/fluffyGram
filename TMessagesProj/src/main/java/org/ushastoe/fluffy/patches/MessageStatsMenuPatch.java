package org.ushastoe.fluffy.patches;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ChatActivity;

import java.util.ArrayList;

public final class MessageStatsMenuPatch {

    private MessageStatsMenuPatch() {
    }

    public static void appendOption(ChatActivity fragment, ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (fragment == null || items == null || options == null || icons == null || selectedMessage == null || selectedMessage.messageOwner == null) {
            return;
        }
        if (!MessageActionsPatch.isMessageStatsEnabled()) {
            return;
        }
        if (selectedMessage.messageOwner.views <= 0) {
            return;
        }
        if (!ChatObject.hasAdminRights(fragment.getCurrentChat())) {
            return;
        }
        items.add(LocaleController.getString(R.string.ViewStatistics));
        options.add(ChatActivity.OPTION_STATISTICS);
        icons.add(R.drawable.msg_stats);
    }
}
