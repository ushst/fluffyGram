package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ChatActivity;

public final class ForceCopyPatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "force_copy_enabled";

    private ForceCopyPatch() {
    }

    public static boolean isEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean shouldBypassNoForwards(ChatActivity chatActivity) {
        return isEnabled()
                && chatActivity != null
                && chatActivity.getDialogId() != UserObject.VERIFY
                && chatActivity.getCurrentEncryptedChat() == null;
    }

    public static boolean isCopyRestricted(ChatActivity chatActivity, MessageObject messageObject) {
        if (messageObject != null && messageObject.hasRevealedExtendedMedia()) {
            return true;
        }
        if (shouldBypassNoForwards(chatActivity)) {
            return false;
        }
        return isNoForwards(chatActivity, messageObject);
    }

    public static boolean isCopyOrSaveRestricted(ChatActivity chatActivity, MessageObject messageObject, boolean includePaidMedia) {
        if (messageObject != null) {
            if (messageObject.hasRevealedExtendedMedia()) {
                return true;
            }
            if (includePaidMedia && messageObject.type == MessageObject.TYPE_PAID_MEDIA) {
                return true;
            }
        }
        if (shouldBypassNoForwards(chatActivity)) {
            return false;
        }
        return isNoForwards(chatActivity, messageObject);
    }

    public static boolean shouldKeepPhotoViewerSecure(ChatActivity chatActivity, long avatarsDialogId, MessageObject messageObject, int currentAccount) {
        if (chatActivity != null && chatActivity.getCurrentEncryptedChat() != null) {
            return true;
        }
        if (messageObject != null && messageObject.hasRevealedExtendedMedia()) {
            return true;
        }
        if (isEnabled()) {
            return false;
        }
        MessagesController controller = MessagesController.getInstance(currentAccount);
        return avatarsDialogId != 0 && controller.isPeerNoForwards(avatarsDialogId)
                || messageObject != null && (controller.isPeerNoForwards(messageObject.getDialogId())
                || messageObject.messageOwner != null && messageObject.messageOwner.noforwards);
    }

    private static boolean isNoForwards(ChatActivity chatActivity, MessageObject messageObject) {
        boolean peerNoForwards = chatActivity != null && chatActivity.isPeerNoForwards();
        boolean messageNoForwards = messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.noforwards;
        return peerNoForwards || messageNoForwards || chatActivity != null && chatActivity.getDialogId() == UserObject.VERIFY;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
