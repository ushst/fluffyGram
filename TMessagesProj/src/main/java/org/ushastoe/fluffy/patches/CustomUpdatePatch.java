package org.ushastoe.fluffy.patches;

import org.telegram.messenger.ApplicationLoader;

public final class CustomUpdatePatch {

    private CustomUpdatePatch() {
    }

    public static boolean shouldAllowLaunchActivityCheck() {
        return ApplicationLoader.applicationLoaderInstance != null
            && ApplicationLoader.applicationLoaderInstance.isCustomUpdate();
    }

    public static boolean shouldCheckCustomUpdateOnLaunch() {
        return UpdateCheckSettingsPatch.shouldCheckOnLaunch();
    }
}
