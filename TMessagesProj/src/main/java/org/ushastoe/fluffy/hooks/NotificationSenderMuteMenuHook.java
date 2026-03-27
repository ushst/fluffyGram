package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.NotificationSenderMutePatch;

import java.util.ArrayList;

public final class NotificationSenderMuteMenuHook {

    public static final int OPTION_TOGGLE_SENDER_NOTIFICATIONS = NotificationSenderMutePatch.OPTION_TOGGLE_SENDER_NOTIFICATIONS;

    private NotificationSenderMuteMenuHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, int currentAccount, MessageObject selectedMessage) {
        NotificationSenderMutePatch.appendOption(items, options, icons, currentAccount, selectedMessage);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        return NotificationSenderMutePatch.handleSelectedOption(fragment, selectedMessage, option);
    }
}
