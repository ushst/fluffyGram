package org.ushastoe.fluffy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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
import org.ushastoe.fluffy.sync.FluffyDriveSyncManager;
import org.ushastoe.fluffy.sync.FluffySyncManager;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;

import java.util.ArrayList;
import java.util.List;

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
    private static final int ROW_DRIVE_HEADER = 8;
    private static final int ROW_DRIVE_STATUS = 9;
    private static final int ROW_DRIVE_CONNECT = 10;
    private static final int ROW_DRIVE_BACKUP = 11;
    private static final int ROW_DRIVE_RESTORE = 12;
    private static final int ROW_DRIVE_MANAGE = 13;
    private static final int ROW_DRIVE_HISTORY_LIMIT = 14;
    private static final int ROW_DRIVE_INFO = 15;

    private static final int DRIVE_ACTION_CONNECT = 1;
    private static final int DRIVE_ACTION_BACKUP = 2;
    private static final int DRIVE_ACTION_RESTORE = 3;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();
    private int pendingDriveAction;
    private boolean driveBusy;
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
            } else if (item.id == ROW_DRIVE_CONNECT) {
                startDriveAction(DRIVE_ACTION_CONNECT);
            } else if (item.id == ROW_DRIVE_BACKUP) {
                startDriveAction(DRIVE_ACTION_BACKUP);
            } else if (item.id == ROW_DRIVE_RESTORE) {
                startDriveAction(DRIVE_ACTION_RESTORE);
            } else if (item.id == ROW_DRIVE_MANAGE) {
                presentFragment(new FluffyDriveBackupsActivity());
            } else if (item.id == ROW_DRIVE_HISTORY_LIMIT) {
                showDriveHistoryLimitDialog();
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
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_DRIVE_HEADER,
                LocaleController.getString(R.string.FluffyDriveSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DRIVE_STATUS,
                LocaleController.getString(R.string.FluffyDriveStatus),
                FluffyDriveSyncManager.getInstance().getStatusText(getParentActivity())));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DRIVE_CONNECT,
                LocaleController.getString(R.string.FluffyDriveConnect), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DRIVE_BACKUP,
                LocaleController.getString(R.string.FluffyDriveBackupNow), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DRIVE_RESTORE,
                LocaleController.getString(R.string.FluffyDriveRestore), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DRIVE_MANAGE,
                LocaleController.getString(R.string.FluffyDriveManageBackups), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DRIVE_HISTORY_LIMIT,
                LocaleController.getString(R.string.FluffyDriveHistoryLimit),
                String.valueOf(FluffyDriveSyncManager.getInstance().getBackupHistoryLimit(getParentActivity()))));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_DRIVE_INFO,
                LocaleController.getString(R.string.FluffyDriveInfo), null));
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
        if (FluffySyncManager.getInstance().hasPendingSync()) {
            base = base + "\n\n" + LocaleController.getString(R.string.FluffySyncQueued);
        }
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
        updateItems();
        BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffySyncStatusSyncing))
                .show();
        FluffySyncManager.getInstance().pushAppearanceSettings(this, (success, errorMessage) -> {
            updateItems();
            scheduleCooldownTicker();
            if (success) {
                BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.FluffySyncPushSuccess)).show();
            } else if (isQueuedMessage(errorMessage)) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_download, errorMessage).show();
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
        updateItems();
        BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffySyncStatusSyncing))
                .show();
        FluffySyncManager.getInstance().pullAppearanceSettings(this, (success, errorMessage) -> {
            updateItems();
            scheduleCooldownTicker();
            if (success) {
                BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.FluffySyncPullSuccess)).show();
            } else if (isQueuedMessage(errorMessage)) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.ic_download, errorMessage).show();
            } else {
                BulletinFactory.of(this).createErrorBulletin(formatSyncError(errorMessage)).show();
            }
        });
    }

    private void startDriveAction(int action) {
        if (driveBusy) {
            return;
        }
        pendingDriveAction = action;
        driveBusy = true;
        updateItems();
        BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffyDriveAuthorizing))
                .show();
        FluffyDriveSyncManager.getInstance().authorize(this, new FluffyDriveSyncManager.AuthorizationCallback() {
            @Override
            public void onAuthorized(@NonNull com.google.android.gms.auth.api.identity.AuthorizationResult result) {
                onDriveAuthorizationReady(result);
            }

            @Override
            public void onError(String errorMessage) {
                driveBusy = false;
                updateItems();
                BulletinFactory.of(FluffyCloudSettingsActivity.this)
                        .createErrorBulletin(formatDriveError(errorMessage))
                        .show();
            }
        });
    }

    private void onDriveAuthorizationReady(@NonNull com.google.android.gms.auth.api.identity.AuthorizationResult result) {
        if (pendingDriveAction == DRIVE_ACTION_CONNECT) {
            driveBusy = false;
            updateItems();
            BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.FluffyDriveConnected)).show();
            return;
        }
        if (pendingDriveAction == DRIVE_ACTION_BACKUP) {
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffyDriveBackupProgress))
                    .show();
            FluffyDriveSyncManager.getInstance().backupConfig(getParentActivity(), result, (success, errorMessage) -> {
                driveBusy = false;
                updateItems();
                if (success) {
                    BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.FluffyDriveBackupDone)).show();
                } else {
                    BulletinFactory.of(this).createErrorBulletin(formatDriveError(errorMessage)).show();
                }
            });
            return;
        }
        if (pendingDriveAction == DRIVE_ACTION_RESTORE) {
            FluffyDriveSyncManager.getInstance().listBackups(getParentActivity(), result, (backups, errorMessage) -> {
                driveBusy = false;
                updateItems();
                if (!TextUtils.isEmpty(errorMessage)) {
                    BulletinFactory.of(this).createErrorBulletin(formatDriveError(errorMessage)).show();
                    return;
                }
                if (backups == null || backups.isEmpty()) {
                    BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyDriveFileMissing)).show();
                    return;
                }
                showDriveRestorePicker(result, backups);
            });
            return;
        }
        driveBusy = false;
        updateItems();
    }

    private boolean isQueuedMessage(String errorMessage) {
        return TextUtils.equals(errorMessage, LocaleController.getString(R.string.FluffySyncQueued));
    }

    private CharSequence formatSyncError(String errorMessage) {
        if (TextUtils.isEmpty(errorMessage)) {
            return LocaleController.getString(R.string.FluffySyncFailed);
        }
        return LocaleController.formatString("FluffySyncStatusError", R.string.FluffySyncStatusError, errorMessage);
    }

    private CharSequence formatDriveError(String errorMessage) {
        if (TextUtils.isEmpty(errorMessage)) {
            return LocaleController.getString(R.string.FluffyDriveRequestFailed);
        }
        return errorMessage;
    }

    private void showDriveHistoryLimitDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final int[] limits = new int[]{5, 10, 20, 50};
        CharSequence[] items = new CharSequence[limits.length];
        for (int i = 0; i < limits.length; i++) {
            items[i] = String.valueOf(limits[i]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyDriveHistoryLimit));
        builder.setItems(items, (dialog, which) -> {
            if (which < 0 || which >= limits.length) {
                return;
            }
            FluffyDriveSyncManager.getInstance().setBackupHistoryLimit(getParentActivity(), limits[which]);
            updateItems();
        });
        showDialog(builder.create());
    }

    private void showDriveRestorePicker(@NonNull com.google.android.gms.auth.api.identity.AuthorizationResult result,
                                        @NonNull List<FluffyDriveSyncManager.BackupEntry> backups) {
        if (getParentActivity() == null) {
            return;
        }
        ArrayList<FluffyDriveSyncManager.BackupEntry> entries = new ArrayList<>(backups);
        CharSequence[] labels = new CharSequence[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            labels[i] = formatDriveBackupLabel(entries.get(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyDriveChooseBackup));
        builder.setItems(labels, (dialog, which) -> {
            if (which < 0 || which >= entries.size()) {
                return;
            }
            FluffyDriveSyncManager.BackupEntry selected = entries.get(which);
            driveBusy = true;
            updateItems();
            BulletinFactory.of(this)
                    .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffyDriveRestoreProgress))
                    .show();
            FluffyDriveSyncManager.getInstance().restoreConfig(getParentActivity(), result, selected.id, (success, errorMessage) -> {
                driveBusy = false;
                updateItems();
                if (success) {
                    if (getParentLayout() != null) {
                        getParentLayout().rebuildAllFragmentViews(true, true);
                    }
                    BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.FluffyDriveRestoreDone)).show();
                } else {
                    BulletinFactory.of(this).createErrorBulletin(formatDriveError(errorMessage)).show();
                }
            });
        });
        showDialog(builder.create());
    }

    private CharSequence formatDriveBackupLabel(@NonNull FluffyDriveSyncManager.BackupEntry backup) {
        if (backup.legacy || backup.backupAt <= 0L) {
            return LocaleController.getString(R.string.FluffyDriveLegacyBackup);
        }
        String date = LocaleController.getInstance().getFormatterYearMax().format(backup.backupAt);
        String time = LocaleController.getInstance().getFormatterDay().format(backup.backupAt);
        String size = org.telegram.messenger.AndroidUtilities.formatFileSize(Math.max(backup.sizeBytes, 0L));
        return date + " " + time + " • " + size;
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
        } else if (item.id == ROW_DRIVE_STATUS || item.id == ROW_DRIVE_CONNECT) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "drive-connect");
        } else if (item.id == ROW_DRIVE_BACKUP) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "drive-backup");
        } else if (item.id == ROW_DRIVE_RESTORE) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "drive-restore");
        } else if (item.id == ROW_DRIVE_MANAGE) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "drive-manage");
        } else if (item.id == ROW_DRIVE_HISTORY_LIMIT) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("cloud", "drive-history-limit");
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
            case "drive-connect":
                return ROW_DRIVE_CONNECT;
            case "drive-backup":
                return ROW_DRIVE_BACKUP;
            case "drive-restore":
                return ROW_DRIVE_RESTORE;
            case "drive-manage":
                return ROW_DRIVE_MANAGE;
            case "drive-history-limit":
                return ROW_DRIVE_HISTORY_LIMIT;
            default:
                return -1;
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == FluffyDriveSyncManager.REQUEST_CODE_AUTHORIZE_DRIVE) {
            if (resultCode != Activity.RESULT_OK || data == null) {
                driveBusy = false;
                updateItems();
                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyDriveAuthorizationCancelled)).show();
                return;
            }
            FluffyDriveSyncManager.getInstance().handleAuthorizationResult(getParentActivity(), data, new FluffyDriveSyncManager.AuthorizationCallback() {
                @Override
                public void onAuthorized(@NonNull com.google.android.gms.auth.api.identity.AuthorizationResult result) {
                    onDriveAuthorizationReady(result);
                }

                @Override
                public void onError(String errorMessage) {
                    driveBusy = false;
                    updateItems();
                    BulletinFactory.of(FluffyCloudSettingsActivity.this)
                            .createErrorBulletin(formatDriveError(errorMessage))
                            .show();
                }
            });
            return;
        }
        super.onActivityResultFragment(requestCode, resultCode, data);
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
            if ((item.id == ROW_SYNC_PUSH || item.id == ROW_SYNC_PULL)
                    && (FluffySyncManager.getInstance().isCooldownActive() || FluffySyncManager.getInstance().isSyncing())) {
                return false;
            }
            if (item.id == ROW_DRIVE_STATUS || item.id == ROW_DRIVE_INFO) {
                return false;
            }
            if ((item.id == ROW_DRIVE_CONNECT || item.id == ROW_DRIVE_BACKUP || item.id == ROW_DRIVE_RESTORE) && driveBusy) {
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
