package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

public final class DialogFilterSelectionPatch {
    private static final String PREFS_NAME = "fluffy_dialog_filters";
    private static final String KEY_USER_PREFIX = "selected_filter_user_";
    private static final String KEY_ACCOUNT_PREFIX = "selected_filter_account_";

    private DialogFilterSelectionPatch() {
    }

    public static int resolveSelectedType(DialogsActivity target, int currentSelectedType, ArrayList<MessagesController.DialogFilter> filters) {
        if (target == null || filters == null || filters.isEmpty() || !target.isMainDialogList()) {
            return currentSelectedType;
        }

        MessagesController messagesController = target.getMessagesController();
        if (messagesController.selectedDialogFilter[0] != null || messagesController.selectedDialogFilter[1] != null) {
            return currentSelectedType;
        }

        long savedFilterId = getPreferences().getLong(getStorageKey(target), Long.MIN_VALUE);
        if (savedFilterId == Long.MIN_VALUE) {
            return currentSelectedType;
        }
        if (savedFilterId == 0L) {
            return 0;
        }

        for (int i = 0; i < filters.size(); i++) {
            MessagesController.DialogFilter filter = filters.get(i);
            if (filter != null && !filter.isDefault() && filter.id == savedFilterId) {
                return i;
            }
        }

        getPreferences().edit().remove(getStorageKey(target)).apply();
        return currentSelectedType;
    }

    public static void onSelectedFilterChanged(DialogsActivity target, MessagesController.DialogFilter filter) {
        if (target == null || !target.isMainDialogList()) {
            return;
        }

        long value = filter != null && !filter.isDefault() ? filter.id : 0L;
        getPreferences().edit().putLong(getStorageKey(target), value).apply();
    }

    public static long getSavedFilterId(DialogsActivity target) {
        if (target == null) {
            return Long.MIN_VALUE;
        }
        return getPreferences().getLong(getStorageKey(target), Long.MIN_VALUE);
    }

    private static SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String getStorageKey(DialogsActivity target) {
        long userId = target.getUserConfig().getClientUserId();
        if (userId != 0L) {
            return KEY_USER_PREFIX + userId;
        }
        return KEY_ACCOUNT_PREFIX + target.getCurrentAccount();
    }
}
