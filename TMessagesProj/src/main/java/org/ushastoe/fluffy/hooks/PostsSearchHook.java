package org.ushastoe.fluffy.hooks;

import android.content.Context;

import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.UItem;
import org.ushastoe.fluffy.patches.PostsSearchHistoryPatch;
import org.ushastoe.fluffy.patches.PostsSearchRefreshPatch;

import java.util.ArrayList;

public final class PostsSearchHook {

    private PostsSearchHook() {
    }

    public static void saveQuery(int currentAccount, String query) {
        PostsSearchHistoryPatch.saveQuery(currentAccount, query);
    }

    public static void fillRecentQueries(int currentAccount, ArrayList<UItem> items) {
        PostsSearchHistoryPatch.fillItems(currentAccount, items);
    }

    public static boolean onRecentQueryClick(
        int currentAccount,
        UItem item,
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        Utilities.Callback<String> onQuerySelected,
        Runnable onHistoryChanged
    ) {
        return PostsSearchHistoryPatch.onItemClick(currentAccount, item, context, resourcesProvider, onQuerySelected, onHistoryChanged);
    }

    public static boolean onRecentQueryLongClick(
        int currentAccount,
        UItem item,
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        Runnable onHistoryChanged
    ) {
        return PostsSearchHistoryPatch.onItemLongClick(currentAccount, item, context, resourcesProvider, onHistoryChanged);
    }

    public static ActionBarMenuItem createRefreshItem(ActionBarMenu menu, Runnable onClick) {
        return PostsSearchRefreshPatch.createRefreshItem(menu, onClick);
    }
}
