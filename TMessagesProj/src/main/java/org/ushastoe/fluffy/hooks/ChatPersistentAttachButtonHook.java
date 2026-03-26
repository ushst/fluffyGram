package org.ushastoe.fluffy.hooks;

import org.telegram.ui.Components.ChatActivityEnterView;
import org.ushastoe.fluffy.patches.ChatPersistentAttachButtonPatch;

public final class ChatPersistentAttachButtonHook {
    private ChatPersistentAttachButtonHook() {
    }

    public static void attach(ChatActivityEnterView enterView) {
        ChatPersistentAttachButtonPatch.attach(enterView);
    }
}
