package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.ForwardCommentOrderPatch;

public final class ForwardCommentOrderHook {

    private ForwardCommentOrderHook() {
    }

    public static boolean isEnabled() {
        return ForwardCommentOrderPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ForwardCommentOrderPatch.setEnabled(enabled);
    }

    public static void beforeMessageSend(ChatActivity chatActivity, CharSequence message, boolean notify, int scheduleDate, int scheduleRepeatPeriod, long payStars) {
        ForwardCommentOrderPatch.beforeMessageSend(chatActivity, message, notify, scheduleDate, scheduleRepeatPeriod, payStars);
    }
}
