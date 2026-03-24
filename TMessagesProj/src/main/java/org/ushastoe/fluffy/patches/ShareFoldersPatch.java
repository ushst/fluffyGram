package org.ushastoe.fluffy.patches;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.List;

public final class ShareFoldersPatch {

    private static final int SEARCH_CONTAINER_PADDING = 42;
    private static final int FOLDERS_PADDING = 50;
    private static final int SEARCH_TOP_MARGIN = 7;
    private static final int SEARCH_TOP_MARGIN_WITH_SWITCH = 47;
    private static final int FOLDERS_TOP_MARGIN = 47;
    private static final int FOLDERS_TOP_MARGIN_WITH_SWITCH = 87;

    private ShareFoldersPatch() {
    }

    public static ArrayList<MessagesController.DialogFilter> getFolders(int currentAccount) {
        return MessagesController.getInstance(currentAccount).getDialogFilters();
    }

    public static boolean hasFolders(int currentAccount) {
        return getFolders(currentAccount).size() > 1;
    }

    public static int getSearchContainerPadding(int currentAccount) {
        return hasFolders(currentAccount) ? SEARCH_CONTAINER_PADDING : 0;
    }

    public static int getFoldersPadding(int currentAccount) {
        return hasFolders(currentAccount) ? FOLDERS_PADDING : 0;
    }

    public static int getSearchTopMargin(boolean hasTopSwitch) {
        return hasTopSwitch ? SEARCH_TOP_MARGIN_WITH_SWITCH : SEARCH_TOP_MARGIN;
    }

    public static int getFoldersTopMargin(int currentAccount, boolean hasTopSwitch) {
        if (!hasFolders(currentAccount)) {
            return 0;
        }
        return hasTopSwitch ? FOLDERS_TOP_MARGIN_WITH_SWITCH : FOLDERS_TOP_MARGIN;
    }

    public static List<TLRPC.Dialog> filterDialogs(int currentAccount, List<TLRPC.Dialog> source, int tabId, int defaultTabId) {
        ArrayList<TLRPC.Dialog> filtered = new ArrayList<>();
        if (source == null) {
            return filtered;
        }

        MessagesController.DialogFilter filter = null;
        if (tabId != defaultTabId) {
            ArrayList<MessagesController.DialogFilter> filters = getFolders(currentAccount);
            if (tabId >= 0 && tabId < filters.size()) {
                filter = filters.get(tabId);
            }
        }

        if (filter == null) {
            filtered.addAll(source);
            return filtered;
        }

        AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
        for (TLRPC.Dialog dialog : source) {
            if (dialog != null && filter.includesDialog(accountInstance, dialog.id)) {
                filtered.add(dialog);
            }
        }
        return filtered;
    }
}
