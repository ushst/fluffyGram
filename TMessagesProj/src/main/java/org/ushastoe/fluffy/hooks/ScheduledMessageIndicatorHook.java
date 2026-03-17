package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.ScheduledMessageIndicatorPatch;

public final class ScheduledMessageIndicatorHook {

    private ScheduledMessageIndicatorHook() {
    }

    public static CharSequence buildScheduledTimeLabel(MessageObject messageObject) {
        return ScheduledMessageIndicatorPatch.buildScheduledTimeLabel(messageObject);
    }

    public static String getScheduledPrefix() {
        return ScheduledMessageIndicatorPatch.getScheduledPrefix();
    }
}
