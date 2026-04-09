package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

public final class GlobalLogsPatch {

    private static final String PREFS_NAME = "systemConfig";
    private static final String KEY_LOGS_ENABLED = "logsEnabled";

    private GlobalLogsPatch() {
    }

    public static void onApplicationCreated() {
        BuildVars.LOGS_ENABLED = isLogsEnabled();
    }

    public static boolean isLogsEnabled() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return BuildVars.LOGS_ENABLED;
        }
        return preferences.getBoolean(KEY_LOGS_ENABLED, BuildVars.DEBUG_VERSION);
    }

    public static void setLogsEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_LOGS_ENABLED, enabled).commit();
        }
        BuildVars.LOGS_ENABLED = enabled;
        if (enabled) {
            FileLog.d("fluffy_global_logs enabled");
        }
    }

    private static SharedPreferences getPreferences() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
