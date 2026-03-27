package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationsController;
import org.ushastoe.fluffy.patches.NotificationSenderMutePatch;

public final class NotificationSenderMuteHook {

    private NotificationSenderMuteHook() {
    }

    public static boolean shouldSuppressNotification(int currentAccount, MessageObject messageObject) {
        return NotificationSenderMutePatch.shouldSuppressNotification(currentAccount, messageObject);
    }

    public static boolean isQueuedMessageForSender(int currentAccount, MessageObject messageObject, long dialogId, long senderId) {
        return NotificationSenderMutePatch.isQueuedMessageForSender(currentAccount, messageObject, dialogId, senderId);
    }

    public static void removeQueuedMessagesForMutedSender(int currentAccount, long dialogId, long senderId) {
        NotificationsController.getInstance(currentAccount).removeQueuedMessagesForMutedSender(dialogId, senderId);
    }
}
