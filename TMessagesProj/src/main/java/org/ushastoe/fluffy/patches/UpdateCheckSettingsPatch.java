package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public final class UpdateCheckSettingsPatch {

    private static final String PREFS_NAME = "fluffy_update_settings";
    private static final String KEY_AUTO_CHECK_MODE = "auto_check_mode";

    public static final int AUTO_CHECK_NEVER = 0;
    public static final int AUTO_CHECK_ON_LAUNCH = 1;

    private UpdateCheckSettingsPatch() {
    }

    public static int getAutoCheckMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return AUTO_CHECK_ON_LAUNCH;
        }
        int mode = preferences.getInt(KEY_AUTO_CHECK_MODE, AUTO_CHECK_ON_LAUNCH);
        if (mode != AUTO_CHECK_NEVER && mode != AUTO_CHECK_ON_LAUNCH) {
            return AUTO_CHECK_ON_LAUNCH;
        }
        return mode;
    }

    public static void setAutoCheckMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        int sanitizedMode = mode == AUTO_CHECK_NEVER ? AUTO_CHECK_NEVER : AUTO_CHECK_ON_LAUNCH;
        preferences.edit().putInt(KEY_AUTO_CHECK_MODE, sanitizedMode).apply();
    }

    public static boolean shouldCheckOnLaunch() {
        return getAutoCheckMode() == AUTO_CHECK_ON_LAUNCH;
    }

    private static SharedPreferences getPreferences() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}