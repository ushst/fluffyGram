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
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.utils.HistoryIntegrityChecker;

import java.util.ArrayList;

/**
 * Lists local chats whose cached message history currently has gaps ("holes") that
 * still need to be re-downloaded from the server, so gaps like the one produced by
 * an accidental cache wipe don't go unnoticed. Tapping a chat opens it, which is
 * enough to make the client fetch the missing history back from Telegram's servers.
 */
public class FluffyHistoryHolesActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private static final int ROW_HEADER = 0;
    private static final int ROW_REFRESH = 1;
    private static final int ROW_INFO = 2;
    private static final int DIALOG_ROW_BASE = 100;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<RowItem> items = new ArrayList<>();
    private ArrayList<MessagesStorage.DialogHoleInfo> holes = new ArrayList<>();
    private boolean loading;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyHistoryHolesTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            RowItem item = items.get(position);
            if (item.id == ROW_REFRESH) {
                load();
            } else if (item.dialogId != 0) {
                openDialog(item.dialogId);
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = frameLayout;
        updateItems();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (loading) {
            return;
        }
        loading = true;
        updateItems();
        HistoryIntegrityChecker.loadSummaryAsync(currentAccount, result -> {
            loading = false;
            holes = result.dialogs;
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
        items.add(new RowItem(VIEW_TYPE_HEADER, ROW_HEADER, LocaleController.getString(R.string.FluffyHistoryHolesTitle), null, 0));
        items.add(new RowItem(VIEW_TYPE_TEXT, ROW_REFRESH, LocaleController.getString(R.string.FluffyHistoryHolesRefresh), null, 0));
        if (loading) {
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO, LocaleController.getString(R.string.FluffyHistoryHolesLoading), null, 0));
        } else if (holes.isEmpty()) {
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO, LocaleController.getString(R.string.FluffyHistoryHolesEmpty), null, 0));
        } else {
            for (int i = 0; i < holes.size(); i++) {
                MessagesStorage.DialogHoleInfo info = holes.get(i);
                String name = DialogObject.getName(currentAccount, info.dialogId);
                if (TextUtils.isEmpty(name)) {
                    name = String.valueOf(info.dialogId);
                }
                CharSequence value = LocaleController.formatString(R.string.FluffyHistoryHolesGapValue, info.holeCount, info.totalGap);
                items.add(new RowItem(VIEW_TYPE_TEXT, DIALOG_ROW_BASE + i, name, value, info.dialogId));
            }
        }
        items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO + 1, LocaleController.getString(R.string.FluffyHistoryHolesInfo), null, 0));
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

        RowItem(int viewType, int id, CharSequence text, CharSequence value, long dialogId) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.value = value;
            this.dialogId = dialogId;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= items.size()) {
                return false;
            }
            return items.get(position).viewType == VIEW_TYPE_TEXT;
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
            } else {
                view = new TextInfoPrivacyCell(parent.getContext());
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowItem item = items.get(position);
            if (holder.itemView instanceof HeaderCell) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.itemView instanceof TextSettingsCell) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (!TextUtils.isEmpty(item.value)) {
                    cell.setTextAndValue(item.text, item.value, false);
                } else {
                    cell.setText(item.text, false);
                }
            } else if (holder.itemView instanceof TextInfoPrivacyCell) {
                ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
            }
        }
    }
}
