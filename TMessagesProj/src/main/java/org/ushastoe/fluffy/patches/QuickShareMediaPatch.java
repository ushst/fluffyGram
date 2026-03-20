package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import org.json.JSONObject;

import android.text.TextUtils;

public final class QuickShareMediaPatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "quick_share_private_media_enabled";

    private QuickShareMediaPatch() {
    }

    public static String exportSettingsJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_ENABLED, isEnabled());
        } catch (Exception ignore) {
        }
        return object.toString();
    }

    public static void importSettingsJson(String json) {
        try {
            JSONObject object = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
            setEnabled(object.optBoolean(KEY_ENABLED, true));
        } catch (Exception ignore) {
        }
    }

    public static boolean isEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean shouldDrawForPrivateChat(MessageObject messageObject, TLRPC.User user) {
        if (!isEnabled()) {
            return false;
        }
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

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
