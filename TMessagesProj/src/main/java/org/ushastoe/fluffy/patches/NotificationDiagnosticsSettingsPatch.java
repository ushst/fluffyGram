package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public final class NotificationDiagnosticsSettingsPatch {

    private static final String PREFS_NAME = "fluffy_debug_settings";
    public static final String KEY_NOTIFICATION_DIAGNOSTICS_ENABLED = "notification_diagnostics_enabled";

    private NotificationDiagnosticsSettingsPatch() {
    }

    public static boolean isNotificationDiagnosticsEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences == null || preferences.getBoolean(KEY_NOTIFICATION_DIAGNOSTICS_ENABLED, true);
    }

    public static void setNotificationDiagnosticsEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_NOTIFICATION_DIAGNOSTICS_ENABLED, enabled).apply();
    }

    private static SharedPreferences getPreferences() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
