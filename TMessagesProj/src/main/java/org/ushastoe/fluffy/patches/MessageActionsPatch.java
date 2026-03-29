package org.ushastoe.fluffy.patches;

import android.content.SharedPreferences;

import org.telegram.messenger.MessagesController;

/**
 * Patch for message action settings storage and logic.
 * Provides the implementation backend for MessageActionsHook.
 *
 * Storage pattern:
 * - Use MessagesController.getInstance(0).getMainSettings() for SharedPreferences
 * - Key format: "feature_name_enabled" (lowercase_underscore)
 * - Default: true (enable new features by default)
 *
 * Logic pattern:
 * - Implement the actual feature behavior here
 * - Keep Telegram core patches minimal (only hook calls)
 * - Return full implementation to this class
 */
public final class MessageActionsPatch {

    private static final String KEY_MESSAGE_DETAILS_ENABLED = "message_details_enabled";
    private static final String KEY_MESSAGE_TRANSLIT_ENABLED = "message_translit_enabled";
    private static final String KEY_LOCAL_MESSAGE_HISTORY_ENABLED = "local_message_history_enabled";
    private static final String KEY_MESSAGE_GOOGLE_AI_ENABLED = "message_google_ai_enabled";
    private static final String KEY_MESSAGE_STATS_ENABLED = "message_stats_enabled";

    private MessageActionsPatch() {
    }

    // ============================================
    // Shared Preferences Helper
    // ============================================

    private static SharedPreferences getPreferences() {
        return MessagesController.getGlobalMainSettings();
    }

    public static boolean isMessageDetailsEnabled() {
        return getPreferences().getBoolean(KEY_MESSAGE_DETAILS_ENABLED, true);
    }

    public static void setMessageDetailsEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(KEY_MESSAGE_DETAILS_ENABLED, enabled).apply();
    }

    public static boolean isMessageTranslitEnabled() {
        return getPreferences().getBoolean(KEY_MESSAGE_TRANSLIT_ENABLED, true);
    }

    public static void setMessageTranslitEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(KEY_MESSAGE_TRANSLIT_ENABLED, enabled).apply();
    }

    public static boolean isLocalMessageHistoryEnabled() {
        return getPreferences().getBoolean(KEY_LOCAL_MESSAGE_HISTORY_ENABLED, true);
    }

    public static void setLocalMessageHistoryEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(KEY_LOCAL_MESSAGE_HISTORY_ENABLED, enabled).apply();
    }

    public static boolean isMessageGoogleAiEnabled() {
        return getPreferences().getBoolean(KEY_MESSAGE_GOOGLE_AI_ENABLED, true);
    }

    public static void setMessageGoogleAiEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(KEY_MESSAGE_GOOGLE_AI_ENABLED, enabled).apply();
    }

    public static boolean isMessageStatsEnabled() {
        return getPreferences().getBoolean(KEY_MESSAGE_STATS_ENABLED, true);
    }

    public static void setMessageStatsEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(KEY_MESSAGE_STATS_ENABLED, enabled).apply();
    }

}
