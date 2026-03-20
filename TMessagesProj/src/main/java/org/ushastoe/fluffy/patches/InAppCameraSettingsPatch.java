package org.ushastoe.fluffy.patches;

import org.telegram.messenger.SharedConfig;

public final class InAppCameraSettingsPatch {

    private InAppCameraSettingsPatch() {
    }

    public static boolean isEnabled() {
        return SharedConfig.inappCamera;
    }

    public static void setEnabled(boolean enabled) {
        if (SharedConfig.inappCamera != enabled) {
            SharedConfig.toggleInappCamera();
        }
    }
}
