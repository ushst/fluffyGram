package org.ushastoe.fluffy.updates;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BetaUpdate;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public final class FluffyUpdateNotificationHelper {

    private static final String CHANNEL_ID = "fluffy_update_notifications";
    private static final int NOTIFICATION_ID = 1016;

    private FluffyUpdateNotificationHelper() {
    }

    public static void showUpdateAvailable(BetaUpdate update, String pageUrl) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null || update == null) {
            return;
        }
        ensureNotificationChannel(context);

        PendingIntent pendingIntent = createContentIntent(context, pageUrl);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);
        builder.setSmallIcon(R.drawable.notification)
                .setContentTitle(LocaleController.formatString(R.string.FluffyUpdateNotificationTitle, update.version))
                .setContentText(LocaleController.getString(R.string.FluffyUpdateNotificationText))
                .setAutoCancel(true)
                .setShowWhen(true);
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }

        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        } catch (Throwable ignore) {
        }
    }

    public static void cancel() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            }
        } catch (Throwable ignore) {
        }
    }

    private static PendingIntent createContentIntent(Context context, String pageUrl) {
        Intent intent;
        if (!TextUtils.isEmpty(pageUrl)) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl));
        } else {
            intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent == null) {
                return null;
            }
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(context, NOTIFICATION_ID, intent, flags);
    }

    private static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager == null || notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                LocaleController.getString(R.string.FluffyUpdateNotificationChannel),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(channel);
    }
}
