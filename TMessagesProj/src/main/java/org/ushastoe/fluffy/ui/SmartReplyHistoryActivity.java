package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;
import org.ushastoe.fluffy.smartreply.SmartReplyEngine;

import java.text.DateFormat;
import java.util.Date;

/** Local-only inspection of previously collected replies for the current account. */
public class SmartReplyHistoryActivity extends BaseFragment {
    private SmartReplyEngine.HistoryView history;
    private ListAdapter adapter;
    private boolean destroyed;
    private int requestId;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.FluffySmartReplyHistoryView));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        RecyclerListView list = new RecyclerListView(context);
        list.setLayoutManager(new LinearLayoutManager(context));
        list.setAdapter(adapter = new ListAdapter());
        actionBar.setAdaptiveBackground(list);
        list.setOnItemClickListener((view, position) -> {
            if (getParentActivity() == null || history == null) {
                return;
            }
            if (position == 1) {
                showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                        .setTitle(LocaleController.getString(R.string.FluffySmartReplyHistoryClear))
                        .setMessage(LocaleController.getString(R.string.FluffySmartReplyHistoryClearConfirm))
                        .setPositiveButton(LocaleController.getString(R.string.Clear), (dialog, which) -> {
                            AppearanceSettingsHook.setSmartReplyHistoryEnabled(false);
                            history = null;
                            adapter.notifyDataSetChanged();
                            reload();
                        })
                        .setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
            } else if (position >= 2 && position - 2 < history.entries.size()) {
                SmartReplyEngine.HistoryEntry entry = history.entries.get(position - 2);
                showDialog(new AlertDialog.Builder(getParentActivity(), getResourceProvider())
                        .setTitle(LocaleController.getString(R.string.FluffySmartReplyHistoryView))
                        .setMessage(LocaleController.formatString(R.string.FluffySmartReplyHistoryPair,
                                entry.incoming, entry.reply))
                        .setPositiveButton(LocaleController.getString(R.string.OK), null).create());
            }
        });
        frame.addView(list, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        fragmentView = frame;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    @Override
    public void onFragmentDestroy() {
        destroyed = true;
        requestId++;
        history = null;
        super.onFragmentDestroy();
    }

    private void reload() {
        final int request = ++requestId;
        SmartReplyEngine.getInstance().inspectHistory(currentAccount, result -> {
            if (destroyed || request != requestId || adapter == null) {
                return;
            }
            history = result;
            adapter.notifyDataSetChanged();
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return 2 + Math.max(1, history == null ? 0 : history.entries.size());
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return history != null && (position == 1 && history.enabled
                    || position >= 2 && position - 2 < history.entries.size());
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 1) {
                return 1;
            }
            return position == 0 || history != null && !history.entries.isEmpty() ? 0 : 2;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            View view;
            if (type == 0) {
                TextDetailSettingsCell cell = new TextDetailSettingsCell(parent.getContext());
                cell.setMultilineDetail(true);
                view = cell;
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (type == 1) {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else {
                view = new TextInfoPrivacyCell(parent.getContext());
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                String updated = history != null && history.builtAt > 0
                        ? LocaleController.formatString(R.string.FluffySmartReplyHistoryUpdated,
                        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(history.builtAt)))
                        : "";
                ((TextDetailSettingsCell) holder.itemView).setTextAndValue(
                        LocaleController.formatString(R.string.FluffySmartReplyHistoryCount,
                                history == null ? 0 : history.entries.size()), updated, true);
            } else if (position == 1) {
                ((TextSettingsCell) holder.itemView).setText(
                        LocaleController.getString(R.string.FluffySmartReplyHistoryClear), false);
                holder.itemView.setAlpha(history != null && history.enabled ? 1f : 0.5f);
            } else if (holder.itemView instanceof TextInfoPrivacyCell) {
                ((TextInfoPrivacyCell) holder.itemView).setText(LocaleController.getString(history == null
                        ? R.string.Loading : history.enabled
                        ? R.string.FluffySmartReplyHistoryEmpty : R.string.FluffySmartReplyHistoryOff));
            } else {
                SmartReplyEngine.HistoryEntry entry = history.entries.get(position - 2);
                ((TextDetailSettingsCell) holder.itemView).setTextAndValue(entry.reply, entry.incoming,
                        position < getItemCount() - 1);
            }
        }
    }
}
