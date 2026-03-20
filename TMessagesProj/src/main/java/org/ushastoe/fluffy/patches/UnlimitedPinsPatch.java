package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;

public final class UnlimitedPinsPatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "unlimited_unarchived_pins_enabled";
    private static final int LOCAL_PIN_LIMIT = 100;

    private UnlimitedPinsPatch() {
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
            setEnabled(object.optBoolean(KEY_ENABLED, false));
        } catch (Exception ignore) {
        }
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

    public static int overrideDialogsPinnedLimit(int currentLimit, int folderId, boolean hasFilter) {
        if (!isEnabled() || folderId != 0 || hasFilter) {
            return currentLimit;
        }
        return Math.max(currentLimit, LOCAL_PIN_LIMIT);
    }

    public static boolean shouldSkipPinnedDialogsSync(int folderId) {
        return isEnabled() && folderId == 0;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
