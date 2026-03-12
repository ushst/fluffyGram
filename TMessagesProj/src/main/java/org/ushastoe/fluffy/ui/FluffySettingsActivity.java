package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.text.TextUtils;
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
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.ui.elements.HeaderSettingsCell;

import java.util.ArrayList;

public class FluffySettingsActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;
    private static final int VIEW_TYPE_ABOUT_HEADER = 3;
    private static final int VIEW_TYPE_SHADOW = 4;

    private static final int ROW_ABOUT = 0;
    private static final int ROW_ABOUT_SHADOW = 1;
    private static final int ROW_APPEARANCE_SECTION = 2;
    private static final int ROW_APPEARANCE = 3;
    private static final int ROW_APPEARANCE_INFO = 4;
    private static final int ROW_PREMIUM_SECTION = 5;
    private static final int ROW_PREMIUM = 6;
    private static final int ROW_PREMIUM_INFO = 7;
    private static final int ROW_DEBUG_SECTION = 8;
    private static final int ROW_DEBUG = 9;
    private static final int ROW_DEBUG_INFO = 10;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffySettings));
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
            ItemInner item = items.get(position);
            if (item.id == ROW_APPEARANCE) {
                presentFragment(new FluffyAppearanceActivity());
            } else if (item.id == ROW_PREMIUM) {
                presentFragment(new FluffyPremiumActivity());
            } else if (item.id == ROW_DEBUG) {
                presentFragment(new FluffyDebugActivity());
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();

        fragmentView = frameLayout;
        return fragmentView;
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_ABOUT_HEADER, ROW_ABOUT, null, null));
        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_ABOUT_SHADOW, null, null));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_APPEARANCE_SECTION, LocaleController.getString(R.string.FluffyAppearanceSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_APPEARANCE, LocaleController.getString(R.string.FluffyAppearance), null));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_APPEARANCE_INFO, LocaleController.getString(R.string.FluffyAppearanceInfo), null));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_PREMIUM_SECTION, LocaleController.getString(R.string.FluffyPremiumSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_PREMIUM, LocaleController.getString(R.string.FluffyPremium), null));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_PREMIUM_INFO, LocaleController.getString(R.string.FluffyPremiumInfo), null));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_DEBUG_SECTION, LocaleController.getString(R.string.FluffyDeveloperSettingsSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DEBUG, LocaleController.getString(R.string.FluffyDebug), null));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_DEBUG_INFO, LocaleController.getString(R.string.FluffyDebugInfo), null));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final CharSequence value;

        ItemInner(int viewType, int id, CharSequence text, CharSequence value) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.value = value;
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
            } else if (viewType == VIEW_TYPE_ABOUT_HEADER) {
                view = new HeaderSettingsCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_SHADOW) {
                view = new ShadowSectionCell(parent.getContext(), 12);
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
            if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.getItemViewType() == VIEW_TYPE_ABOUT_HEADER) {
                // Static decorative header, no binding required.
            } else if (holder.getItemViewType() == VIEW_TYPE_SHADOW) {
                // Static section separator, no binding required.
            } else if (holder.getItemViewType() == VIEW_TYPE_TEXT) {
                ((TextSettingsCell) holder.itemView).setText(item.text, false);
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                if (TextUtils.isEmpty(item.text)) {
                    cell.setFixedSize(12);
                    cell.setText(null);
                } else {
                    cell.setFixedSize(0);
                    cell.setText(item.text);
                }
            }
        }
    }
}
