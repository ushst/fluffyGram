package org.ushastoe.fluffy.patches;

import org.telegram.messenger.R;

public final class NotificationIconPatch {

    private NotificationIconPatch() {
    }

    public static int getNotificationSmallIcon() {
        return AppearanceSettingsPatch.useFluffyNotificationIcon()
                ? R.drawable.fluffy_notification
                : R.drawable.notification;
    }
}
