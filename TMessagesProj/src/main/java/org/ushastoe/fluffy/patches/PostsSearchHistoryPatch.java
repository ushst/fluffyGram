package org.ushastoe.fluffy.patches;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.SparseArray;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

/**
 * Recent queries of the "Posts" search tab: public post search has no local cache,
 * so the queries themselves are kept to let the user repeat a search in one tap.
 */
public final class PostsSearchHistoryPatch {

    public static final String QUERIES_KEY = "postssearch_recent";
    private static final String QUERIES_SEPARATOR = "\n";
    private static final int MAX_QUERIES = 10;

    private static final int CLEAR_ITEM_ID = 999;
    private static final int FIRST_QUERY_ITEM_ID = 1000;

    private static final SparseArray<ArrayList<String>> queries = new SparseArray<>();

    private PostsSearchHistoryPatch() {
    }

    private static SharedPreferences prefs(int currentAccount) {
        return MessagesController.getMainSettings(currentAccount);
    }

    public static ArrayList<String> getQueries(int currentAccount) {
        ArrayList<String> cached = queries.get(currentAccount);
        if (cached != null) {
            return cached;
        }
        cached = new ArrayList<>();
        final String value = prefs(currentAccount).getString(QUERIES_KEY, null);
        if (!TextUtils.isEmpty(value)) {
            for (String query : value.split(QUERIES_SEPARATOR)) {
                if (!TextUtils.isEmpty(query)) {
                    cached.add(query);
                }
            }
        }
        queries.put(currentAccount, cached);
        return cached;
    }

    private static void save(int currentAccount) {
        prefs(currentAccount).edit()
            .putString(QUERIES_KEY, TextUtils.join(QUERIES_SEPARATOR, getQueries(currentAccount)))
            .apply();
    }

    public static void saveQuery(int currentAccount, String query) {
        if (query == null) {
            return;
        }
        query = query.trim().replace(QUERIES_SEPARATOR, " ");
        if (TextUtils.isEmpty(query)) {
            return;
        }
        final ArrayList<String> history = getQueries(currentAccount);
        for (int i = 0; i < history.size(); ++i) {
            // queries typed on the way to this one ("kot" -> "kotik") are not worth keeping
            if (query.toLowerCase().startsWith(history.get(i).toLowerCase())) {
                history.remove(i--);
            }
        }
        history.add(0, query);
        while (history.size() > MAX_QUERIES) {
            history.remove(history.size() - 1);
        }
        save(currentAccount);
    }

    private static void removeQuery(int currentAccount, String query) {
        if (getQueries(currentAccount).remove(query)) {
            save(currentAccount);
        }
    }

    private static void clearQueries(int currentAccount) {
        final ArrayList<String> history = getQueries(currentAccount);
        if (history.isEmpty()) {
            return;
        }
        history.clear();
        save(currentAccount);
    }

    public static void fillItems(int currentAccount, ArrayList<UItem> items) {
        final ArrayList<String> history = getQueries(currentAccount);
        if (history.isEmpty()) {
            return;
        }
        items.add(UItem.asGraySection(getString(R.string.Recent)));
        for (int i = 0; i < history.size(); ++i) {
            items.add(UItem.asButton(FIRST_QUERY_ITEM_ID + i, R.drawable.msg_recent, history.get(i)));
        }
        items.add(UItem.asButton(CLEAR_ITEM_ID, R.drawable.msg_clear_recent, getString(R.string.ClearHistory)));
    }

    private static String getQuery(int currentAccount, UItem item) {
        if (item.viewType != UniversalAdapter.VIEW_TYPE_TEXT) {
            return null;
        }
        final ArrayList<String> history = getQueries(currentAccount);
        final int index = item.id - FIRST_QUERY_ITEM_ID;
        if (index < 0 || index >= history.size()) {
            return null;
        }
        return history.get(index);
    }

    public static boolean onItemClick(
        int currentAccount,
        UItem item,
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        Utilities.Callback<String> onQuerySelected,
        Runnable onHistoryChanged
    ) {
        if (item.viewType == UniversalAdapter.VIEW_TYPE_TEXT && item.id == CLEAR_ITEM_ID) {
            new AlertDialog.Builder(context, resourcesProvider)
                .setTitle(getString(R.string.ClearSearchAlertTitle))
                .setMessage(getString(R.string.ClearSearchAlert))
                .setPositiveButton(getString(R.string.ClearButton), (dialog, which) -> {
                    clearQueries(currentAccount);
                    onHistoryChanged.run();
                })
                .setNegativeButton(getString(R.string.Cancel), null)
                .show();
            return true;
        }
        final String query = getQuery(currentAccount, item);
        if (query == null) {
            return false;
        }
        if (onQuerySelected != null) {
            onQuerySelected.run(query);
        }
        return true;
    }

    public static boolean onItemLongClick(
        int currentAccount,
        UItem item,
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        Runnable onHistoryChanged
    ) {
        final String query = getQuery(currentAccount, item);
        if (query == null) {
            return false;
        }
        new AlertDialog.Builder(context, resourcesProvider)
            .setTitle(getString(R.string.ClearSearchSingleAlertTitle))
            .setMessage(formatString(R.string.ClearSearchSingleChatAlertText, query))
            .setPositiveButton(getString(R.string.ClearSearchRemove), (dialog, which) -> {
                removeQuery(currentAccount, query);
                onHistoryChanged.run();
            })
            .setNegativeButton(getString(R.string.Cancel), null)
            .show();
        return true;
    }
}
