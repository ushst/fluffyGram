package org.ushastoe.fluffy.patches;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;

/**
 * "Refresh" icon of the search field: reruns the current "Posts" query,
 * which otherwise can only be repeated by retyping it.
 */
public final class PostsSearchRefreshPatch {

    private static final int MENU_ITEM_ID = -48;

    private PostsSearchRefreshPatch() {
    }

    public static ActionBarMenuItem createRefreshItem(ActionBarMenu menu, Runnable onClick) {
        final ActionBarMenuItem item = menu.addItem(MENU_ITEM_ID, R.drawable.msg_retry);
        AndroidUtilities.removeFromParent(item);
        item.setContentDescription(getString(R.string.Refresh));
        item.setOnClickListener(v -> onClick.run());
        item.setAlpha(0.0f);
        item.setVisibility(View.GONE);
        return item;
    }
}
