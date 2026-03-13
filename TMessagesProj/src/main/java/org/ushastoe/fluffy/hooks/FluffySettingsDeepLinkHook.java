package org.ushastoe.fluffy.hooks;

import android.content.Intent;

import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;

public final class FluffySettingsDeepLinkHook {

    private FluffySettingsDeepLinkHook() {
    }

    public static boolean handleIntent(LaunchActivity activity, Intent intent) {
        return FluffySettingsDeepLinkPatch.handleIntent(activity, intent);
    }
}
