package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.ForwardedOriginalReactionsPatch;

import java.util.ArrayList;

public final class ForwardedOriginalReactionsHook {

    private ForwardedOriginalReactionsHook() {
    }

    public static boolean isEnabled() {
        return ForwardedOriginalReactionsPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ForwardedOriginalReactionsPatch.setEnabled(enabled);
    }

    public static void maybeLoadForVisibleMessages(ChatActivity chatActivity, ArrayList<MessageObject> visibleObjects) {
        ForwardedOriginalReactionsPatch.maybeLoadForVisibleMessages(chatActivity, visibleObjects);
    }
}
