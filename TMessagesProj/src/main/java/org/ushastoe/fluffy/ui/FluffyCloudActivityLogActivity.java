package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.sync.FluffySyncManager;

import java.util.ArrayList;
import java.util.Date;

public class FluffyCloudActivityLogActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffySyncActivity));
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
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        setLoadingState();
        fragmentView = frameLayout;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        setLoadingState();
        loadActivity();
    }

    private void setLoadingState() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_HEADER, LocaleController.getString(R.string.FluffySyncActivity), null));
        items.add(new ItemInner(VIEW_TYPE_INFO, LocaleController.getString(R.string.Loading), null));
        notifyItemsChanged();
    }

    private void loadActivity() {
        FluffySyncManager.getInstance().loadActivityLog(this, 20, (entries, role, errorMessage) -> {
            items.clear();
            items.add(new ItemInner(VIEW_TYPE_HEADER, LocaleController.getString(R.string.FluffySyncActivity), null));
            items.add(new ItemInner(VIEW_TYPE_TEXT, LocaleController.getString(R.string.FluffySyncRole), role));
            if (!TextUtils.isEmpty(errorMessage)) {
                items.add(new ItemInner(VIEW_TYPE_INFO, errorMessage, null));
                notifyItemsChanged();
                return;
            }
            if (entries == null || entries.isEmpty()) {
                items.add(new ItemInner(VIEW_TYPE_INFO, LocaleController.getString(R.string.FluffySyncActivityEmpty), null));
                notifyItemsChanged();
                return;
            }
            for (FluffySyncManager.ActivityEntry entry : entries) {
                items.add(new ItemInner(VIEW_TYPE_TEXT, formatEntryText(entry), formatEntryTime(entry)));
            }
            notifyItemsChanged();
        });
    }

    private CharSequence formatEntryText(FluffySyncManager.ActivityEntry entry) {
        String deviceName = entry != null && !TextUtils.isEmpty(entry.deviceName)
                ? entry.deviceName
                : LocaleController.getString(R.string.FluffySyncUnknownDevice);
        if (entry != null && "download".equals(entry.action)) {
            return LocaleController.formatString("FluffySyncActivityDownloaded", R.string.FluffySyncActivityDownloaded, deviceName);
        }
        return LocaleController.formatString("FluffySyncActivityUploaded", R.string.FluffySyncActivityUploaded, deviceName);
    }

    private CharSequence formatEntryTime(FluffySyncManager.ActivityEntry entry) {
        if (entry == null || entry.createdAtMs <= 0L) {
            return null;
        }
        return DateFormat.getMediumDateFormat(getParentActivity()).format(new Date(entry.createdAtMs))
                + " "
                + DateFormat.getTimeFormat(getParentActivity()).format(new Date(entry.createdAtMs));
    }

    private void notifyItemsChanged() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static final class ItemInner {
        final int viewType;
        final CharSequence text;
        final CharSequence value;

        ItemInner(int viewType, CharSequence text, CharSequence value) {
            this.viewType = viewType;
            this.text = text;
            this.value = value;
        }
    }

    private final class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
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
            ItemInner item = items.get(position);
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
