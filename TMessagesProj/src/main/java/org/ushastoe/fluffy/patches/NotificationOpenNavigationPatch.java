package org.ushastoe.fluffy.patches;

import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ChatActivity;
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

    // Telegram closes every open chat before opening a chat from a notification,
    // so Back no longer returns to the chat the user was reading. Keep the stack
    // and only replace the top chat when it is the same dialog (avoids a duplicate).
    public static void closeChatsForPush(LaunchActivity activity, long dialogId, long topicId) {
        if (activity == null) {
            return;
        }
        INavigationLayout layout = activity.getActionBarLayout();
        if (layout == null) {
            return;
        }
        BaseFragment last = layout.getLastFragment();
        if (last instanceof ChatActivity) {
            ChatActivity chat = (ChatActivity) last;
            if (chat.getDialogId() == dialogId && chat.getTopicId() == topicId) {
                chat.removeSelfFromStack(true);
            }
        }
    }
}
