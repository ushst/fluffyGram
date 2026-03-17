package org.ushastoe.fluffy.patches;

import java.util.Locale;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public final class BuildVersionBrandingPatch {

    private BuildVersionBrandingPatch() {}

    public static String formatVersionName(String versionName, int code, String abi, boolean multiline) {
        String appName = LocaleController.getString(R.string.AppName);
        if (appName == null || appName.trim().isEmpty()) {
            appName = "fluffyGram";
        }
        String versionPart = multiline
                ? String.format(Locale.US, "v%s (%d)\n%s", versionName, code, abi)
                : String.format(Locale.US, "v%s (%d) %s", versionName, code, abi);
        return appName + " " + versionPart;
    }
}
