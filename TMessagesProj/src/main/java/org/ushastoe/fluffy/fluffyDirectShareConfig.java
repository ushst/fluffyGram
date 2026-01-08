package org.ushastoe.fluffy;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * Stores appearance-related settings for the Direct Share folders UI.
 */
public final class fluffyDirectShareConfig {

    private static final String PREFS_NAME = "fluffyDirectShareConfig";

    private static final String KEY_TABS_HIDE_ALL_CHATS = "tabsHideAllChats";
    private static final String KEY_TAB_STYLE = "tabStyle";
    private static final String KEY_TAB_MODE = "tabMode";

    public static final int TAB_STYLE_CLASSIC = 0;
    public static final int TAB_STYLE_VKUI = 1;

    public static final int TAB_MODE_TEXT = 0;
    public static final int TAB_MODE_ICON = 1;

    private static SharedPreferences preferences;

    private static boolean tabsHideAllChats;
    private static int tabStyle = TAB_STYLE_CLASSIC;
    private static int tabMode = TAB_MODE_TEXT;

    private fluffyDirectShareConfig() {
    }

    private static SharedPreferences getPreferences() {
        if (preferences == null) {
            preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
            load();
        }
        return preferences;
    }

    public static void load() {
        SharedPreferences prefs = getPreferences();
        tabsHideAllChats = prefs.getBoolean(KEY_TABS_HIDE_ALL_CHATS, false);
        tabStyle = prefs.getInt(KEY_TAB_STYLE, TAB_STYLE_CLASSIC);
        tabMode = prefs.getInt(KEY_TAB_MODE, TAB_MODE_TEXT);
    }

    public static boolean getTabsHideAllChats() {
        getPreferences();
        return tabsHideAllChats;
    }

    public static void setTabsHideAllChats(boolean hideAllChats) {
        tabsHideAllChats = hideAllChats;
        getPreferences().edit().putBoolean(KEY_TABS_HIDE_ALL_CHATS, hideAllChats).apply();
    }

    public static int getTabStyle() {
        getPreferences();
        return tabStyle;
    }

    public static void setTabStyle(int style) {
        tabStyle = style;
        getPreferences().edit().putInt(KEY_TAB_STYLE, style).apply();
    }

    public static int getTabMode() {
        getPreferences();
        return tabMode;
    }

    public static void setTabMode(int mode) {
        tabMode = mode;
        getPreferences().edit().putInt(KEY_TAB_MODE, mode).apply();
    }
}
