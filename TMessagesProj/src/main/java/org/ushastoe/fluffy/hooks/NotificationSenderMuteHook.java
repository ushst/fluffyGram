package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.NotificationSenderMutePatch;

public final class NotificationSenderMuteHook {

    private NotificationSenderMuteHook() {
    }

    public static boolean shouldSuppressNotification(int currentAccount, MessageObject messageObject) {
        return NotificationSenderMutePatch.shouldSuppressNotification(currentAccount, messageObject);
    }
}
