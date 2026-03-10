package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.NotificationIconPatch;

public final class NotificationIconHook {

    private NotificationIconHook() {
    }

    public static int getNotificationSmallIcon() {
        return NotificationIconPatch.getNotificationSmallIcon();
    }
}
