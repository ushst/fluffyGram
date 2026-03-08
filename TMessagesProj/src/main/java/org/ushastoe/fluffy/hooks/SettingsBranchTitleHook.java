package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.patches.SettingsBranchTitlePatch;

public final class SettingsBranchTitleHook {

    private SettingsBranchTitleHook() {
    }

    public static void attachIfNeeded(BaseFragment fragment, ActionBar actionBar) {
        SettingsBranchTitlePatch.attachIfNeeded(fragment, actionBar);
    }
}
