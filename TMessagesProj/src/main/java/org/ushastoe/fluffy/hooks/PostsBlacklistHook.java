package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.PostsBlacklistPatch;

import java.util.ArrayList;
import java.util.List;

public final class PostsBlacklistHook {

    private PostsBlacklistHook() {
    }

    public static boolean isHidden(int currentAccount, String query, long dialogId) {
        return PostsBlacklistPatch.isHidden(currentAccount, query, dialogId);
    }

    public static int getHiddenCount(int currentAccount, String query) {
        return PostsBlacklistPatch.getHiddenCount(currentAccount, query);
    }

    public static void filterMessages(int currentAccount, String query, List<MessageObject> messages) {
        PostsBlacklistPatch.filterMessages(currentAccount, query, messages);
    }

    public static void fillMessageMenu(boolean searchMode, String query, MessageObject message, ArrayList<Integer> icons, ArrayList<CharSequence> items, ArrayList<Integer> options) {
        PostsBlacklistPatch.fillMessageMenu(searchMode, query, message, icons, items, options);
    }

    public static boolean processOption(ChatActivity chatActivity, int option, MessageObject message, String query) {
        return PostsBlacklistPatch.processOption(chatActivity, option, message, query);
    }
}
