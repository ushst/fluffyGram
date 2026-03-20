package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.InAppCameraSettingsPatch;

public final class InAppCameraSettingsHook {

    private InAppCameraSettingsHook() {
    }

    public static boolean isEnabled() {
        return InAppCameraSettingsPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        InAppCameraSettingsPatch.setEnabled(enabled);
    }
}
