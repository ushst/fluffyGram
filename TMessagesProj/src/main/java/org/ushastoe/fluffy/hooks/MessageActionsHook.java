package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.MessageActionsPatch;

/**
 * Hook interface for message action toggles and settings.
 * All message actions must expose their enable/disable state through this hook.
 *
 * Usage:
 * - Call isXxxEnabled() to check if a feature is active
 * - Call setXxxEnabled() to toggle a feature
 * - Add corresponding UI controls in FluffyAppearanceActivity or FluffyMessageActionsActivity
 *
 * Pattern:
 * 1. Define hook method here
 * 2. Implement in MessageActionsPatch with SharedPreferences storage
 * 3. Add UI checkbox in settings activity
 * 4. Use hook in Telegram core message context menu builder
 */
public final class MessageActionsHook {

    private MessageActionsHook() {
    }

    // ============================================
    // Template for new message action features
    // ============================================
    // Uncomment and customize for each new feature:
    //
    // public static boolean isMyFeatureEnabled() {
    //     return MessageActionsPatch.isMyFeatureEnabled();
    // }
    //
    // public static void setMyFeatureEnabled(boolean enabled) {
    //     MessageActionsPatch.setMyFeatureEnabled(enabled);
    // }
    //
    // ============================================

}
