package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.LocalMessageHistoryMenuPatch;

import java.util.ArrayList;

public final class LocalMessageHistoryMenuHook {

    public static final int OPTION_LOCAL_MESSAGE_HISTORY = LocalMessageHistoryMenuPatch.OPTION_LOCAL_MESSAGE_HISTORY;

    private LocalMessageHistoryMenuHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        LocalMessageHistoryMenuPatch.appendOption(items, options, icons, selectedMessage);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        return LocalMessageHistoryMenuPatch.handleSelectedOption(fragment, selectedMessage, option);
    }
}
