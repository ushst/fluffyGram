package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.patches.FluffyLocalLogPatch;
import org.ushastoe.fluffy.patches.GlobalLogsPatch;

public final class FluffyLocalLogHook {
    private FluffyLocalLogHook() {
    }

    public static void onApplicationCreated(ApplicationLoader applicationLoader) {
        GlobalLogsPatch.onApplicationCreated();
        FluffyLocalLogPatch.onApplicationCreated(applicationLoader);
    }

    public static void onLaunchActivityResumed(LaunchActivity target) {
        FluffyLocalLogPatch.onLaunchActivityResumed(target);
    }

    public static void onSelectedAccountChanged(LaunchActivity target, int account) {
        FluffyLocalLogPatch.onSelectedAccountChanged(target, account);
    }

    public static void onSaveLogClicked(BaseFragment fragment) {
        FluffyLocalLogPatch.onSaveLogClicked(fragment);
    }
}
