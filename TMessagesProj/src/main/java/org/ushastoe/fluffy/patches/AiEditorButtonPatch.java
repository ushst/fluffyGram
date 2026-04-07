package org.ushastoe.fluffy.patches;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

public final class AiEditorButtonPatch {
    private static final int DEFAULT_ENTER_VIEW_GRAVITY = Gravity.TOP | Gravity.RIGHT;

    private AiEditorButtonPatch() {
    }

    public static boolean useActionModeEntry() {
        return AppearanceSettingsPatch.isChatAiButtonShortTextEnabled();
    }

    public static boolean shouldShowButton(boolean aiEditorAvailable, int lineCount, CharSequence text) {
        if (useActionModeEntry()) {
            return false;
        }
        return shouldShowAuxiliaryButton(aiEditorAvailable, lineCount, text);
    }

    public static boolean shouldShowAuxiliaryButton(boolean aiEditorAvailable, int lineCount, CharSequence text) {
        if (!aiEditorAvailable || TextUtils.isEmpty(text) || TextUtils.isEmpty(text.toString().trim())) {
            return false;
        }
        return useActionModeEntry() || lineCount > 2;
    }

    public static void applyEnterViewLayout(View aiButton) {
        if (aiButton == null) {
            return;
        }
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) aiButton.getLayoutParams();
        if (layoutParams == null) {
            return;
        }
        if (layoutParams.gravity == DEFAULT_ENTER_VIEW_GRAVITY
                && layoutParams.leftMargin == 0
                && layoutParams.topMargin == dp(1)
                && layoutParams.rightMargin == 0
                && layoutParams.bottomMargin == 0) {
            return;
        }
        layoutParams.gravity = DEFAULT_ENTER_VIEW_GRAVITY;
        layoutParams.leftMargin = 0;
        layoutParams.topMargin = dp(1);
        layoutParams.rightMargin = 0;
        layoutParams.bottomMargin = 0;
        aiButton.setLayoutParams(layoutParams);
    }

    public static int adjustEnterViewTextRightMargin(int rightMargin) {
        return rightMargin;
    }

    public static boolean shouldShowCompactEnterViewHint() {
        return !useActionModeEntry();
    }

    private static int dp(int value) {
        return org.telegram.messenger.AndroidUtilities.dp(value);
    }
}
