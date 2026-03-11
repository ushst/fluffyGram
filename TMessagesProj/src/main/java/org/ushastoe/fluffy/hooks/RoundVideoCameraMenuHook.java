package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.ushastoe.fluffy.patches.RoundVideoCameraMenuPatch;

public final class RoundVideoCameraMenuHook {

    private RoundVideoCameraMenuHook() {
    }

    public static void addChatMenuItems(ActionBarMenuItem headerItem, int switchFrontId, int switchBackId) {
        RoundVideoCameraMenuPatch.addChatMenuItems(headerItem, switchFrontId, switchBackId);
    }

    public static void updateChatMenuItems(ActionBarMenuItem headerItem, ChatActivityEnterView enterView, int switchFrontId, int switchBackId) {
        RoundVideoCameraMenuPatch.updateChatMenuItems(headerItem, enterView, switchFrontId, switchBackId);
    }

    public static boolean onChatMenuItemClick(ChatActivityEnterView enterView, ActionBarMenuItem headerItem, int id, int switchFrontId, int switchBackId) {
        return RoundVideoCameraMenuPatch.onChatMenuItemClick(enterView, headerItem, id, switchFrontId, switchBackId);
    }
}
