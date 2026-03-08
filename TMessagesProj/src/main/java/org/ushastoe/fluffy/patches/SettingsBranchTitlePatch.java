package org.ushastoe.fluffy.patches;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.SettingsActivity;

import java.util.List;

public final class SettingsBranchTitlePatch {

    private SettingsBranchTitlePatch() {
    }

    public static void attachIfNeeded(BaseFragment fragment, ActionBar actionBar) {
        if (!isSettingsSubpage(fragment) || actionBar == null) {
            return;
        }
        DialogsCenteredTitlePatch.attach(actionBar);
    }

    private static boolean isSettingsSubpage(BaseFragment fragment) {
        if (fragment == null || fragment instanceof SettingsActivity || fragment.getParentLayout() == null) {
            return false;
        }

        List<BaseFragment> fragmentStack = fragment.getParentLayout().getFragmentStack();
        if (fragmentStack == null || fragmentStack.isEmpty()) {
            return false;
        }

        for (BaseFragment stackFragment : fragmentStack) {
            if (stackFragment != fragment && stackFragment instanceof SettingsActivity) {
                return true;
            }
            if (stackFragment instanceof MainTabsActivity
                    && ((MainTabsActivity) stackFragment).getCurrentVisibleFragment() instanceof SettingsActivity) {
                return true;
            }
        }
        return false;
    }
}
