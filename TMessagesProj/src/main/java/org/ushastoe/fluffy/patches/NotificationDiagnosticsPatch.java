package org.ushastoe.fluffy.patches;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NotificationDiagnosticsPatch {

    private static final String PREFIX = "fluffy_notification_diag";
    private static final Map<String, Long> wakeLockAcquiredAt = new ConcurrentHashMap<>();
    private static final Map<String, Long> taskStartedAt = new ConcurrentHashMap<>();

    private NotificationDiagnosticsPatch() {
    }

    private static boolean isScreenOn() {
        return ApplicationLoader.isScreenOn;
    }

    public static void onProcessNewMessages(int currentAccount, ArrayList<MessageObject> messageObjects, boolean isLast, boolean isFcm, long startMs) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        int count = messageObjects == null ? 0 : messageObjects.size();
        if (count <= 0) {
            FileLog.d(String.format(Locale.US, "%s processNewMessages:start account=%d count=%d isLast=%s isFcm=%s screenOn=%s startMs=%d", PREFIX, currentAccount, count, isLast, isFcm, isScreenOn(), startMs));
            return;
        }
        MessageObject first = messageObjects.get(0);
        long dialogId = first != null ? first.getDialogId() : 0L;
        int messageId = first != null ? first.getId() : 0;
        FileLog.d(String.format(Locale.US, "%s processNewMessages:start account=%d count=%d isLast=%s isFcm=%s screenOn=%s firstDialogId=%d firstMessageId=%d startMs=%d", PREFIX, currentAccount, count, isLast, isFcm, isScreenOn(), dialogId, messageId, startMs));
    }

    public static void onProcessNewMessagesResolved(int currentAccount, boolean isFcm, int processedCount, long startMs) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long durationMs = System.currentTimeMillis() - startMs;
        FileLog.d(String.format(Locale.US, "%s processNewMessages:resolved account=%d isFcm=%s processedCount=%d screenOn=%s durationMs=%d", PREFIX, currentAccount, isFcm, processedCount, isScreenOn(), durationMs));
    }

    public static void onShowOrUpdateNotificationStart(int currentAccount, boolean notifyAboutLast, int pushCount, int storyPushCount, long startMs) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s showOrUpdateNotification:start account=%d notifyAboutLast=%s push=%d storyPush=%d screenOn=%s startMs=%d", PREFIX, currentAccount, notifyAboutLast, pushCount, storyPushCount, isScreenOn(), startMs));
    }

    public static void onShowOrUpdateNotificationSkipped(int currentAccount, String reason, int pushCount, int storyPushCount, long startMs) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long durationMs = System.currentTimeMillis() - startMs;
        FileLog.d(String.format(Locale.US, "%s showOrUpdateNotification:skip account=%d reason=%s push=%d storyPush=%d screenOn=%s durationMs=%d", PREFIX, currentAccount, reason, pushCount, storyPushCount, isScreenOn(), durationMs));
    }

    public static void onShowOrUpdateNotificationResolved(int currentAccount, long dialogId, int messageId, boolean isStory, long maxDate, long startMs) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long durationMs = System.currentTimeMillis() - startMs;
        FileLog.d(String.format(Locale.US, "%s showOrUpdateNotification:resolved account=%d dialogId=%d messageId=%d isStory=%s maxDate=%d screenOn=%s durationMs=%d", PREFIX, currentAccount, dialogId, messageId, isStory, maxDate, isScreenOn(), durationMs));
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

    public static void onWakeLockAcquire(String tag, String caller, long timeoutMs) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        wakeLockAcquiredAt.put(tag, now);
        FileLog.d(String.format(Locale.US, "%s wakelock:acquire tag=%s caller=%s timeoutMs=%d screenOn=%s atMs=%d", PREFIX, tag, caller, timeoutMs, isScreenOn(), now));
    }

    public static void onWakeLockRelease(String tag, String caller) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long acquiredAt = wakeLockAcquiredAt.remove(tag);
        long heldMs = acquiredAt != null ? now - acquiredAt : -1L;
        FileLog.d(String.format(Locale.US, "%s wakelock:release tag=%s caller=%s heldMs=%d screenOn=%s atMs=%d", PREFIX, tag, caller, heldMs, isScreenOn(), now));
    }

    public static void onSyncNetworkCall(String manager, String action, String detail) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s sync:call manager=%s action=%s detail=%s screenOn=%s atMs=%d", PREFIX, manager, action, detail, isScreenOn(), System.currentTimeMillis()));
    }

    public static void onBackgroundTaskStart(String task, String detail) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        taskStartedAt.put(task, now);
        FileLog.d(String.format(Locale.US, "%s bgtask:start task=%s detail=%s screenOn=%s atMs=%d", PREFIX, task, detail, isScreenOn(), now));
    }

    public static void onBackgroundTaskEnd(String task, String detail) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long startedAt = taskStartedAt.remove(task);
        long durationMs = startedAt != null ? now - startedAt : -1L;
        FileLog.d(String.format(Locale.US, "%s bgtask:end task=%s detail=%s screenOn=%s durationMs=%d atMs=%d", PREFIX, task, detail, isScreenOn(), durationMs, now));
    }

    public static void onAlarmScheduled(String source, long triggerAtMs, String type) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        FileLog.d(String.format(Locale.US, "%s alarm:scheduled source=%s type=%s triggerInMs=%d screenOn=%s atMs=%d", PREFIX, source, type, triggerAtMs - now, isScreenOn(), now));
    }

    public static void onAlarmCancelled(String source) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s alarm:cancelled source=%s screenOn=%s atMs=%d", PREFIX, source, isScreenOn(), System.currentTimeMillis()));
    }

    public static void onBotSensorsAutoPause(long botId) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s botsensors:auto_pause botId=%d reason=app_backgrounded screenOn=%s atMs=%d", PREFIX, botId, isScreenOn(), System.currentTimeMillis()));
    }

    public static void onBotSensorsAutoResume(long botId) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(String.format(Locale.US, "%s botsensors:auto_resume botId=%d screenOn=%s atMs=%d", PREFIX, botId, isScreenOn(), System.currentTimeMillis()));
    }
}
