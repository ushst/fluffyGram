package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.patches.PostsFeedPatch;

public final class PostsFeedHook {

    private PostsFeedHook() {
    }

    public static void applyPostsSearchQuery(TLRPC.TL_channels_searchPosts req, String query, int currentAccount) {
        PostsFeedPatch.applySearchQuery(req, query);
        PostsFeedPatch.applyPaidStars(req, currentAccount);
    }

    public static void setPendingPaidStars(int currentAccount, long stars) {
        PostsFeedPatch.setPendingPaidStars(currentAccount, stars);
    }

    public static void setSearchErrorListener(int currentAccount, Utilities.Callback<TLRPC.TL_error> listener) {
        PostsFeedPatch.setSearchErrorListener(currentAccount, listener);
    }

    public static void onSearchError(int currentAccount, TLRPC.TL_error error) {
        PostsFeedPatch.onSearchError(currentAccount, error);
    }

    public static boolean isHashtag(String query) {
        return PostsFeedPatch.isHashtag(query);
    }

    public static int getEmbeddedSearchBottomPadding(boolean searchMode, boolean panelVisible) {
        return PostsFeedPatch.getEmbeddedSearchBottomPadding(searchMode, panelVisible);
    }
}
