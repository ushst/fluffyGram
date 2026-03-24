package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.patches.ShareFoldersPatch;

import java.util.ArrayList;
import java.util.List;

public final class ShareFoldersHook {

    private ShareFoldersHook() {
    }

    public static ArrayList<MessagesController.DialogFilter> getFolders(int currentAccount) {
        return ShareFoldersPatch.getFolders(currentAccount);
    }

    public static boolean hasFolders(int currentAccount) {
        return ShareFoldersPatch.hasFolders(currentAccount);
    }

    public static int getSearchContainerPadding(int currentAccount) {
        return ShareFoldersPatch.getSearchContainerPadding(currentAccount);
    }

    public static int getFoldersPadding(int currentAccount) {
        return ShareFoldersPatch.getFoldersPadding(currentAccount);
    }

    public static int getSearchTopMargin(boolean hasTopSwitch) {
        return ShareFoldersPatch.getSearchTopMargin(hasTopSwitch);
    }

    public static int getFoldersTopMargin(int currentAccount, boolean hasTopSwitch) {
        return ShareFoldersPatch.getFoldersTopMargin(currentAccount, hasTopSwitch);
    }

    public static List<TLRPC.Dialog> filterDialogs(int currentAccount, List<TLRPC.Dialog> source, int tabId, int defaultTabId) {
        return ShareFoldersPatch.filterDialogs(currentAccount, source, tabId, defaultTabId);
    }
}
