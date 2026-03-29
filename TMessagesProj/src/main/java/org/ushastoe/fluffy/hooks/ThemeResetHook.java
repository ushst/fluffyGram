package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.patches.ThemeResetPatch;

public final class ThemeResetHook {

    private ThemeResetHook() {
    }

    public static boolean openSystemAppSettings(BaseFragment fragment) {
        return ThemeResetPatch.openSystemAppSettings(fragment);
    }
}
