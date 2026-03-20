package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.patches.FluffyPasskeysUnsupportedPatch;

public final class FluffyPasskeysUnsupportedHook {
    private FluffyPasskeysUnsupportedHook() {
    }

    public static void showUnsupportedAlert(BaseFragment fragment) {
        FluffyPasskeysUnsupportedPatch.showUnsupportedAlert(fragment);
    }
}
