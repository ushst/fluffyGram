package org.ushastoe.fluffy.patches;

import org.telegram.ui.Cells.TextDetailCell;

public final class ShowIdPatch {

    private ShowIdPatch() {}

    public static boolean isEnabled() {
        return true;
    }

    public static String getIdString(long userId, long chatId, boolean isChannel) {
        if (userId != 0) return String.valueOf(userId);
        if (chatId != 0) {
            if (isChannel) return "-100" + chatId;
            return String.valueOf(-chatId);
        }
        return null;
    }

    public static void bindIdCell(TextDetailCell cell, long userId, long chatId, boolean isChannel) {
        String id = getIdString(userId, chatId, isChannel);
        if (id == null) id = "—";
        cell.setTextAndValue(id, "Telegram ID", false);
    }
}
