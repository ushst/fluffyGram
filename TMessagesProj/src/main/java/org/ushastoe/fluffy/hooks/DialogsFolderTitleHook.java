package org.ushastoe.fluffy.hooks;

import android.graphics.drawable.Drawable;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Components.FilterTabsView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.Stories.DialogStoriesCell;
import org.ushastoe.fluffy.patches.DialogsFolderTitlePatch;

public final class DialogsFolderTitleHook {

    private DialogsFolderTitleHook() {
    }

    public static void syncDialogsTitle(DialogsActivity dialogsActivity, ActionBar actionBar,
                                        DialogStoriesCell dialogStoriesCell, int currentAccount,
                                        Drawable statusDrawable) {
        DialogsFolderTitlePatch.syncDialogsTitle(dialogsActivity, actionBar, dialogStoriesCell, currentAccount, statusDrawable);
    }

    public static void onTabPressed(DialogsActivity dialogsActivity, ActionBar actionBar,
                                    DialogStoriesCell dialogStoriesCell, FilterTabsView.Tab tab,
                                    int currentAccount, Drawable statusDrawable) {
        DialogsFolderTitlePatch.onTabPressed(dialogsActivity, actionBar, dialogStoriesCell, tab, currentAccount, statusDrawable);
    }

    public static void onTabSelected(ActionBar actionBar, DialogStoriesCell dialogStoriesCell,
                                     FilterTabsView.Tab tab, int currentAccount, Drawable statusDrawable) {
        DialogsFolderTitlePatch.onTabSelected(actionBar, dialogStoriesCell, tab, currentAccount, statusDrawable);
    }

    public static void onPageSettled(DialogsActivity dialogsActivity, ActionBar actionBar,
                                     DialogStoriesCell dialogStoriesCell, int currentAccount,
                                     Drawable statusDrawable) {
        DialogsFolderTitlePatch.onPageSettled(dialogsActivity, actionBar, dialogStoriesCell, currentAccount, statusDrawable);
    }
}
