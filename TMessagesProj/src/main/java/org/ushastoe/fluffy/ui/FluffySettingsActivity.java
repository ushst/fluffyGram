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

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.AppFontHook;
import org.ushastoe.fluffy.hooks.PremiumAccessHook;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.ui.elements.HeaderSettingsCell;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;

import java.util.ArrayList;

public class FluffySettingsActivity extends BaseFragment {

    private static final String ARG_TARGET = "fluffy_settings_target";
    private static final int MAX_RIGHT_VALUE_LENGTH = 10;

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
    private static final int ROW_LINKS_SECTION = 11;
    private static final int ROW_CHANNEL = 12;
    private static final int ROW_GITHUB = 13;

    private static final String FLUFFY_CHANNEL_USERNAME = "fluffyGram";
    private static final String FLUFFY_GITHUB_URL = "https://github.com/ushst/fluffyGram";

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    public FluffySettingsActivity() {
        super();
    }

    public FluffySettingsActivity(Bundle args) {
        super(args);
    }

    public static FluffySettingsActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffySettingsActivity(args);
    }

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
            } else if (item.id == ROW_CHANNEL) {
                MessagesController.getInstance(currentAccount).openByUserName(FLUFFY_CHANNEL_USERNAME, this, 1);
            } else if (item.id == ROW_GITHUB) {
                Browser.openUrl(getParentActivity(), FLUFFY_GITHUB_URL);
            }
        });
        listView.setOnItemLongClickListener((view, position) -> copyDeepLinkForPosition(position));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();
        applyTargetScroll();

        fragmentView = frameLayout;
        return fragmentView;
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_ABOUT_HEADER, ROW_ABOUT, null, null, 0));
        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_ABOUT_SHADOW, null, null, 0));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_APPEARANCE_SECTION, LocaleController.getString(R.string.FluffyAppearanceSection), null, 0));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_APPEARANCE, LocaleController.getString(R.string.FluffyAppearance), null, R.drawable.msg_theme));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_APPEARANCE_INFO, LocaleController.getString(R.string.FluffyAppearanceInfo), null, 0));
        if (PremiumAccessHook.hasPremiumAccess()) {
            items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_PREMIUM_SECTION, LocaleController.getString(R.string.FluffyPremiumSection), null, 0));
            items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_PREMIUM, LocaleController.getString(R.string.FluffyPremium), null, R.drawable.msg_settings_premium));
            items.add(new ItemInner(VIEW_TYPE_INFO, ROW_PREMIUM_INFO, LocaleController.getString(R.string.FluffyPremiumInfo), null, 0));
        }
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_DEBUG_SECTION, LocaleController.getString(R.string.FluffyDeveloperSettingsSection), null, 0));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DEBUG, LocaleController.getString(R.string.FluffyDebug), null, R.drawable.msg_log));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_DEBUG_INFO, LocaleController.getString(R.string.FluffyDebugInfo), null, 0));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_LINKS_SECTION, LocaleController.getString(R.string.FluffyLinksSection), null, 0));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_CHANNEL, LocaleController.getString(R.string.ProfileChannel), "t.me/fluffyGram", R.drawable.msg_channel));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GITHUB, LocaleController.getString(R.string.FluffyGitHub), "github.com/ushst/fluffyGram", R.drawable.msg_link2));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        applyTargetScroll();
    }

    private boolean copyDeepLinkForPosition(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        return FluffySettingsDeepLinkPatch.copyLink(this, getDeepLinkForItem(items.get(position)));
    }

    private String getDeepLinkForItem(ItemInner item) {
        if (item == null) {
            return null;
        }
        switch (item.id) {
            case ROW_APPEARANCE_SECTION:
            case ROW_APPEARANCE:
            case ROW_APPEARANCE_INFO:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance");
            case ROW_PREMIUM_SECTION:
            case ROW_PREMIUM:
            case ROW_PREMIUM_INFO:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("premium");
            case ROW_DEBUG_SECTION:
            case ROW_DEBUG:
            case ROW_DEBUG_INFO:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("debug");
            case ROW_LINKS_SECTION:
            case ROW_CHANNEL:
            case ROW_GITHUB:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("community");
            default:
                return FluffySettingsDeepLinkPatch.buildSettingsLink();
        }
    }

    private void applyTargetScroll() {
        if (listView == null) {
            return;
        }
        int rowId = getTargetRowId();
        if (rowId < 0) {
            return;
        }
        int index = findItemIndexById(rowId);
        if (index < 0) {
            return;
        }
        listView.post(() -> {
            if (listView == null) {
                return;
            }
            FluffySettingsTargetAnimator.scrollAndPulseTarget(listView, index);
        });
    }

    private int getTargetRowId() {
        Bundle args = getArguments();
        if (args == null) {
            return -1;
        }
        String target = args.getString(ARG_TARGET);
        if (TextUtils.isEmpty(target)) {
            return -1;
        }
        switch (target) {
            case "appearance":
                return ROW_APPEARANCE;
            case "premium":
                return ROW_PREMIUM;
            case "debug":
                return ROW_DEBUG;
            case "community":
                return ROW_CHANNEL;
            default:
                return -1;
        }
    }

    private int findItemIndexById(int id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    private CharSequence getDisplayValue(CharSequence value) {
        if (value == null) {
            return null;
        }
        String source = value.toString();
        if (source.length() <= MAX_RIGHT_VALUE_LENGTH) {
            return source;
        }
        return source.substring(0, MAX_RIGHT_VALUE_LENGTH) + "...";
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final CharSequence value;
        final int iconResId;

        ItemInner(int viewType, int id, CharSequence text, CharSequence value, int iconResId) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.value = value;
            this.iconResId = iconResId;
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
                TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(parent.getContext());
                AppFontHook.applyToTextView(infoCell.getTextView());
                view = infoCell;
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
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (TextUtils.isEmpty(item.value)) {
                    cell.setText(item.text, false);
                } else {
                    cell.setTextAndValue(item.text, getDisplayValue(item.value), false);
                }
                cell.setIcon(item.iconResId);
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
