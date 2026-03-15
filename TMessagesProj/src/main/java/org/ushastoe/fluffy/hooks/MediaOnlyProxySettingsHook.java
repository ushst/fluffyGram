package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.MediaOnlyProxyPatch;

public final class MediaOnlyProxySettingsHook {
    private MediaOnlyProxySettingsHook() {
    }

    public static boolean isEnabled() {
        return MediaOnlyProxyPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        MediaOnlyProxyPatch.setEnabled(enabled);
    }
}
