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

    public static boolean isMessageDetailsEnabled() {
        return MessageActionsPatch.isMessageDetailsEnabled();
    }

    public static void setMessageDetailsEnabled(boolean enabled) {
        MessageActionsPatch.setMessageDetailsEnabled(enabled);
    }

    public static boolean isMessageTranslitEnabled() {
        return MessageActionsPatch.isMessageTranslitEnabled();
    }

    public static void setMessageTranslitEnabled(boolean enabled) {
        MessageActionsPatch.setMessageTranslitEnabled(enabled);
    }

    public static boolean isLocalMessageHistoryEnabled() {
        return MessageActionsPatch.isLocalMessageHistoryEnabled();
    }

    public static void setLocalMessageHistoryEnabled(boolean enabled) {
        MessageActionsPatch.setLocalMessageHistoryEnabled(enabled);
    }

}
