package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.ForceCopyPatch;

public final class ForceCopyHook {

    private ForceCopyHook() {
    }

    public static boolean isEnabled() {
        return ForceCopyPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ForceCopyPatch.setEnabled(enabled);
    }

    public static boolean shouldBypassNoForwards(ChatActivity chatActivity) {
        return ForceCopyPatch.shouldBypassNoForwards(chatActivity);
    }

    public static boolean isCopyRestricted(ChatActivity chatActivity, MessageObject messageObject) {
        return ForceCopyPatch.isCopyRestricted(chatActivity, messageObject);
    }

    public static boolean isCopyOrSaveRestricted(ChatActivity chatActivity, MessageObject messageObject, boolean includePaidMedia) {
        return ForceCopyPatch.isCopyOrSaveRestricted(chatActivity, messageObject, includePaidMedia);
    }

    public static boolean shouldKeepPhotoViewerSecure(ChatActivity chatActivity, long avatarsDialogId, MessageObject messageObject, int currentAccount) {
        return ForceCopyPatch.shouldKeepPhotoViewerSecure(chatActivity, avatarsDialogId, messageObject, currentAccount);
    }
}
