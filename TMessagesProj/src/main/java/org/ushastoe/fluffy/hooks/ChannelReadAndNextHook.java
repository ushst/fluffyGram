package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.chat.layouts.ChatActivitySideControlsButtonsLayout;
import org.ushastoe.fluffy.patches.ChannelReadAndNextPatch;

public final class ChannelReadAndNextHook {
    private ChannelReadAndNextHook() {
    }

    public static boolean isEnabled() {
        return ChannelReadAndNextPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ChannelReadAndNextPatch.setEnabled(enabled);
    }

    public static void attach(ChatActivity chatActivity, ChatActivitySideControlsButtonsLayout layout) {
        ChannelReadAndNextPatch.attach(chatActivity, layout);
    }

    public static void onClick(ChatActivity chatActivity) {
        ChannelReadAndNextPatch.onClick(chatActivity);
    }
}
