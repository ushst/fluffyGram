package org.ushastoe.fluffy.patches;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class NotificationLaunchIntentPatch {

    private NotificationLaunchIntentPatch() {
    }

    public static PendingIntent createContentIntent(Context context, Intent intent, int requestCode) {
        return PendingIntent.getActivity(
                context,
                sanitizeRequestCode(intent, requestCode),
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    public static void applyTopicExtra(Intent intent, long topicId) {
        if (intent == null || topicId == 0) {
            return;
        }
        intent.putExtra("topicId", topicId);
    }

    private static int sanitizeRequestCode(Intent intent, int requestCode) {
        int resolved = requestCode;
        if (resolved == 0 && intent != null && intent.getAction() != null) {
            resolved = intent.getAction().hashCode();
        }
        if (resolved == Integer.MIN_VALUE) {
            resolved = 0;
        }
        return Math.abs(resolved);
    }
}
