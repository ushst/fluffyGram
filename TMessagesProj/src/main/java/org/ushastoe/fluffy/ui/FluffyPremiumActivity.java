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
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.PremiumSettingsHook;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.patches.PremiumSettingsPatch;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;

import java.util.ArrayList;

public class FluffyPremiumActivity extends BaseFragment {

    private static final String ARG_TARGET = "fluffy_premium_target";

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_INFO = 2;
    private static final int VIEW_TYPE_SETTING = 3;

    private static final int ROW_PREMIUM_HEADER = 0;
    private static final int ROW_LOCAL_ANON_STORY_VIEW = 1;
    private static final int ROW_LOCAL_ANON_STORY_VIEW_INFO = 2;
    private static final int ROW_LOCAL_MESSAGE_FAKE_EDIT = 3;
    private static final int ROW_LOCAL_MESSAGE_FAKE_EDIT_INFO = 4;
    private static final int ROW_LOCAL_MESSAGE_HISTORY = 5;
    private static final int ROW_LOCAL_MESSAGE_HISTORY_INFO = 6;
    private static final int ROW_SAVE_DELETED_MESSAGES = 7;
    private static final int ROW_SAVE_DELETED_MESSAGES_INFO = 8;
    private static final int ROW_DELETED_MESSAGE_MARKER_MODE = 9;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    public FluffyPremiumActivity() {
        super();
    }

    public FluffyPremiumActivity(Bundle args) {
        super(args);
    }

