package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.SilentMessageIndicatorPatch;

public final class SilentMessageIndicatorHook {

    private SilentMessageIndicatorHook() {
    }

    public static CharSequence buildSilentTimeLabel(MessageObject messageObject) {
        return SilentMessageIndicatorPatch.buildSilentTimeLabel(messageObject);
    }

    public static String getSilentPrefix() {
        return SilentMessageIndicatorPatch.getSilentPrefix();
    }
}
