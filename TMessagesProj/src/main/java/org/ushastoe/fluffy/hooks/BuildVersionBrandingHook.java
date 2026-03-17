package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.BuildVersionBrandingPatch;

public final class BuildVersionBrandingHook {

    private BuildVersionBrandingHook() {}

    public static String formatVersionName(String versionName, int code, String abi, boolean multiline) {
        return BuildVersionBrandingPatch.formatVersionName(versionName, code, abi, multiline);
    }
}
