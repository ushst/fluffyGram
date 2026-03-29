package org.ushastoe.fluffy.hooks;

import org.telegram.ui.Cells.TextDetailCell;
import org.ushastoe.fluffy.patches.ShowIdPatch;

public final class ShowIdHook {

    private ShowIdHook() {}

    public static boolean isEnabled() {
        return ShowIdPatch.isEnabled();
    }

    public static String getIdCopyString(int currentAccount, long userId, long chatId, boolean isChannel) {
        return ShowIdPatch.getIdCopyString(currentAccount, userId, chatId, isChannel);
    }

    public static void bindIdCell(TextDetailCell cell, int currentAccount, long userId, long chatId, boolean isChannel, int fallbackDcId) {
        ShowIdPatch.bindIdCell(cell, currentAccount, userId, chatId, isChannel, fallbackDcId);
    }
}
