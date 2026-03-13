package org.ushastoe.fluffy.hooks;

import android.content.Intent;
import android.net.Uri;

import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;

public final class FluffySettingsDeepLinkHook {

    private FluffySettingsDeepLinkHook() {
    }

    public static boolean handleIntent(LaunchActivity activity, Intent intent) {
        return FluffySettingsDeepLinkPatch.handleIntent(activity, intent);
    }

    public static String[] getAutoLinkSchemes() {
        return FluffySettingsDeepLinkPatch.getAutoLinkSchemes();
    }

    public static boolean isInternalUri(Uri uri) {
        return FluffySettingsDeepLinkPatch.isInternalUri(uri);
    }

    public static boolean containsDeepLink(CharSequence text) {
        return FluffySettingsDeepLinkPatch.containsDeepLink(text);
    }

    public static boolean addCustomLinks(android.text.Spannable text, boolean removeOldReplacements) {
        return FluffySettingsDeepLinkPatch.addCustomLinks(text, removeOldReplacements);
    }

    public static boolean shouldForceLinkParse(CharSequence text) {
        return FluffySettingsDeepLinkPatch.shouldForceLinkParse(text);
    }
}
