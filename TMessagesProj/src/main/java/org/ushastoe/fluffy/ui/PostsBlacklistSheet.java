package org.ushastoe.fluffy.ui;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BottomSheetWithRecyclerListView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.ushastoe.fluffy.patches.PostsBlacklistPatch;

import java.util.ArrayList;

/**
 * Channels hidden from posts search: the ones hidden for the current query and the ones hidden
 * everywhere, each restorable in one tap.
 */
public class PostsBlacklistSheet extends BottomSheetWithRecyclerListView {

    private static final int ITEM_QUERY_START = 1000;
    private static final int ITEM_GLOBAL_START = 2000;

    private final String query;
    private final Runnable onChanged;

    private UniversalAdapter adapter;

    private final ArrayList<Long> hiddenForQuery = new ArrayList<>();
    private final ArrayList<Long> hiddenGlobally = new ArrayList<>();

    public PostsBlacklistSheet(BaseFragment fragment, String query, Runnable onChanged) {
        super(fragment, false, false);

        this.query = query;
        this.onChanged = onChanged;

        recyclerListView.setOnItemClickListener((view, position) -> {
            if (position == 0) {
                return;
            }
            final UItem item = adapter.getItem(position - 1);
            if (item == null) {
                return;
            }
            restore(item);
        });
        updateTitle();
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.FluffyPostsHiddenTitle);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter(RecyclerListView listView) {
        adapter = new UniversalAdapter(listView, getContext(), currentAccount, 0, true, this::fillItems, resourcesProvider);
        return adapter;
    }

    private void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        hiddenForQuery.clear();
        hiddenGlobally.clear();
        hiddenForQuery.addAll(PostsBlacklistPatch.getHidden(currentAccount, query, false));
        hiddenGlobally.addAll(PostsBlacklistPatch.getHidden(currentAccount, query, true));

        if (hiddenForQuery.isEmpty() && hiddenGlobally.isEmpty()) {
            items.add(UItem.asShadow(getString(R.string.FluffyPostsHiddenEmpty)));
            return;
        }
        if (!hiddenForQuery.isEmpty() && !TextUtils.isEmpty(query)) {
            items.add(UItem.asGraySection(formatString(R.string.FluffyPostsHiddenInQuery, query)));
            for (int i = 0; i < hiddenForQuery.size(); ++i) {
                items.add(asChannel(ITEM_QUERY_START + i, hiddenForQuery.get(i)));
            }
        }
        if (!hiddenGlobally.isEmpty()) {
            items.add(UItem.asGraySection(getString(R.string.FluffyPostsHiddenEverywhere)));
            for (int i = 0; i < hiddenGlobally.size(); ++i) {
                items.add(asChannel(ITEM_GLOBAL_START + i, hiddenGlobally.get(i)));
            }
        }
        items.add(UItem.asShadow(getString(R.string.FluffyPostsHiddenRestore)));
    }

    private UItem asChannel(int id, long dialogId) {
        return UItem.asButton(id, R.drawable.msg_block2, PostsBlacklistPatch.getChannelName(currentAccount, dialogId));
    }

    private void restore(UItem item) {
        final boolean everywhere = item.id >= ITEM_GLOBAL_START;
        final int index = item.id - (everywhere ? ITEM_GLOBAL_START : ITEM_QUERY_START);
        final ArrayList<Long> list = everywhere ? hiddenGlobally : hiddenForQuery;
        if (index < 0 || index >= list.size()) {
            return;
        }
        final long dialogId = list.get(index);
        PostsBlacklistPatch.show(currentAccount, query, dialogId, everywhere);
        adapter.update(true);
        if (onChanged != null) {
            onChanged.run();
        }
        BulletinFactory.of(container, resourcesProvider)
            .createSimpleBulletin(R.raw.chats_infotip, formatString(R.string.FluffyPostsShownToast, PostsBlacklistPatch.getChannelName(currentAccount, dialogId)))
            .show();
    }
}
