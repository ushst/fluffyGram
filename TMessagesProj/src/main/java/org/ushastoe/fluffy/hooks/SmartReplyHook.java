package org.ushastoe.fluffy.hooks;

import android.view.ViewGroup;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.ushastoe.fluffy.patches.SmartReplyPredictorPatch;

import java.util.ArrayList;

public final class SmartReplyHook {

    private SmartReplyHook() {
    }

    public static void attach(ChatActivity activity, ViewGroup contentView, BlurredBackgroundDrawableViewFactory glassFactory) {
        SmartReplyPredictorPatch.attach(activity, contentView, glassFactory);
    }

    public static void detach(ChatActivity activity) {
        SmartReplyPredictorPatch.detach(activity);
    }

    public static void updatePosition(ChatActivity activity, float animatedMaxBottomInset) {
        SmartReplyPredictorPatch.updatePosition(activity, animatedMaxBottomInset);
    }

    public static void onNewMessages(ChatActivity activity, ArrayList<MessageObject> messages) {
        SmartReplyPredictorPatch.onNewMessages(activity, messages);
    }

    public static void onFieldPanelChanged(ChatActivity activity) {
        SmartReplyPredictorPatch.onFieldPanelChanged(activity);
    }

    public static void onChatReady(ChatActivity activity) {
        SmartReplyPredictorPatch.refreshFromLastIncoming(activity);
    }

    public static void hide(ChatActivity activity) {
        SmartReplyPredictorPatch.hide(activity);
    }

    public static void onPause(ChatActivity activity) {
        SmartReplyPredictorPatch.onPause(activity);
    }

    public static void onResume(ChatActivity activity) {
        SmartReplyPredictorPatch.onResume(activity);
    }

    public static void onSettingChanged() {
        SmartReplyPredictorPatch.onSettingChanged();
    }

    public static float getExtraChatListBottomPadding(ChatActivity activity) {
        return SmartReplyPredictorPatch.getExtraChatListBottomPadding(activity);
    }
}
