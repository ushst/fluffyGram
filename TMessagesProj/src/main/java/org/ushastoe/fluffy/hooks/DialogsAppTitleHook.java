package org.ushastoe.fluffy.hooks;

import android.graphics.drawable.Drawable;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Stories.DialogStoriesCell;
import org.ushastoe.fluffy.patches.DialogsAppTitlePatch;

public final class DialogsAppTitleHook {

    private DialogsAppTitleHook() {
    }

    public static void applyDialogsActionBarTitle(ActionBar actionBar, int currentAccount, Drawable rightDrawable) {
        DialogsAppTitlePatch.applyDialogsActionBarTitle(actionBar, currentAccount, rightDrawable);
    }

    public static CharSequence getDialogsStoryTitle(CharSequence currentTitle, int currentAccount) {
        return DialogsAppTitlePatch.getDialogsStoryTitle(currentTitle, currentAccount);
    }

    public static void refreshDialogsStoryStatus(DialogStoriesCell dialogStoriesCell, int currentAccount) {
        DialogsAppTitlePatch.refreshDialogsStoryStatus(dialogStoriesCell, currentAccount);
    }

    public static void refreshDialogsActionBarTitleLayout(ActionBar actionBar) {
        DialogsAppTitlePatch.refreshDialogsActionBarTitleLayout(actionBar);
    }

    public static void onDialogsEmojiLoaded(ActionBar actionBar) {
        DialogsAppTitlePatch.onDialogsEmojiLoaded(actionBar);
    }

    public static void onActionBarTitleUpdated(ActionBar actionBar) {
        DialogsAppTitlePatch.onActionBarTitleUpdated(actionBar);
    }

    public static void onActionBarResume(ActionBar actionBar) {
        DialogsAppTitlePatch.onActionBarResume(actionBar);
    }

    public static void onActionBarPause(ActionBar actionBar) {
        DialogsAppTitlePatch.onActionBarPause(actionBar);
    }

    public static void onActionBarAttached(ActionBar actionBar) {
        DialogsAppTitlePatch.onActionBarAttached(actionBar);
    }

    public static void onActionBarDetached(ActionBar actionBar) {
        DialogsAppTitlePatch.onActionBarDetached(actionBar);
    }

    public static void onDialogsStatusUpdated(ActionBar actionBar, int currentAccount, Drawable statusDrawable) {
        DialogsAppTitlePatch.onDialogsStatusUpdated(actionBar, currentAccount, statusDrawable);
    }

    public static boolean shouldShowDialogsStoryStatusWithTitle(CharSequence currentTitle) {
        return DialogsAppTitlePatch.shouldShowDialogsStoryStatusWithTitle(currentTitle);
    }

    public static CharSequence getDialogsAppTitle(int currentAccount) {
        return DialogsAppTitlePatch.getDialogsAppTitle(currentAccount);
    }
}
