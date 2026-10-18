package org.ushastoe.fluffy.hooks;

import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.patches.NotificationOpenNavigationPatch;

public final class NotificationOpenNavigationHook {

    private NotificationOpenNavigationHook() {
    }

    public static void ensureMainTabsRoot(LaunchActivity activity, int account, boolean needsChatOpen) {
        NotificationOpenNavigationPatch.ensureMainTabsRoot(activity, account, needsChatOpen);
    }

    public static void closeChatsForPush(LaunchActivity activity, long dialogId, long topicId) {
        NotificationOpenNavigationPatch.closeChatsForPush(activity, dialogId, topicId);
    }
}
