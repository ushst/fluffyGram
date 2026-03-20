package org.ushastoe.fluffy.patches;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Components.FilterTabsView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.Stories.DialogStoriesCell;

public final class DialogsFolderTitlePatch {
    private DialogsFolderTitlePatch() {
    }

    public static void syncDialogsTitle(DialogsActivity dialogsActivity, ActionBar actionBar,
                                        DialogStoriesCell dialogStoriesCell, int currentAccount,
                                        Drawable statusDrawable) {
        applyResolvedTitle(actionBar, dialogStoriesCell, currentAccount, statusDrawable, resolveFolderTitle(dialogsActivity));
    }

    public static void onTabPressed(DialogsActivity dialogsActivity, ActionBar actionBar,
                                    DialogStoriesCell dialogStoriesCell, FilterTabsView.Tab tab,
                                    int currentAccount, Drawable statusDrawable) {
        applyResolvedTitle(actionBar, dialogStoriesCell, currentAccount, statusDrawable, resolveFolderTitle(tab));
    }

    public static void onPageSettled(DialogsActivity dialogsActivity, ActionBar actionBar,
                                     DialogStoriesCell dialogStoriesCell, int currentAccount,
                                     Drawable statusDrawable) {
        syncDialogsTitle(dialogsActivity, actionBar, dialogStoriesCell, currentAccount, statusDrawable);
    }

    public static void onTabSelected(ActionBar actionBar, DialogStoriesCell dialogStoriesCell,
                                     FilterTabsView.Tab tab, int currentAccount, Drawable statusDrawable) {
        CharSequence folderTitle = resolveFolderTitle(tab);
        applyResolvedTitle(actionBar, dialogStoriesCell, currentAccount, statusDrawable, folderTitle);
    }

    private static void applyResolvedTitle(ActionBar actionBar, DialogStoriesCell dialogStoriesCell,
                                           int currentAccount, Drawable statusDrawable,
                                           CharSequence folderTitle) {
        if (TextUtils.isEmpty(folderTitle)) {
            DialogsAppTitlePatch.applyDialogsActionBarTitle(actionBar, currentAccount, statusDrawable);
            if (dialogStoriesCell != null) {
                dialogStoriesCell.setTitleOverlayText((CharSequence) null);
            }
            return;
        }
        if (actionBar != null) {
            actionBar.setTitle(folderTitle, null);
        }
        if (dialogStoriesCell != null) {
            dialogStoriesCell.setTitleOverlayText(folderTitle);
        }
    }

    private static CharSequence resolveFolderTitle(DialogsActivity dialogsActivity) {
        if (dialogsActivity == null) {
            return null;
        }
        return resolveFolderTitle(dialogsActivity.getCurrentFluffySelectedTab());
    }

    private static CharSequence resolveFolderTitle(FilterTabsView.Tab tab) {
        if (AppearanceSettingsPatch.getDialogsAppTitleMode() != AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_FOLDER
                || tab == null || tab.isDefault) {
            return null;
        }
        return TextUtils.isEmpty(tab.title) ? null : tab.title;
    }
}
