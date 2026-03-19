package org.ushastoe.fluffy.patches;

import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.URLSpan;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.hooks.PremiumAccessHook;
import org.ushastoe.fluffy.ui.FluffyAppearanceActivity;
import org.ushastoe.fluffy.ui.FluffyCloudSettingsActivity;
import org.ushastoe.fluffy.ui.FluffyDebugActivity;
import org.ushastoe.fluffy.ui.FluffyPremiumActivity;
import org.ushastoe.fluffy.ui.FluffySettingsActivity;
import org.ushastoe.fluffy.ui.FluffyTabsActivity;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FluffySettingsDeepLinkPatch {

    public static final String SCHEME = "fluffy";
    public static final String SETTINGS_HOST = "settings";
    public static final String PREMIUM_ACCESS_HOST = "premium-access";
    private static final String[] AUTO_LINK_SCHEMES = new String[]{"http://", "https://", "tg://", "tonsite://", SCHEME + "://"};
    private static final Pattern DEEP_LINK_PATTERN = Pattern.compile("(?i)\\bfluffy://[\\w\\-./?%&=+#:~]+");

    private FluffySettingsDeepLinkPatch() {
    }

    public static boolean handleIntent(LaunchActivity activity, Intent intent) {
        if (activity == null || intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) {
            return false;
        }
        Uri data = intent.getData();
        if (data == null || !SCHEME.equalsIgnoreCase(data.getScheme())) {
            return false;
        }

        if (PREMIUM_ACCESS_HOST.equalsIgnoreCase(data.getHost())) {
            return handlePremiumAccessIntent(activity, intent, data);
        }
        if (!SETTINGS_HOST.equalsIgnoreCase(data.getHost())) {
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

    public static String[] getAutoLinkSchemes() {
        return AUTO_LINK_SCHEMES;
    }

    public static boolean isInternalUri(Uri uri) {
        return uri != null && SCHEME.equalsIgnoreCase(uri.getScheme());
    }

    public static boolean containsDeepLink(CharSequence text) {
        return text != null && DEEP_LINK_PATTERN.matcher(text).find();
    }

    public static boolean shouldForceLinkParse(CharSequence text) {
        return containsDeepLink(text);
    }

    public static boolean addCustomLinks(Spannable text, boolean removeOldReplacements) {
        if (text == null) {
            return false;
        }
        boolean added = false;
        Matcher matcher = DEEP_LINK_PATTERN.matcher(text);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            URLSpan[] spans = text.getSpans(start, end, URLSpan.class);
            if (spans != null) {
                for (URLSpan span : spans) {
                    if (!(span instanceof URLSpanNoUnderline) || removeOldReplacements) {
                        text.removeSpan(span);
                    }
                }
            }
            String url = matcher.group();
            text.setSpan(new URLSpanNoUnderline(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            added = true;
        }
        return added;
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
            if (!PremiumAccessHook.hasPremiumAccess()) {
                return new FluffySettingsActivity();
            }
            return FluffyPremiumActivity.createForTarget(joinSegments(segments, 1));
        }
        if ("cloud".equals(root)) {
            return FluffyCloudSettingsActivity.createForTarget(joinSegments(segments, 1));
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

    private static boolean handlePremiumAccessIntent(LaunchActivity activity, Intent intent, Uri data) {
        String token = data.getQueryParameter("token");
        if (PremiumAccessHook.importPremiumAccessToken(token)) {
            intent.setAction(null);
            intent.setData(null);
            showPremiumBullet(activity, true);
            return true;
        }
        showPremiumBullet(activity, false);
        return true;
    }

    private static void showPremiumBullet(LaunchActivity activity, boolean success) {
        Runnable openSettings = () -> activity.presentFragment(new FluffySettingsActivity());
        int icon = success ? R.raw.info : R.raw.error;
        String message = LocaleController.getString(success
                ? R.string.FluffyPremiumAccessGranted
                : R.string.FluffyPremiumAccessDenied);
        BulletinFactory.global().createSimpleBulletin(icon, message,
                LocaleController.getString(R.string.FluffyPremiumGoToSettings), openSettings).show();
    }
}
