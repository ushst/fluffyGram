package org.ushastoe.fluffy.hooks;

import androidx.core.app.NotificationCompat;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.NotificationReactionPatch;

public final class NotificationReactionHook {

    private NotificationReactionHook() {
    }

    public static NotificationCompat.Action createHeartReactionAction(MessageObject messageObject, long dialogId, int maxId, long topicId, int currentAccount, int requestCode, boolean waitingForPasscode, boolean isStory) {
        return NotificationReactionPatch.createHeartReactionAction(messageObject, dialogId, maxId, topicId, currentAccount, requestCode, waitingForPasscode, isStory);
    }

    public static void handleHeartReactionIntent(long dialogId, int maxId, long topicId, int currentAccount) {
        NotificationReactionPatch.handleHeartReactionIntent(dialogId, maxId, topicId, currentAccount);
    }
}
