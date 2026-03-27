package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;

public final class InAppCameraSettingsPatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "in_app_camera_enabled";

    private InAppCameraSettingsPatch() {
    }

    public static boolean isEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences == null || preferences.getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean shouldUseInAppCamera() {
        return SharedConfig.inappCamera && isEnabled();
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
