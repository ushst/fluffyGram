package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.patches.QuickShareMediaPatch;

public final class QuickShareMediaHook {

    private QuickShareMediaHook() {
    }

    public static boolean isEnabled() {
        return QuickShareMediaPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        QuickShareMediaPatch.setEnabled(enabled);
    }

    public static boolean shouldDrawForPrivateChat(MessageObject messageObject, TLRPC.User user) {
        return QuickShareMediaPatch.shouldDrawForPrivateChat(messageObject, user);
    }
}
