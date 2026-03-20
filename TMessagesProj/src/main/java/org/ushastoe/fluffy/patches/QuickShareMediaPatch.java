package org.ushastoe.fluffy.patches;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

public final class QuickShareMediaPatch {

    private QuickShareMediaPatch() {
    }

    public static boolean shouldDrawForPrivateChat(MessageObject messageObject, TLRPC.User user) {
        if (messageObject == null || user == null || messageObject.messageOwner == null || messageObject.messageOwner.from_id == null) {
            return false;
        }
        if (DialogObject.isEncryptedDialog(messageObject.getDialogId())) {
            return false;
        }
        if (messageObject.hasExtendedMedia()) {
            return false;
        }
        return messageObject.messageOwner.from_id.user_id != UserConfig.getInstance(messageObject.currentAccount).getClientUserId();
    }
}
