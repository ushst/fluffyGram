package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.ChatVideoVolumeButtonsPatch;

public final class ChatVideoVolumeButtonsHook {

    private ChatVideoVolumeButtonsHook() {
    }

    public static boolean isEnabled() {
        return ChatVideoVolumeButtonsPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        ChatVideoVolumeButtonsPatch.setEnabled(enabled);
    }

    public static boolean shouldHandleVolumeButtonsForChatVideo() {
        return ChatVideoVolumeButtonsPatch.shouldHandleVolumeButtonsForChatVideo();
    }
}
