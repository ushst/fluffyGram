package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.UpdateCheckSettingsPatch;

public final class UpdateCheckSettingsHook {

    public static final int AUTO_CHECK_NEVER = UpdateCheckSettingsPatch.AUTO_CHECK_NEVER;
    public static final int AUTO_CHECK_ON_LAUNCH = UpdateCheckSettingsPatch.AUTO_CHECK_ON_LAUNCH;

    private UpdateCheckSettingsHook() {
    }

    public static int getAutoCheckMode() {
        return UpdateCheckSettingsPatch.getAutoCheckMode();
    }

    public static void setAutoCheckMode(int mode) {
        UpdateCheckSettingsPatch.setAutoCheckMode(mode);
    }

    public static boolean shouldCheckOnLaunch() {
        return UpdateCheckSettingsPatch.shouldCheckOnLaunch();
    }
}