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
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.sync.FluffySyncManager;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;

import java.util.ArrayList;

public class FluffyCloudSettingsActivity extends BaseFragment {

    private static final String ARG_TARGET = "fluffy_cloud_target";

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;
    private static final int VIEW_TYPE_CHECK = 3;

    private static final int ROW_CLOUD_HEADER = 0;
    private static final int ROW_SYNC_ROLE = 1;
    private static final int ROW_SYNC_REFRESH_ACCESS = 2;
    private static final int ROW_SYNC_LOG = 3;
    private static final int ROW_SYNC_AUTO = 4;
    private static final int ROW_SYNC_PUSH = 5;
    private static final int ROW_SYNC_PULL = 6;
    private static final int ROW_SYNC_INFO = 7;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();
    private final Runnable cooldownTicker = new Runnable() {
        @Override
        public void run() {
            if (listView == null) {
                return;
            }
            if (FluffySyncManager.getInstance().isCooldownActive()) {
                updateItems();
                org.telegram.messenger.AndroidUtilities.runOnUIThread(this, 1000L);
            } else {
                updateItems();
            }
        }
    };

    public FluffyCloudSettingsActivity() {
        super();
    }

    public FluffyCloudSettingsActivity(Bundle args) {
        super(args);
    }

    public static FluffyCloudSettingsActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffyCloudSettingsActivity(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyCloud));
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
            if (item.id == ROW_SYNC_AUTO) {
                boolean enabled = !FluffySyncManager.getInstance().isAutoSyncEnabled();
                FluffySyncManager.getInstance().setAutoSyncEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
                updateItems();
            } else if (item.id == ROW_SYNC_REFRESH_ACCESS) {
                refreshAccessRole(true);
            } else if (item.id == ROW_SYNC_LOG) {
                presentFragment(new FluffyCloudActivityLogActivity());
            } else if (item.id == ROW_SYNC_PUSH) {
                pushFluffySettings();
            } else if (item.id == ROW_SYNC_PULL) {
                pullFluffySettings();
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
        ensureConsentAndRefresh();
        scheduleCooldownTicker();
    }

    @Override
    public void onPause() {
        super.onPause();
        org.telegram.messenger.AndroidUtilities.cancelRunOnUIThread(cooldownTicker);
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_CLOUD_HEADER,
                LocaleController.getString(R.string.FluffyCloudSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SYNC_ROLE,
                LocaleController.getString(R.string.FluffySyncRole), FluffySyncManager.getInstance().getRoleText()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SYNC_REFRESH_ACCESS,
                LocaleController.getString(R.string.FluffySyncRefreshAccess), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SYNC_LOG,
                LocaleController.getString(R.string.FluffySyncActivity), null));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_SYNC_AUTO,
                LocaleController.getString(R.string.FluffySyncAutoSync), FluffySyncManager.getInstance().isAutoSyncEnabled()));
        long cooldownSeconds = FluffySyncManager.getInstance().getCooldownRemainingSeconds();
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SYNC_PUSH,
                getActionLabel(R.string.FluffySyncPushAppearance, cooldownSeconds), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SYNC_PULL,
                getActionLabel(R.string.FluffySyncPullAppearance, cooldownSeconds), null));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_SYNC_INFO,
                getSyncInfoText(cooldownSeconds), null));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private CharSequence getSyncInfoText(long cooldownSeconds) {
        String base = FluffySyncManager.getInstance().getStatusText() + "\n\n"
                + LocaleController.getString(R.string.FluffySyncInfoInactive);
        if (cooldownSeconds <= 0L) {
            return base;
        }
        return base + "\n\n" + LocaleController.formatString("FluffySyncCooldownActive", R.string.FluffySyncCooldownActive, cooldownSeconds);
    }

    private CharSequence getActionLabel(int resId, long cooldownSeconds) {
        String label = LocaleController.getString(resId);
        if (cooldownSeconds <= 0L) {
            return label;
        }
        return label + " (" + cooldownSeconds + "s)";
    }

    private void scheduleCooldownTicker() {
        org.telegram.messenger.AndroidUtilities.cancelRunOnUIThread(cooldownTicker);
        if (FluffySyncManager.getInstance().isCooldownActive()) {
            org.telegram.messenger.AndroidUtilities.runOnUIThread(cooldownTicker, 1000L);
        }
    }

    private void ensureConsentAndRefresh() {
        if (!FluffySyncManager.getInstance().hasNetworkConsent()) {
            showConsentDialog();
            return;
        }
        refreshAccessRole(false);
    }

    private void showConsentDialog() {
        if (getParentActivity() == null) {
            finishFragment();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffySyncConsentTitle));
        builder.setMessage(LocaleController.getString(R.string.FluffySyncConsentMessage));
        builder.setPositiveButton(LocaleController.getString(R.string.FluffySyncConsentAgree), (dialog, which) -> {
            FluffySyncManager.getInstance().setNetworkConsent(true);
            updateItems();
            refreshAccessRole(false);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.FluffySyncConsentExit), (dialog, which) -> finishFragment());
        builder.setOnCancelListener(dialog -> finishFragment());
        showDialog(builder.create());
    }

    private void pushFluffySettings() {
        BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffySyncStatusSyncing))
                .show();
        FluffySyncManager.getInstance().pushAppearanceSettings(this, (success, errorMessage) -> {
            updateItems();
            scheduleCooldownTicker();
            if (success) {
                BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.FluffySyncPushSuccess)).show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(formatSyncError(errorMessage)).show();
            }
        });
    }

    private void refreshAccessRole(boolean showBulletin) {
        FluffySyncManager.getInstance().loadActivityLog(this, 1, (items, role, errorMessage) -> {
            updateItems();
            if (!showBulletin) {
                return;
            }
            if (TextUtils.isEmpty(errorMessage)) {
                BulletinFactory.of(this).createSuccessBulletin(
                        LocaleController.formatString("FluffySyncAccessUpdated", R.string.FluffySyncAccessUpdated, role)
                ).show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(formatSyncError(errorMessage)).show();
            }
        });
    }

    private void pullFluffySettings() {
        BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffySyncStatusSyncing))
                .show();
        FluffySyncManager.getInstance().pullAppearanceSettings(this, (success, errorMessage) -> {
            updateItems();
            scheduleCooldownTicker();
            if (success) {
                BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.FluffySyncPullSuccess)).show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(formatSyncError(errorMessage)).show();
            }
        });
    }

    private CharSequence formatSyncError(String errorMessage) {
        if (TextUtils.isEmpty(errorMessage)) {
            return LocaleController.getString(R.string.FluffySyncFailed);
        }
        return LocaleController.formatString("FluffySyncStatusError", R.string.FluffySyncStatusError, errorMessage);
    }

    private boolean copyDeepLinkForPosition(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        ItemInner item = items.get(position);
        String link;
        if (item.id == ROW_SYNC_ROLE) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "role");
        } else if (item.id == ROW_SYNC_REFRESH_ACCESS) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "refresh-access");
        } else if (item.id == ROW_SYNC_LOG) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "activity");
        } else if (item.id == ROW_SYNC_AUTO || item.id == ROW_CLOUD_HEADER || item.id == ROW_SYNC_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "sync-auto");
        } else if (item.id == ROW_SYNC_PUSH) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "sync-push");
        } else if (item.id == ROW_SYNC_PULL) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "sync-pull");
        } else {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud");
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
        switch (target) {
            case "role":
                return ROW_SYNC_ROLE;
            case "refresh-access":
                return ROW_SYNC_REFRESH_ACCESS;
            case "activity":
                return ROW_SYNC_LOG;
            case "sync-auto":
                return ROW_SYNC_AUTO;
            case "sync-push":
                return ROW_SYNC_PUSH;
            case "sync-pull":
                return ROW_SYNC_PULL;
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

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final Object value;

        ItemInner(int viewType, int id, CharSequence text, Object value) {
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
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= items.size()) {
                return false;
            }
            ItemInner item = items.get(position);
            if ((item.id == ROW_SYNC_PUSH || item.id == ROW_SYNC_PULL) && FluffySyncManager.getInstance().isCooldownActive()) {
                return false;
            }
            int type = holder.getItemViewType();
            return type == VIEW_TYPE_TEXT || type == VIEW_TYPE_CHECK;
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
                view = new TextInfoPrivacyCell(parent.getContext());
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            if (holder.itemView instanceof HeaderCell) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.itemView instanceof TextCheckCell) {
                ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, Boolean.TRUE.equals(item.value), false);
            } else if (holder.itemView instanceof TextSettingsCell) {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (item.value instanceof CharSequence && !TextUtils.isEmpty((CharSequence) item.value)) {
                    cell.setTextAndValue(item.text, (CharSequence) item.value, false);
                } else {
                    cell.setText(item.text, false);
                }
            } else if (holder.itemView instanceof TextInfoPrivacyCell) {
                ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
            }
        }
    }
}
