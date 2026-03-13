package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ChatActivity;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.ushastoe.fluffy.patches.ChatHeaderCenteringPatch;

public final class ChatHeaderCenteringHook {

    private ChatHeaderCenteringHook() {
    }

    public static void applyMode(ChatAvatarContainer container) {
        ChatHeaderCenteringPatch.applyMode(container);
    }

    public static void onTitleChanged(ChatAvatarContainer container) {
        ChatHeaderCenteringPatch.onTitleChanged(container);
    }

    public static void applyFadeCopyMode(ChatAvatarContainer container, SimpleTextView titleCopy, SimpleTextView subtitleCopy) {
        ChatHeaderCenteringPatch.applyFadeCopyMode(container, titleCopy, subtitleCopy);
    }

    public static boolean onMeasure(ChatAvatarContainer container, int widthMeasureSpec, int heightMeasureSpec) {
        return ChatHeaderCenteringPatch.onMeasure(container, widthMeasureSpec, heightMeasureSpec);
    }

    public static boolean onLayout(ChatAvatarContainer container, boolean changed, int left, int top, int right, int bottom) {
        return ChatHeaderCenteringPatch.onLayout(container, changed, left, top, right, bottom);
    }

    public static int getAvatarContainerRightMargin(ChatActivity chatActivity, int defaultMargin) {
        return ChatHeaderCenteringPatch.getAvatarContainerRightMargin(chatActivity, defaultMargin);
    }

    public static boolean resolveShowAudioCallAsIcon(ChatActivity chatActivity, boolean defaultValue) {
        return ChatHeaderCenteringPatch.resolveShowAudioCallAsIcon(chatActivity, defaultValue);
    }

    public static void onHeaderItemCreated(ChatActivity chatActivity) {
        ChatHeaderCenteringPatch.onHeaderItemCreated(chatActivity);
    }
}
