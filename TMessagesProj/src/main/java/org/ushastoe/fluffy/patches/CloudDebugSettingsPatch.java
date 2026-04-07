package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public final class CloudDebugSettingsPatch {

    private static final String PREFS_NAME = "fluffy_debug_settings";
    public static final String KEY_SELFHOSTED_CLOUD_ENABLED = "selfhosted_cloud_enabled";

    private CloudDebugSettingsPatch() {
    }

    public static boolean isSelfhostedCloudEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_SELFHOSTED_CLOUD_ENABLED, false);
    }

    public static void setSelfhostedCloudEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_SELFHOSTED_CLOUD_ENABLED, enabled).apply();
    }

    private static SharedPreferences getPreferences() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
