package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.MessageStatsMenuPatch;

import java.util.ArrayList;

public final class MessageStatsMenuHook {

    private MessageStatsMenuHook() {
    }

    public static void appendOption(ChatActivity fragment, ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        MessageStatsMenuPatch.appendOption(fragment, items, options, icons, selectedMessage);
    }
}
