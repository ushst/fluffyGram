package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessagesController;
import org.telegram.ui.DialogsActivity;
import org.ushastoe.fluffy.patches.DialogFilterSelectionPatch;

import java.util.ArrayList;

public final class DialogFilterSelectionHook {
    private DialogFilterSelectionHook() {
    }

    public static int resolveSelectedType(DialogsActivity target, int currentSelectedType, ArrayList<MessagesController.DialogFilter> filters) {
        return DialogFilterSelectionPatch.resolveSelectedType(target, currentSelectedType, filters);
    }

    public static void onSelectedFilterChanged(DialogsActivity target, MessagesController.DialogFilter filter) {
        DialogFilterSelectionPatch.onSelectedFilterChanged(target, filter);
    }

    public static long getSavedFilterId(DialogsActivity target) {
        return DialogFilterSelectionPatch.getSavedFilterId(target);
    }
}
