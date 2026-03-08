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
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;

import java.util.ArrayList;

public class FluffyAppearanceActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_TEXT = 2;

    private static final int ROW_APPEARANCE_HEADER = 0;
    private static final int ROW_HIDE_CHANNEL_POST_STARS_OFFER = 1;
    private static final int ROW_DIALOGS_TITLE_MODE = 2;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyAppearance));
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
            if (item.id == ROW_HIDE_CHANNEL_POST_STARS_OFFER) {
                boolean hidden = !AppearanceSettingsHook.isChannelPostStarsOfferHidden();
                AppearanceSettingsHook.setChannelPostStarsOfferHidden(hidden);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(hidden);
                }
            } else if (item.id == ROW_DIALOGS_TITLE_MODE) {
                showDialogsTitleModeDialog();
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();

        fragmentView = frameLayout;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateItems();
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_APPEARANCE_HEADER,
                LocaleController.getString(R.string.FluffyAppearanceSection), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_HIDE_CHANNEL_POST_STARS_OFFER,
                LocaleController.getString(R.string.FluffyHideChannelPostStarsOffer),
                AppearanceSettingsHook.isChannelPostStarsOfferHidden()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DIALOGS_TITLE_MODE,
                LocaleController.getString(R.string.FluffyCenterDialogsTitle),
                false));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showDialogsTitleModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[] {
                LocaleController.getString(R.string.FluffyDialogsTitleModeDefault),
                LocaleController.getString(R.string.FluffyDialogsTitleModeCentered),
                LocaleController.getString(R.string.FluffyDialogsTitleModeCenteredIgnoreActions)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyCenterDialogsTitle));
        builder.setItems(items, (dialog, which) -> {
            AppearanceSettingsHook.setDialogsTitleMode(which);
            updateItems();
        });
        showDialog(builder.create());
    }

    private CharSequence getDialogsTitleModeValue() {
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_CENTERED) {
            return LocaleController.getString(R.string.FluffyDialogsTitleModeCentered);
        }
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS) {
            return LocaleController.getString(R.string.FluffyDialogsTitleModeCenteredIgnoreActions);
        }
        return LocaleController.getString(R.string.FluffyDialogsTitleModeDefault);
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final boolean checked;

        ItemInner(int viewType, int id, CharSequence text, boolean checked) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.checked = checked;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == VIEW_TYPE_CHECK || type == VIEW_TYPE_TEXT;
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
            } else if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.getItemViewType() == VIEW_TYPE_CHECK) {
                ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, item.checked, false);
            } else {
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, getDialogsTitleModeValue(), false);
            }
        }
    }
}
