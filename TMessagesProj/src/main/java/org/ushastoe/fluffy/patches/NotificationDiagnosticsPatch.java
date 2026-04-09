package org.ushastoe.fluffy.patches;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.Locale;

public final class NotificationDiagnosticsPatch {

    private static final String PREFIX = "fluffy_notification_diag";

    private NotificationDiagnosticsPatch() {
    }

    public static void onProcessNewMessages(int currentAccount, ArrayList<MessageObject> messageObjects, boolean isLast, boolean isFcm) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        int count = messageObjects == null ? 0 : messageObjects.size();
        if (count <= 0) {
            FileLog.d(String.format(Locale.US, "%s processNewMessages account=%d count=%d isLast=%s isFcm=%s", PREFIX, currentAccount, count, isLast, isFcm));
            return;
        }
        MessageObject first = messageObjects.get(0);
        long dialogId = first != null ? first.getDialogId() : 0L;
        int messageId = first != null ? first.getId() : 0;
        FileLog.d(String.format(Locale.US, "%s processNewMessages account=%d count=%d isLast=%s isFcm=%s firstDialogId=%d firstMessageId=%d", PREFIX, currentAccount, count, isLast, isFcm, dialogId, messageId));
    }

    public static void onShowOrUpdateNotificationStart(int currentAccount, boolean notifyAboutLast, int pushCount, int storyPushCount) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s showOrUpdateNotification:start account=%d notifyAboutLast=%s push=%d storyPush=%d", PREFIX, currentAccount, notifyAboutLast, pushCount, storyPushCount));
    }

    public static void onShowOrUpdateNotificationSkipped(int currentAccount, String reason, int pushCount, int storyPushCount) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s showOrUpdateNotification:skip account=%d reason=%s push=%d storyPush=%d", PREFIX, currentAccount, reason, pushCount, storyPushCount));
    }

    public static void onShowOrUpdateNotificationResolved(int currentAccount, long dialogId, int messageId, boolean isStory, long maxDate) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s showOrUpdateNotification:resolved account=%d dialogId=%d messageId=%d isStory=%s maxDate=%d", PREFIX, currentAccount, dialogId, messageId, isStory, maxDate));
    }

    public static void onSummaryNotify(int currentAccount, int notificationId, long dialogId, long topicId, int pushCount, int storyPushCount) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s notify:summary account=%d notificationId=%d dialogId=%d topicId=%d push=%d storyPush=%d", PREFIX, currentAccount, notificationId, dialogId, topicId, pushCount, storyPushCount));
    }

    public static void onDialogNotify(int currentAccount, int notificationId, long dialogId, long topicId, boolean story) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s notify:dialog account=%d notificationId=%d dialogId=%d topicId=%d story=%s", PREFIX, currentAccount, notificationId, dialogId, topicId, story));
    }
}
