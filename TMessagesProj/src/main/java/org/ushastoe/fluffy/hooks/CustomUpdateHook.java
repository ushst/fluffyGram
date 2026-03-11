package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.CustomUpdatePatch;

public final class CustomUpdateHook {

    private CustomUpdateHook() {
    }

    public static boolean shouldAllowLaunchActivityCheck() {
        return CustomUpdatePatch.shouldAllowLaunchActivityCheck();
    }
}
