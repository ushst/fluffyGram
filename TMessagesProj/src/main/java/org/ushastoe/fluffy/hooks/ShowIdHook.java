package org.ushastoe.fluffy.hooks;

import org.telegram.ui.Cells.TextDetailCell;
import org.ushastoe.fluffy.patches.ShowIdPatch;

public final class ShowIdHook {

    private ShowIdHook() {}

    public static boolean isEnabled() {
        return ShowIdPatch.isEnabled();
    }

    public static String getIdString(long userId, long chatId, boolean isChannel) {
        return ShowIdPatch.getIdString(userId, chatId, isChannel);
    }

    public static void bindIdCell(TextDetailCell cell, long userId, long chatId, boolean isChannel) {
        ShowIdPatch.bindIdCell(cell, userId, chatId, isChannel);
    }
}
