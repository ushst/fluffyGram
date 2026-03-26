package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.os.Bundle;
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
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.MessageActionsHook;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;

import java.util.ArrayList;

/**
 * Message Actions & Interaction Functions Settings.
 *
 * This activity manages toggles for all additional message context menu features.
 * New message action functions must include an enable/disable toggle here.
 *
 * See: .github/docs/MESSAGE_ACTIONS_STANDARD.md for implementation guidelines.
 */
public class FluffyMessageActionsActivity extends BaseFragment {

    private static final String ARG_TARGET = "fluffy_message_actions_target";

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private static final int ROW_ACTIONS_HEADER = 0;
    private static final int ROW_ACTIONS_INFO = 1;
    private static final int ROW_MESSAGE_DETAILS = 2;
    private static final int ROW_MESSAGE_TRANSLIT = 3;
    private static final int ROW_LOCAL_MESSAGE_HISTORY = 4;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    public FluffyMessageActionsActivity() {
        super();
    }

    public FluffyMessageActionsActivity(Bundle args) {
        super(args);
    }

    public static FluffyMessageActionsActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffyMessageActionsActivity(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyMessageActions));
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
            if (item.id == ROW_MESSAGE_DETAILS) {
                boolean enabled = !MessageActionsHook.isMessageDetailsEnabled();
                MessageActionsHook.setMessageDetailsEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_MESSAGE_TRANSLIT) {
                boolean enabled = !MessageActionsHook.isMessageTranslitEnabled();
                MessageActionsHook.setMessageTranslitEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_LOCAL_MESSAGE_HISTORY) {
                boolean enabled = !MessageActionsHook.isLocalMessageHistoryEnabled();
                MessageActionsHook.setLocalMessageHistoryEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
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
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_ACTIONS_HEADER, LocaleController.getString(R.string.FluffyInteractionFunctions), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_MESSAGE_DETAILS, LocaleController.getString(R.string.MessageDetails), MessageActionsHook.isMessageDetailsEnabled()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_MESSAGE_TRANSLIT, LocaleController.getString(R.string.FluffyMessageTranslit), MessageActionsHook.isMessageTranslitEnabled()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_LOCAL_MESSAGE_HISTORY, LocaleController.getString(R.string.FluffyLocalMessageHistoryAction), MessageActionsHook.isLocalMessageHistoryEnabled()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_ACTIONS_INFO, LocaleController.getString(R.string.FluffyMessageActionsHelp), false));
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateItems();
        applyTargetScroll();
    }

    private boolean copyDeepLinkForPosition(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        ItemInner item = items.get(position);
        if (item.id == ROW_MESSAGE_DETAILS) {
            return FluffySettingsDeepLinkPatch.copyLink(this, FluffySettingsDeepLinkPatch.buildSettingsLink("message_actions", "details"));
        }
        if (item.id == ROW_MESSAGE_TRANSLIT) {
            return FluffySettingsDeepLinkPatch.copyLink(this, FluffySettingsDeepLinkPatch.buildSettingsLink("message_actions", "translit"));
        }
        if (item.id == ROW_LOCAL_MESSAGE_HISTORY) {
            return FluffySettingsDeepLinkPatch.copyLink(this, FluffySettingsDeepLinkPatch.buildSettingsLink("message_actions", "local-history"));
        }
        return FluffySettingsDeepLinkPatch.copyLink(this, FluffySettingsDeepLinkPatch.buildSettingsLink("message_actions"));
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
        if ("actions".equals(target) || "message_actions".equals(target)) {
            return ROW_ACTIONS_HEADER;
        }
        if ("details".equals(target)) {
            return ROW_MESSAGE_DETAILS;
        }
        if ("translit".equals(target)) {
            return ROW_MESSAGE_TRANSLIT;
        }
        if ("local-history".equals(target)) {
            return ROW_LOCAL_MESSAGE_HISTORY;
        }
        return -1;
    }

    private int findItemIndexById(int id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    // ============================================================================
    // Adapter
    // ============================================================================

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_TYPE_CHECK;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    view = new HeaderCell(parent.getContext());
                    break;
                case VIEW_TYPE_CHECK:
                    view = new TextCheckCell(parent.getContext());
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_INFO:
                    view = new TextInfoPrivacyCell(parent.getContext());
                    break;
                default:
                    view = new TextInfoPrivacyCell(parent.getContext());
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            switch (item.viewType) {
                case VIEW_TYPE_HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(item.title);
                    break;
                case VIEW_TYPE_CHECK:
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    checkCell.setTextAndCheck(item.title, item.checked, true);
                    break;
                case VIEW_TYPE_INFO:
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    infoCell.setText(item.title);
                    break;
            }
        }
    }

    // ============================================================================
    // ItemInner
    // ============================================================================

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence title;
        final boolean checked;

        ItemInner(int viewType, int id, CharSequence title, boolean checked) {
            this.viewType = viewType;
            this.id = id;
            this.title = title;
            this.checked = checked;
        }
    }
}
