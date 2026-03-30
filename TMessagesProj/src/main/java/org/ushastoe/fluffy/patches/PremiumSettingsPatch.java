package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;

public final class PremiumSettingsPatch {
    private static final String PREFS_NAME = "fluffy_premium_settings";
    private static final String KEY_LOCAL_ANON_STORY_VIEW = "local_anon_story_view";
    private static final String KEY_LOCAL_MESSAGE_FAKE_EDIT_ENABLED = "local_message_fake_edit_enabled";
    private static final String KEY_LOCAL_MESSAGE_HISTORY_ENABLED = "local_message_history_enabled";
    private static final String KEY_SAVE_DELETED_MESSAGES_ENABLED = "save_deleted_messages_enabled";
    private static final String KEY_DELETED_MESSAGE_MARKER_MODE = "deleted_message_marker_mode";
    private static final String KEY_DOCUMENT_AUTHOR_MARKER_MODE = "document_author_marker_mode";

    public static final int DELETED_MESSAGE_MARKER_MODE_TEXT = 0;
    public static final int DELETED_MESSAGE_MARKER_MODE_SHORT_TEXT = 1;
    public static final int DELETED_MESSAGE_MARKER_MODE_ICON = 2;
    public static final int DOCUMENT_AUTHOR_MARKER_MODE_TEXT = 0;
    public static final int DOCUMENT_AUTHOR_MARKER_MODE_SHORT_TEXT = 1;
    public static final int DOCUMENT_AUTHOR_MARKER_MODE_ICON = 2;

    private PremiumSettingsPatch() {
    }

    public static String exportSettingsJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_LOCAL_ANON_STORY_VIEW, useLocalAnonymousStoryView());
            object.put(KEY_LOCAL_MESSAGE_HISTORY_ENABLED, isLocalMessageHistoryEnabled());
            object.put(KEY_SAVE_DELETED_MESSAGES_ENABLED, isSaveDeletedMessagesEnabled());
            object.put(KEY_DELETED_MESSAGE_MARKER_MODE, getDeletedMessageMarkerMode());
            object.put(KEY_DOCUMENT_AUTHOR_MARKER_MODE, getDocumentAuthorMarkerMode());
        } catch (Exception ignore) {
        }
        return object.toString();
    }

    public static void importSettingsJson(String json) {
        try {
            JSONObject object = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
            setUseLocalAnonymousStoryView(object.optBoolean(KEY_LOCAL_ANON_STORY_VIEW, false));
            setLocalMessageHistoryEnabled(object.optBoolean(KEY_LOCAL_MESSAGE_HISTORY_ENABLED, false));
            setSaveDeletedMessagesEnabled(object.optBoolean(KEY_SAVE_DELETED_MESSAGES_ENABLED, false));
            setDeletedMessageMarkerMode(object.optInt(KEY_DELETED_MESSAGE_MARKER_MODE, DELETED_MESSAGE_MARKER_MODE_ICON));
            setDocumentAuthorMarkerMode(object.optInt(KEY_DOCUMENT_AUTHOR_MARKER_MODE, DOCUMENT_AUTHOR_MARKER_MODE_ICON));
        } catch (Exception ignore) {
        }
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

    public static boolean isLocalMessageFakeEditEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_LOCAL_MESSAGE_FAKE_EDIT_ENABLED, false);
    }

    public static void setLocalMessageFakeEditEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_LOCAL_MESSAGE_FAKE_EDIT_ENABLED, enabled).apply();
    }

    public static boolean isLocalMessageHistoryEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_LOCAL_MESSAGE_HISTORY_ENABLED, false);
    }

    public static void setLocalMessageHistoryEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_LOCAL_MESSAGE_HISTORY_ENABLED, enabled).apply();
    }

    public static boolean isSaveDeletedMessagesEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_SAVE_DELETED_MESSAGES_ENABLED, false);
    }

    public static void setSaveDeletedMessagesEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_SAVE_DELETED_MESSAGES_ENABLED, enabled).apply();
    }

    public static int getDeletedMessageMarkerMode() {
        SharedPreferences preferences = getPreferences();
        return clampDeletedMessageMarkerMode(preferences != null ? preferences.getInt(KEY_DELETED_MESSAGE_MARKER_MODE, DELETED_MESSAGE_MARKER_MODE_ICON) : DELETED_MESSAGE_MARKER_MODE_ICON);
    }

    public static void setDeletedMessageMarkerMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DELETED_MESSAGE_MARKER_MODE, clampDeletedMessageMarkerMode(mode)).apply();
    }

    public static int getDocumentAuthorMarkerMode() {
        SharedPreferences preferences = getPreferences();
        return clampDocumentAuthorMarkerMode(preferences != null ? preferences.getInt(KEY_DOCUMENT_AUTHOR_MARKER_MODE, DOCUMENT_AUTHOR_MARKER_MODE_ICON) : DOCUMENT_AUTHOR_MARKER_MODE_ICON);
    }

    public static void setDocumentAuthorMarkerMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DOCUMENT_AUTHOR_MARKER_MODE, clampDocumentAuthorMarkerMode(mode)).apply();
    }

    private static int clampDeletedMessageMarkerMode(int mode) {
        if (mode == DELETED_MESSAGE_MARKER_MODE_TEXT
                || mode == DELETED_MESSAGE_MARKER_MODE_SHORT_TEXT
                || mode == DELETED_MESSAGE_MARKER_MODE_ICON) {
            return mode;
        }
        return DELETED_MESSAGE_MARKER_MODE_ICON;
    }

    private static int clampDocumentAuthorMarkerMode(int mode) {
        if (mode == DOCUMENT_AUTHOR_MARKER_MODE_TEXT
                || mode == DOCUMENT_AUTHOR_MARKER_MODE_SHORT_TEXT
                || mode == DOCUMENT_AUTHOR_MARKER_MODE_ICON) {
            return mode;
        }
        return DOCUMENT_AUTHOR_MARKER_MODE_ICON;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
