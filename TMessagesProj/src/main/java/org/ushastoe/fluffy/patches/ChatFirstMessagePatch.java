package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ChatActivity;

public final class ChatFirstMessagePatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "chat_go_to_first_message_enabled";
    private static final int FIRST_HISTORY_DATE = 1;

    private ChatFirstMessagePatch() {
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
        return preferences == null || preferences.getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void addChatMenuItem(ActionBarMenuItem headerItem, int menuId) {
        if (headerItem == null || headerItem.hasSubItem(menuId)) {
            return;
        }
        headerItem.lazilyAddSubItem(menuId, R.drawable.msg_home, LocaleController.getString(R.string.FluffyGoToFirstMessage));
    }

    public static void updateChatMenuItem(ActionBarMenuItem headerItem, int menuId) {
        if (headerItem == null) {
            return;
        }
        headerItem.setSubItemShown(menuId, isEnabled());
    }

    public static boolean onChatMenuItemClick(ChatActivity chatActivity, ActionBarMenuItem headerItem, int id, int menuId) {
        if (id != menuId || chatActivity == null) {
            return false;
        }
        if (headerItem != null) {
            headerItem.toggleSubMenu();
        }
        chatActivity.jumpToDate(FIRST_HISTORY_DATE);
        return true;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
