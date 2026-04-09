package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.NotificationDiagnosticsPatch;

import java.util.ArrayList;

public final class NotificationDiagnosticsHook {

    private NotificationDiagnosticsHook() {
    }

    public static void onProcessNewMessages(int currentAccount, ArrayList<MessageObject> messageObjects, boolean isLast, boolean isFcm) {
        NotificationDiagnosticsPatch.onProcessNewMessages(currentAccount, messageObjects, isLast, isFcm);
    }

    public static void onShowOrUpdateNotificationStart(int currentAccount, boolean notifyAboutLast, int pushCount, int storyPushCount) {
        NotificationDiagnosticsPatch.onShowOrUpdateNotificationStart(currentAccount, notifyAboutLast, pushCount, storyPushCount);
    }

    public static void onShowOrUpdateNotificationSkipped(int currentAccount, String reason, int pushCount, int storyPushCount) {
        NotificationDiagnosticsPatch.onShowOrUpdateNotificationSkipped(currentAccount, reason, pushCount, storyPushCount);
    }

    public static void onShowOrUpdateNotificationResolved(int currentAccount, long dialogId, int messageId, boolean isStory, long maxDate) {
        NotificationDiagnosticsPatch.onShowOrUpdateNotificationResolved(currentAccount, dialogId, messageId, isStory, maxDate);
    }

    public static void onSummaryNotify(int currentAccount, int notificationId, long dialogId, long topicId, int pushCount, int storyPushCount) {
        NotificationDiagnosticsPatch.onSummaryNotify(currentAccount, notificationId, dialogId, topicId, pushCount, storyPushCount);
    }

    public static void onDialogNotify(int currentAccount, int notificationId, long dialogId, long topicId, boolean story) {
        NotificationDiagnosticsPatch.onDialogNotify(currentAccount, notificationId, dialogId, topicId, story);
    }
}
