package org.ushastoe.fluffy.patches;

import org.telegram.messenger.MessagesController;
import android.content.SharedPreferences;

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

    private MessageActionsPatch() {
    }

    // ============================================
    // Shared Preferences Helper
    // ============================================

    private static SharedPreferences getPreferences() {
        return MessagesController.getInstance(0).getMainSettings();
    }

    // ============================================
    // Template for new message action features
    // ============================================
    // Uncomment and customize for each new feature:
    //
    // public static boolean isMyFeatureEnabled() {
    //     return getPreferences().getBoolean("my_feature_enabled", true);
    // }
    //
    // public static void setMyFeatureEnabled(boolean enabled) {
    //     getPreferences().edit()
    //         .putBoolean("my_feature_enabled", enabled)
    //         .apply();
    // }
    //
    // public static void applyMyFeatureLogic(Object... params) {
    //     // Full feature implementation here
    // }
    //
    // ============================================

}
