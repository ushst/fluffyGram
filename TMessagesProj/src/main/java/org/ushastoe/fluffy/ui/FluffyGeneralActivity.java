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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.ChatFirstMessageHook;
import org.ushastoe.fluffy.hooks.InAppCameraSettingsHook;
import org.ushastoe.fluffy.hooks.UnlimitedPinsHook;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;

import java.util.ArrayList;

public class FluffyGeneralActivity extends BaseFragment {

    private static final String ARG_TARGET = "fluffy_general_target";

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private static final int ROW_GENERAL_HEADER = 0;
    private static final int ROW_IN_APP_CAMERA = 1;
    private static final int ROW_IN_APP_CAMERA_INFO = 2;
    private static final int ROW_CHAT_FIRST_MESSAGE = 3;
    private static final int ROW_CHAT_FIRST_MESSAGE_INFO = 4;
    private static final int ROW_UNLIMITED_PINS = 5;
    private static final int ROW_UNLIMITED_PINS_INFO = 6;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    public FluffyGeneralActivity() {
        super();
    }

    public FluffyGeneralActivity(Bundle args) {
        super(args);
    }

    public static FluffyGeneralActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffyGeneralActivity(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyGeneral));
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
            if (item.id == ROW_IN_APP_CAMERA) {
                boolean enabled = !InAppCameraSettingsHook.isEnabled();
                InAppCameraSettingsHook.setEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_CHAT_FIRST_MESSAGE) {
                boolean enabled = !ChatFirstMessageHook.isEnabled();
                ChatFirstMessageHook.setEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_UNLIMITED_PINS) {
                boolean enabled = !UnlimitedPinsHook.isEnabled();
                UnlimitedPinsHook.setEnabled(enabled);
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

    @Override
    public void onResume() {
        super.onResume();
        updateItems();
        applyTargetScroll();
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_GENERAL_HEADER, LocaleController.getString(R.string.FluffyGeneralSection), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_IN_APP_CAMERA, LocaleController.getString(R.string.FluffyInAppCamera), InAppCameraSettingsHook.isEnabled()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_IN_APP_CAMERA_INFO, LocaleController.getString(R.string.FluffyInAppCameraInfo), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_CHAT_FIRST_MESSAGE, LocaleController.getString(R.string.FluffyGoToFirstMessage), ChatFirstMessageHook.isEnabled()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_CHAT_FIRST_MESSAGE_INFO, LocaleController.getString(R.string.FluffyGoToFirstMessageInfo), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_UNLIMITED_PINS, LocaleController.getString(R.string.FluffyUnlimitedUnarchivedPins), UnlimitedPinsHook.isEnabled()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_UNLIMITED_PINS_INFO, LocaleController.getString(R.string.FluffyUnlimitedUnarchivedPinsInfo), false));
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
        if (item.id == ROW_IN_APP_CAMERA || item.id == ROW_IN_APP_CAMERA_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("general", "in-app-camera");
        } else if (item.id == ROW_CHAT_FIRST_MESSAGE || item.id == ROW_CHAT_FIRST_MESSAGE_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("general", "go-to-first-message");
        } else if (item.id == ROW_UNLIMITED_PINS || item.id == ROW_UNLIMITED_PINS_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("general", "unlimited-unarchived-pins");
        } else {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("general");
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
        if ("in-app-camera".equals(target)) {
            return ROW_IN_APP_CAMERA;
        }
        if ("go-to-first-message".equals(target)) {
            return ROW_CHAT_FIRST_MESSAGE;
        }
        if ("unlimited-unarchived-pins".equals(target)) {
            return ROW_UNLIMITED_PINS;
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
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(parent.getContext());
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
            } else {
                ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
            }
        }
    }
}
