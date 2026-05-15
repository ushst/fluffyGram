package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.MessageTimeLabelPatch;

public final class MessageTimeLabelHook {
    private MessageTimeLabelHook() {
    }

    public static CharSequence buildSilentTimeLabel(MessageObject messageObject, boolean edited) {
        return MessageTimeLabelPatch.buildSilentTimeLabel(messageObject, edited);
    }

    public static CharSequence buildCustomTimeLabel(MessageObject messageObject, boolean edited) {
        return MessageTimeLabelPatch.buildCustomTimeLabel(messageObject, edited);
    }

    public static int getTimeWidthAdjustment(MessageObject messageObject) {
        return MessageTimeLabelPatch.getTimeWidthAdjustment(messageObject);
    }
}
