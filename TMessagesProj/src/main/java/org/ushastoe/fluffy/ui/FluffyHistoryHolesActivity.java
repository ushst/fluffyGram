package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.utils.HistoryIntegrityChecker;

import java.util.ArrayList;

/**
 * Lists chats with mid-history gaps in the local cache (not the normal
 * "older history not loaded yet" hole). Tapping a chat opens it so Telegram can
 * re-download the missing range.
 */
public class FluffyHistoryHolesActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;
    private static final int VIEW_TYPE_SHADOW = 3;

    private static final int ROW_HEADER = 0;
    private static final int ROW_STATUS = 1;
    private static final int ROW_SHADOW = 2;
    private static final int ROW_INFO = 3;
    private static final int DIALOG_ROW_BASE = 100;
    private static final int MENU_REFRESH = 1;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<RowItem> items = new ArrayList<>();
    private ArrayList<MessagesStorage.DialogHoleInfo> holes = new ArrayList<>();
    private boolean loading;
    private boolean loaded;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyHistoryHolesTitle));
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(MENU_REFRESH, LocaleController.getString(R.string.FluffyHistoryHolesRefresh));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_REFRESH) {
                    load(true);
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            RowItem item = items.get(position);
            if (item.dialogId != 0) {
                openDialog(item.dialogId);
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = frameLayout;
        updateItems();
        load(false);
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (loaded) {
            updateItems();
        }
    }

    private void load(boolean force) {
        if (loading) {
            return;
        }
        if (loaded && !force) {
            return;
        }
        loading = true;
        updateItems();
        HistoryIntegrityChecker.loadSummaryAsync(currentAccount, result -> {
            loading = false;
            loaded = true;
            holes = result.dialogs != null ? result.dialogs : new ArrayList<>();
            updateItems();
        });
    }

    private void openDialog(long dialogId) {
        if (getParentActivity() == null) {
            return;
        }
        Bundle args = new Bundle();
        if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else if (DialogObject.isChatDialog(dialogId)) {
            args.putLong("chat_id", -dialogId);
        } else {
            return;
        }
        if (!getMessagesController().checkCanOpenChat(args, this)) {
            return;
        }
        presentFragment(new ChatActivity(args));
    }

    private void updateItems() {
        items.clear();
        items.add(new RowItem(VIEW_TYPE_HEADER, ROW_HEADER, LocaleController.getString(R.string.FluffyHistoryHolesSection), null, 0, false));

        if (loading && !loaded) {
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_STATUS, LocaleController.getString(R.string.FluffyHistoryHolesLoading), null, 0, false));
        } else if (holes.isEmpty()) {
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_STATUS, LocaleController.getString(R.string.FluffyHistoryHolesEmpty), null, 0, false));
        } else {
            for (int i = 0; i < holes.size(); i++) {
                MessagesStorage.DialogHoleInfo info = holes.get(i);
                String name = DialogObject.getName(currentAccount, info.dialogId);
                if (TextUtils.isEmpty(name)) {
                    name = String.valueOf(info.dialogId);
                }
                CharSequence value = LocaleController.formatString(
                        R.string.FluffyHistoryHolesGapValue,
                        info.holeCount,
                        info.totalGap,
                        info.rangeStart,
                        info.rangeEnd);
                boolean divider = i < holes.size() - 1;
                items.add(new RowItem(VIEW_TYPE_TEXT, DIALOG_ROW_BASE + i, name, value, info.dialogId, divider));
            }
            items.add(new RowItem(VIEW_TYPE_SHADOW, ROW_SHADOW, null, null, 0, false));
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO, LocaleController.getString(R.string.FluffyHistoryHolesInfo), null, 0, false));
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static class RowItem {
        final int viewType;
        final int id;
        final CharSequence text;
        final CharSequence value;
        final long dialogId;
        final boolean divider;

        RowItem(int viewType, int id, CharSequence text, CharSequence value, long dialogId, boolean divider) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.value = value;
            this.dialogId = dialogId;
            this.divider = divider;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_TYPE_TEXT;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_SHADOW) {
                view = new ShadowSectionCell(parent.getContext(), 12);
            } else {
                view = new TextInfoPrivacyCell(parent.getContext());
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowItem item = items.get(position);
            int viewType = holder.getItemViewType();
            if (viewType == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (viewType == VIEW_TYPE_TEXT) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (!TextUtils.isEmpty(item.value)) {
                    cell.setTextAndValue(item.text, item.value, item.divider);
                } else {
                    cell.setText(item.text, item.divider);
                }
            } else if (viewType == VIEW_TYPE_INFO) {
                ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
            }
        }
    }
}
