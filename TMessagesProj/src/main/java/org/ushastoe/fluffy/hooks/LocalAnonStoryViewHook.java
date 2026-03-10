package org.ushastoe.fluffy.hooks;

import android.view.View;

import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Stories.DialogStoriesCell;
import org.ushastoe.fluffy.patches.LocalAnonStoryViewPatch;

public final class LocalAnonStoryViewHook {

    private LocalAnonStoryViewHook() {
    }

    public static void addDialogStoryOption(ItemOptions options, DialogStoriesCell dialogStoriesCell, View view, int currentAccount, long dialogId, boolean isUnread, boolean isLive) {
        LocalAnonStoryViewPatch.addDialogStoryOption(options, dialogStoriesCell, view, currentAccount, dialogId, isUnread, isLive);
    }

    public static void onStoryViewerOpen(int currentAccount, long dialogId) {
        LocalAnonStoryViewPatch.onStoryViewerOpen(currentAccount, dialogId);
    }

    public static void onStoryViewerClosed(int currentAccount) {
        LocalAnonStoryViewPatch.onStoryViewerClosed(currentAccount);
    }

    public static boolean shouldSendReadStoriesRequest(int currentAccount) {
        return LocalAnonStoryViewPatch.shouldSendReadStoriesRequest(currentAccount);
    }

    public static boolean shouldSendIncrementStoryViewsRequest(int currentAccount) {
        return LocalAnonStoryViewPatch.shouldSendIncrementStoryViewsRequest(currentAccount);
    }
}