    public static FluffyPremiumActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffyPremiumActivity(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyPremium));
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
            if (item.id == ROW_LOCAL_ANON_STORY_VIEW) {
                boolean enabled = !PremiumSettingsHook.useLocalAnonymousStoryView();
                PremiumSettingsHook.setUseLocalAnonymousStoryView(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_LOCAL_MESSAGE_FAKE_EDIT) {
                boolean enabled = !PremiumSettingsHook.isLocalMessageFakeEditEnabled();
                PremiumSettingsHook.setLocalMessageFakeEditEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_LOCAL_MESSAGE_HISTORY) {
                boolean enabled = !PremiumSettingsHook.isLocalMessageHistoryEnabled();
                PremiumSettingsHook.setLocalMessageHistoryEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_SAVE_DELETED_MESSAGES) {
                boolean enabled = !PremiumSettingsHook.isSaveDeletedMessagesEnabled();
                PremiumSettingsHook.setSaveDeletedMessagesEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
                updateItems();
            } else if (item.id == ROW_DELETED_MESSAGE_MARKER_MODE) {
                showDeletedMessageMarkerModeDialog();
            }
        });
        listView.setOnItemLongClickListener((view, position) -> copyDeepLinkForPosition(position));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();
        applyTargetScroll();

        fragmentView = frameLayout;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateItems();
        applyTargetScroll();
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_PREMIUM_HEADER, LocaleController.getString(R.string.FluffyPremiumSection), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_LOCAL_ANON_STORY_VIEW, LocaleController.getString(R.string.FluffyStoryViewAnonLocal), PremiumSettingsHook.useLocalAnonymousStoryView()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_LOCAL_ANON_STORY_VIEW_INFO, LocaleController.getString(R.string.FluffyStoryViewAnonLocalInfo), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_LOCAL_MESSAGE_FAKE_EDIT, LocaleController.getString(R.string.FluffyLocalMessageFakeEditEnabled), PremiumSettingsHook.isLocalMessageFakeEditEnabled()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_LOCAL_MESSAGE_FAKE_EDIT_INFO, LocaleController.getString(R.string.FluffyLocalMessageFakeEditEnabledInfo), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_LOCAL_MESSAGE_HISTORY, LocaleController.getString(R.string.FluffyLocalMessageHistoryEnabled), PremiumSettingsHook.isLocalMessageHistoryEnabled()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_LOCAL_MESSAGE_HISTORY_INFO, LocaleController.getString(R.string.FluffyLocalMessageHistoryEnabledInfo), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_SAVE_DELETED_MESSAGES, LocaleController.getString(R.string.FluffySaveDeletedMessagesEnabled), PremiumSettingsHook.isSaveDeletedMessagesEnabled()));
        items.add(new ItemInner(VIEW_TYPE_SETTING, ROW_DELETED_MESSAGE_MARKER_MODE, LocaleController.getString(R.string.FluffyDeletedMessageMarkerMode), false, getDeletedMessageMarkerModeValue()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_SAVE_DELETED_MESSAGES_INFO, LocaleController.getString(R.string.FluffySaveDeletedMessagesEnabledInfo), false));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private boolean copyDeepLinkForPosition(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        ItemInner item = items.get(position);
        String link;
        if (item.id == ROW_LOCAL_ANON_STORY_VIEW || item.id == ROW_LOCAL_ANON_STORY_VIEW_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("premium", "local-anon-story-view");
        } else if (item.id == ROW_LOCAL_MESSAGE_FAKE_EDIT || item.id == ROW_LOCAL_MESSAGE_FAKE_EDIT_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("premium", "local-message-fake-edit");
        } else if (item.id == ROW_LOCAL_MESSAGE_HISTORY || item.id == ROW_LOCAL_MESSAGE_HISTORY_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("premium", "local-message-history");
        } else if (item.id == ROW_SAVE_DELETED_MESSAGES || item.id == ROW_SAVE_DELETED_MESSAGES_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("premium", "save-deleted-messages");
        } else if (item.id == ROW_DELETED_MESSAGE_MARKER_MODE) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("premium", "deleted-message-marker");
        } else {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("premium");
        }
        return FluffySettingsDeepLinkPatch.copyLink(this, link);
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
        if ("local-anon-story-view".equals(target)) {
            return ROW_LOCAL_ANON_STORY_VIEW;
        }
        if ("local-message-fake-edit".equals(target)) {
            return ROW_LOCAL_MESSAGE_FAKE_EDIT;
        }
        if ("local-message-history".equals(target)) {
            return ROW_LOCAL_MESSAGE_HISTORY;
        }
        if ("save-deleted-messages".equals(target)) {
            return ROW_SAVE_DELETED_MESSAGES;
        }
        if ("deleted-message-marker".equals(target)) {
            return ROW_DELETED_MESSAGE_MARKER_MODE;
        }
        return -1;
    }

    private CharSequence getDeletedMessageMarkerModeValue() {
        return PremiumSettingsHook.getDeletedMessageMarkerMode() == PremiumSettingsPatch.DELETED_MESSAGE_MARKER_MODE_TEXT
                ? LocaleController.getString(R.string.FluffyDeletedMessageMarkerModeText)
                : LocaleController.getString(R.string.FluffyDeletedMessageMarkerModeIcon);
    }

    private void showDeletedMessageMarkerModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[]{
                LocaleController.getString(R.string.FluffyDeletedMessageMarkerModeText),
                LocaleController.getString(R.string.FluffyDeletedMessageMarkerModeIcon)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyDeletedMessageMarkerMode));
        builder.setItems(items, (dialog, which) -> {
            int mode = which == 0 ? PremiumSettingsPatch.DELETED_MESSAGE_MARKER_MODE_TEXT : PremiumSettingsPatch.DELETED_MESSAGE_MARKER_MODE_ICON;
            PremiumSettingsHook.setDeletedMessageMarkerMode(mode);
            updateItems();
        });
        showDialog(builder.create());
    }

    private int findItemIndexById(int id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final boolean checked;
        final CharSequence value;

        ItemInner(int viewType, int id, CharSequence text, boolean checked) {
            this(viewType, id, text, checked, null);
        }

        ItemInner(int viewType, int id, CharSequence text, boolean checked, CharSequence value) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.checked = checked;
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
            int type = holder.getItemViewType();
            return type == VIEW_TYPE_CHECK || type == VIEW_TYPE_SETTING;
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
            } else if (viewType == VIEW_TYPE_SETTING) {
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
            } else if (holder.getItemViewType() == VIEW_TYPE_CHECK) {
                ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, item.checked, false);
            } else if (holder.getItemViewType() == VIEW_TYPE_SETTING) {
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, item.value, false);
            } else {
                ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
            }
        }
    }
}
