package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.NotificationDiagnosticsPatch;

import java.util.ArrayList;

public final class NotificationDiagnosticsHook {

    private NotificationDiagnosticsHook() {
    }

    public static void onProcessNewMessages(int currentAccount, ArrayList<MessageObject> messageObjects, boolean isLast, boolean isFcm, long startMs) {
        NotificationDiagnosticsPatch.onProcessNewMessages(currentAccount, messageObjects, isLast, isFcm, startMs);
    }

    public static void onProcessNewMessagesResolved(int currentAccount, boolean isFcm, int processedCount, long startMs) {
        NotificationDiagnosticsPatch.onProcessNewMessagesResolved(currentAccount, isFcm, processedCount, startMs);
    }

    public static void onShowOrUpdateNotificationStart(int currentAccount, boolean notifyAboutLast, int pushCount, int storyPushCount, long startMs) {
        NotificationDiagnosticsPatch.onShowOrUpdateNotificationStart(currentAccount, notifyAboutLast, pushCount, storyPushCount, startMs);
    }

    public static void onShowOrUpdateNotificationSkipped(int currentAccount, String reason, int pushCount, int storyPushCount, long startMs) {
        NotificationDiagnosticsPatch.onShowOrUpdateNotificationSkipped(currentAccount, reason, pushCount, storyPushCount, startMs);
    }

    public static void onShowOrUpdateNotificationResolved(int currentAccount, long dialogId, int messageId, boolean isStory, long maxDate, long startMs) {
        NotificationDiagnosticsPatch.onShowOrUpdateNotificationResolved(currentAccount, dialogId, messageId, isStory, maxDate, startMs);
    }

    public static void onSummaryNotify(int currentAccount, int notificationId, long dialogId, long topicId, int pushCount, int storyPushCount) {
        NotificationDiagnosticsPatch.onSummaryNotify(currentAccount, notificationId, dialogId, topicId, pushCount, storyPushCount);
    }

    public static void onDialogNotify(int currentAccount, int notificationId, long dialogId, long topicId, boolean story) {
        NotificationDiagnosticsPatch.onDialogNotify(currentAccount, notificationId, dialogId, topicId, story);
    }

    public static void onWakeLockAcquire(String tag, String caller, long timeoutMs) {
        NotificationDiagnosticsPatch.onWakeLockAcquire(tag, caller, timeoutMs);
    }

    public static void onWakeLockRelease(String tag, String caller) {
        NotificationDiagnosticsPatch.onWakeLockRelease(tag, caller);
    }

    public static void onBackgroundTaskStart(String task, String detail) {
        NotificationDiagnosticsPatch.onBackgroundTaskStart(task, detail);
    }

    public static void onBackgroundTaskEnd(String task, String detail) {
        NotificationDiagnosticsPatch.onBackgroundTaskEnd(task, detail);
    }

    public static void onAlarmScheduled(String source, long triggerAtMs, String type) {
        NotificationDiagnosticsPatch.onAlarmScheduled(source, triggerAtMs, type);
    }

    public static void onAlarmCancelled(String source) {
        NotificationDiagnosticsPatch.onAlarmCancelled(source);
    }

    public static void onBotSensorsAutoPause(long botId) {
        NotificationDiagnosticsPatch.onBotSensorsAutoPause(botId);
    }

    public static void onBotSensorsAutoResume(long botId) {
        NotificationDiagnosticsPatch.onBotSensorsAutoResume(botId);
    }
}
