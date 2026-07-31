package org.ushastoe.fluffy.patches;

import android.text.TextUtils;
import android.util.SparseArray;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;

/**
 * Public posts search used to accept hashtags only. The feed tab searches by plain words too,
 * so the query goes either into the hashtag or into the query field of channels.searchPosts.
 */
public final class PostsFeedPatch {

    private static final int FLAG_HASHTAG = 1;
    private static final int FLAG_QUERY = 2;
    private static final int FLAG_PAID_STARS = 4;

    private static final int SEARCH_PANEL_HEIGHT = 44;
    private static final int SEARCH_PANEL_MARGIN = 7;

    private static final SparseArray<Long> pendingPaidStars = new SparseArray<>();
    private static final SparseArray<Utilities.Callback<TLRPC.TL_error>> errorListeners = new SparseArray<>();

    private PostsFeedPatch() {
    }

    /** Stars the user agreed to spend, applied to the next public posts search of this account. */
    public static void setPendingPaidStars(int currentAccount, long stars) {
        if (stars > 0) {
            pendingPaidStars.put(currentAccount, stars);
        } else {
            pendingPaidStars.remove(currentAccount);
        }
    }

    public static void applyPaidStars(TLRPC.TL_channels_searchPosts req, int currentAccount) {
        final Long stars = pendingPaidStars.get(currentAccount);
        if (stars == null) {
            return;
        }
        pendingPaidStars.remove(currentAccount);
        req.flags |= FLAG_PAID_STARS;
        req.allow_paid_stars = stars;
    }

    public static void setSearchErrorListener(int currentAccount, Utilities.Callback<TLRPC.TL_error> listener) {
        if (listener == null) {
            errorListeners.remove(currentAccount);
        } else {
            errorListeners.put(currentAccount, listener);
        }
    }

    public static void onSearchError(int currentAccount, TLRPC.TL_error error) {
        final Utilities.Callback<TLRPC.TL_error> listener = errorListeners.get(currentAccount);
        if (listener == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> listener.run(error));
    }

    /**
     * A chat embedded into a container pads its list by the navigation bar only, so the bottom
     * "N results" panel of search mode ends up drawn over the last post.
     */
    public static int getEmbeddedSearchBottomPadding(boolean searchMode, boolean panelVisible) {
        if (!searchMode || !panelVisible) {
            return 0;
        }
        return AndroidUtilities.dp(SEARCH_PANEL_HEIGHT + SEARCH_PANEL_MARGIN * 2);
    }

    public static boolean isHashtag(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        final char first = query.charAt(0);
        return first == '#' || first == '$';
    }

    public static void applySearchQuery(TLRPC.TL_channels_searchPosts req, String query) {
        if (isHashtag(query)) {
            req.flags |= FLAG_HASHTAG;
            req.hashtag = query;
        } else {
            req.flags |= FLAG_QUERY;
            req.query = query;
        }
    }
}
