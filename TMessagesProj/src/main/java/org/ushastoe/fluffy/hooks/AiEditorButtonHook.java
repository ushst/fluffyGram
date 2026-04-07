package org.ushastoe.fluffy.hooks;

import android.view.View;

import org.ushastoe.fluffy.patches.AiEditorButtonPatch;

public final class AiEditorButtonHook {
    private AiEditorButtonHook() {
    }

    public static boolean shouldShowButton(boolean aiEditorAvailable, int lineCount, CharSequence text) {
        return AiEditorButtonPatch.shouldShowButton(aiEditorAvailable, lineCount, text);
    }

    public static boolean shouldShowAuxiliaryButton(boolean aiEditorAvailable, int lineCount, CharSequence text) {
        return AiEditorButtonPatch.shouldShowAuxiliaryButton(aiEditorAvailable, lineCount, text);
    }

    public static boolean useActionModeEntry() {
        return AiEditorButtonPatch.useActionModeEntry();
    }

    public static void applyEnterViewLayout(View aiButton) {
        AiEditorButtonPatch.applyEnterViewLayout(aiButton);
    }

    public static int adjustEnterViewTextRightMargin(int rightMargin) {
        return AiEditorButtonPatch.adjustEnterViewTextRightMargin(rightMargin);
    }

    public static boolean shouldShowEnterViewHint() {
        return AiEditorButtonPatch.shouldShowCompactEnterViewHint();
    }
}
