package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;
import android.widget.TextView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.EditTextBoldCursor;

import java.lang.reflect.Method;

public final class TextUndoRedoPatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "text_undo_redo_enabled";
    private static final int UNDO_GROUP_ID = R.id.menu_undo_redo;

    private static Method canUndoMethod;
    private static Method canRedoMethod;
    private static boolean reflectionInitialized;

    private TextUndoRedoPatch() {
    }

    public static boolean isEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void extendActionMode(EditTextBoldCursor editText, Menu menu) {
        if (!isEnabled() || editText == null || menu == null) {
            return;
        }
        ensureReflectionInitialized();
        if (canUndoMethod == null || canRedoMethod == null) {
            return;
        }
        menu.removeGroup(UNDO_GROUP_ID);
        if (canUndo(editText)) {
            menu.add(UNDO_GROUP_ID, android.R.id.undo, 2, LocaleController.getString(R.string.FluffyTextUndo));
        }
        if (canRedo(editText)) {
            menu.add(UNDO_GROUP_ID, android.R.id.redo, 3, LocaleController.getString(R.string.FluffyTextRedo));
        }
    }

    private static boolean canUndo(EditTextBoldCursor editText) {
        try {
            return (boolean) canUndoMethod.invoke(editText);
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private static boolean canRedo(EditTextBoldCursor editText) {
        try {
            return (boolean) canRedoMethod.invoke(editText);
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private static void ensureReflectionInitialized() {
        if (reflectionInitialized) {
            return;
        }
        reflectionInitialized = true;
        try {
            canUndoMethod = TextView.class.getDeclaredMethod("canUndo");
            canRedoMethod = TextView.class.getDeclaredMethod("canRedo");
            canUndoMethod.setAccessible(true);
            canRedoMethod.setAccessible(true);
        } catch (Exception e) {
            canUndoMethod = null;
            canRedoMethod = null;
            FileLog.e(e);
        }
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
