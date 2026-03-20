package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.ChatFirstMessagePatch;

public final class ChatFirstMessageHook {

    private ChatFirstMessageHook() {
    }

    public static boolean isEnabled() {
        return ChatFirstMessagePatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ChatFirstMessagePatch.setEnabled(enabled);
    }

    public static void addChatMenuItem(ActionBarMenuItem headerItem, int menuId) {
        ChatFirstMessagePatch.addChatMenuItem(headerItem, menuId);
    }

    public static void updateChatMenuItem(ActionBarMenuItem headerItem, int menuId) {
        ChatFirstMessagePatch.updateChatMenuItem(headerItem, menuId);
    }

    public static boolean onChatMenuItemClick(ChatActivity chatActivity, ActionBarMenuItem headerItem, int id, int menuId) {
        return ChatFirstMessagePatch.onChatMenuItemClick(chatActivity, headerItem, id, menuId);
    }
}
