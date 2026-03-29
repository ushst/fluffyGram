package org.ushastoe.fluffy.patches;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.TextDetailCell;

public final class ShowIdPatch {

    private ShowIdPatch() {}

    public static boolean isEnabled() {
        return true;
    }

    public static String getIdCopyString(int currentAccount, long userId, long chatId, boolean isChannel) {
        if (userId != 0) return String.valueOf(userId);
        if (chatId != 0) {
            if (isChannel) return "-100" + chatId;
            return String.valueOf(-chatId);
        }
        return null;
    }

    public static void bindIdCell(TextDetailCell cell, int currentAccount, long userId, long chatId, boolean isChannel, int fallbackDcId) {
        String id = getIdCopyString(currentAccount, userId, chatId, isChannel);
        if (id == null) id = "—";
        int dcId = resolveDcId(currentAccount, userId, chatId, fallbackDcId);
        String value = dcId > 0 ? id + ", DC " + dcId : id;
        cell.setTextAndValue(value, "Telegram ID", false);
    }

    private static int resolveDcId(int currentAccount, long userId, long chatId, int fallbackDcId) {
        if (fallbackDcId > 0) {
            return fallbackDcId;
        }

        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        if (userId != 0) {
            TLRPC.User user = messagesController.getUser(userId);
            if (user != null) {
                if (user.photo != null && user.photo.dc_id > 0) {
                    return user.photo.dc_id;
                }
                int currentDcId = ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId();
                if (UserObject.isUserSelf(user) && currentDcId > 0) {
                    return currentDcId;
                }
            }
        } else if (chatId != 0) {
            TLRPC.Chat chat = messagesController.getChat(chatId);
            if (chat != null && chat.photo != null && chat.photo.dc_id > 0) {
                return chat.photo.dc_id;
            }
        }
        return 0;
    }
}
