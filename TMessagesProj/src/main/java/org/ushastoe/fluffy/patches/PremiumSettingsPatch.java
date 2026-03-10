package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public final class PremiumSettingsPatch {
    private static final String PREFS_NAME = "fluffy_premium_settings";
    private static final String KEY_LOCAL_ANON_STORY_VIEW = "local_anon_story_view";

    private PremiumSettingsPatch() {
    }

    public static boolean useLocalAnonymousStoryView() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_LOCAL_ANON_STORY_VIEW, false);
    }

    public static void setUseLocalAnonymousStoryView(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_LOCAL_ANON_STORY_VIEW, enabled).apply();
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
