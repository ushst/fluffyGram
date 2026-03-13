package org.ushastoe.fluffy.patches;

import android.content.Intent;
import android.net.Uri;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.ui.FluffyAppearanceActivity;
import org.ushastoe.fluffy.ui.FluffyDebugActivity;
import org.ushastoe.fluffy.ui.FluffyPremiumActivity;
import org.ushastoe.fluffy.ui.FluffySettingsActivity;
import org.ushastoe.fluffy.ui.FluffyTabsActivity;

import java.util.List;

public final class FluffySettingsDeepLinkPatch {

    public static final String SCHEME = "fluffy";
    public static final String SETTINGS_HOST = "settings";

    private FluffySettingsDeepLinkPatch() {
    }

    public static boolean handleIntent(LaunchActivity activity, Intent intent) {
        if (activity == null || intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return false;
        }
        Uri data = intent.getData();
        if (data == null || !SCHEME.equalsIgnoreCase(data.getScheme()) || !SETTINGS_HOST.equalsIgnoreCase(data.getHost())) {
            return false;
        }

        BaseFragment fragment = createSettingsFragment(data.getPathSegments());
        if (fragment == null) {
            return false;
        }

        intent.setAction(null);
        intent.setData(null);
        activity.presentFragment(fragment);
        return true;
    }

    public static String buildSettingsLink(String... pathSegments) {
        StringBuilder builder = new StringBuilder(SCHEME).append("://").append(SETTINGS_HOST);
        if (pathSegments == null) {
            return builder.toString();
        }
        for (String pathSegment : pathSegments) {
            if (pathSegment == null || pathSegment.isEmpty()) {
                continue;
            }
            builder.append('/').append(pathSegment);
        }
        return builder.toString();
    }

    public static boolean copyLink(BaseFragment fragment, String link) {
        if (fragment == null || link == null || link.isEmpty() || !AndroidUtilities.addToClipboard(link)) {
            return false;
        }
        BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString(R.string.LinkCopied)).show();
        return true;
    }

    private static BaseFragment createSettingsFragment(List<String> segments) {
        if (segments == null || segments.isEmpty()) {
            return new FluffySettingsActivity();
        }

        String root = segments.get(0);
        if ("appearance".equals(root)) {
            if (segments.size() >= 2 && "tabs".equals(segments.get(1))) {
                return new FluffyTabsActivity();
            }
            return FluffyAppearanceActivity.createForTarget(joinSegments(segments, 1));
        }
        if ("premium".equals(root)) {
            return FluffyPremiumActivity.createForTarget(joinSegments(segments, 1));
        }
        if ("debug".equals(root)) {
            return FluffyDebugActivity.createForTarget(joinSegments(segments, 1));
        }
        return FluffySettingsActivity.createForTarget(root);
    }

    private static String joinSegments(List<String> segments, int startIndex) {
        if (segments == null || startIndex >= segments.size()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < segments.size(); i++) {
            String segment = segments.get(i);
            if (segment == null || segment.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('/');
            }
            builder.append(segment);
        }
        return builder.length() == 0 ? null : builder.toString();
    }
}
