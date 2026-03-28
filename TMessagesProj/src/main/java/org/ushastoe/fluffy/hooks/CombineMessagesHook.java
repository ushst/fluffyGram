package org.ushastoe.fluffy.hooks;

import android.util.SparseArray;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.CombineMessagesPatch;

public final class CombineMessagesHook {

    private CombineMessagesHook() {
    }

    public static boolean isEnabled() {
        return CombineMessagesPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        CombineMessagesPatch.setEnabled(enabled);
    }

    public static void addActionModeItem(ActionBarMenu actionMode, int menuId) {
        CombineMessagesPatch.addActionModeItem(actionMode, menuId);
    }

    public static void updateActionModeItem(ActionBarMenu actionMode, int menuId, boolean visible) {
        CombineMessagesPatch.updateActionModeItem(actionMode, menuId, visible);
    }

    public static boolean onActionModeItemClick(ChatActivity chatActivity, int id, int menuId, SparseArray<MessageObject>[] selectedMessagesCanCopyIds) {
        return CombineMessagesPatch.onActionModeItemClick(chatActivity, id, menuId, selectedMessagesCanCopyIds);
    }
}
