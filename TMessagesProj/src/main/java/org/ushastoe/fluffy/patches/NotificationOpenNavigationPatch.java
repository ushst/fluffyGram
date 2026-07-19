package org.ushastoe.fluffy.patches;

import org.telegram.messenger.UserConfig;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.MainTabsActivity;

public final class NotificationOpenNavigationPatch {

    private NotificationOpenNavigationPatch() {
    }

    public static void ensureMainTabsRoot(LaunchActivity activity, int account, boolean needsChatOpen) {
        if (activity == null || !needsChatOpen) {
            return;
        }
        if (!UserConfig.getInstance(account).isClientActivated()) {
            return;
        }
        if (activity.getMainFragmentsStackSize() > 0) {
            return;
        }

        MainTabsActivity mainTabsActivity = new MainTabsActivity();
        mainTabsActivity.prepareDialogsActivity(null);
        activity.fluffyAddMainTabsRoot(mainTabsActivity);
    }
}
