package org.ushastoe.fluffy.hooks;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.ushastoe.fluffy.patches.NotificationLaunchIntentPatch;

public final class NotificationLaunchIntentHook {

    private NotificationLaunchIntentHook() {
    }

    public static PendingIntent createContentIntent(Context context, Intent intent, int requestCode) {
        return NotificationLaunchIntentPatch.createContentIntent(context, intent, requestCode);
    }

    public static void applyTopicExtra(Intent intent, long topicId) {
        NotificationLaunchIntentPatch.applyTopicExtra(intent, topicId);
    }
}
