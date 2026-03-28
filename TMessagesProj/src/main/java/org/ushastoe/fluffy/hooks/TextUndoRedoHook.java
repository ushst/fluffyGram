package org.ushastoe.fluffy.hooks;

import android.view.Menu;

import org.telegram.ui.Components.EditTextBoldCursor;
import org.ushastoe.fluffy.patches.TextUndoRedoPatch;

public final class TextUndoRedoHook {

    private TextUndoRedoHook() {
    }

    public static boolean isEnabled() {
        return TextUndoRedoPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        TextUndoRedoPatch.setEnabled(enabled);
    }

    public static void extendActionMode(EditTextBoldCursor editText, Menu menu) {
        TextUndoRedoPatch.extendActionMode(editText, menu);
    }
}
