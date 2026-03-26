package org.ushastoe.fluffy.patches;

import android.text.TextUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.EditTextCaption;

import java.util.WeakHashMap;

public final class ChatPersistentAttachButtonPatch {
    private static final WeakHashMap<ChatActivityEnterView, ViewTreeObserver.OnPreDrawListener> listeners = new WeakHashMap<>();

    private ChatPersistentAttachButtonPatch() {
    }

    public static void attach(ChatActivityEnterView enterView) {
        if (enterView == null || listeners.containsKey(enterView)) {
            return;
        }
        ViewTreeObserver.OnPreDrawListener listener = () -> {
            syncAttachButton(enterView);
            return true;
        };
        enterView.getViewTreeObserver().addOnPreDrawListener(listener);
        listeners.put(enterView, listener);
    }

    private static void syncAttachButton(ChatActivityEnterView enterView) {
        ImageView attachButton = enterView.getAttachButton();
        View sendButton = enterView.getSendButtonInternal();
        if (attachButton == null || sendButton == null) {
            return;
        }
        boolean keepVisible = sendButton.getVisibility() == View.VISIBLE
                && enterView.hasText()
                && TextUtils.isEmpty(enterView.getSlowModeTimer());
        if (!keepVisible) {
            return;
        }
        attachButton.animate().cancel();
        attachButton.setVisibility(View.VISIBLE);
        attachButton.setAlpha(1.0f);
        attachButton.setScaleX(1.0f);
        attachButton.setScaleY(1.0f);
        EditTextCaption editField = enterView.getEditField();
        if (editField != null) {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) editField.getLayoutParams();
            int minRightMargin = AndroidUtilities.dp(50);
            if (layoutParams.rightMargin < minRightMargin) {
                layoutParams.rightMargin = minRightMargin;
                editField.setLayoutParams(layoutParams);
            }
        }
    }
}
