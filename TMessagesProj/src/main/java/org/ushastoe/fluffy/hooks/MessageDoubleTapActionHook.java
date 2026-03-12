package org.ushastoe.fluffy.hooks;

import android.view.View;

import androidx.annotation.Nullable;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.patches.MessageDoubleTapActionPatch;

public final class MessageDoubleTapActionHook {

    private MessageDoubleTapActionHook() {
    }

    @Nullable
    public static Boolean hasDoubleTapOverride(ChatActivity chatActivity, View view, int position) {
        return MessageDoubleTapActionPatch.hasDoubleTapOverride(chatActivity, view);
    }

    public static boolean onDoubleTap(ChatActivity chatActivity, View view, int position, float x, float y) {
        return MessageDoubleTapActionPatch.onDoubleTap(chatActivity, view);
    }

    public static void onStartEditingMessageObject(ChatActivity chatActivity, MessageObject messageObject, RecyclerListView chatListView, int blurredViewBottomOffset, ChatActivityEnterView chatActivityEnterView) {
        MessageDoubleTapActionPatch.onStartEditingMessageObject(chatActivity, messageObject, chatListView, blurredViewBottomOffset, chatActivityEnterView);
    }
}
