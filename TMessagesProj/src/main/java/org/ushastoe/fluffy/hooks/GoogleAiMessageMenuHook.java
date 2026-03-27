package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.GoogleAiMessageMenuPatch;

import java.util.ArrayList;

public final class GoogleAiMessageMenuHook {

    public static final int OPTION_GOOGLE_AI = GoogleAiMessageMenuPatch.OPTION_GOOGLE_AI;

    private GoogleAiMessageMenuHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        GoogleAiMessageMenuPatch.appendOption(items, options, icons, selectedMessage);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        return GoogleAiMessageMenuPatch.handleSelectedOption(fragment, selectedMessage, option);
    }
}
