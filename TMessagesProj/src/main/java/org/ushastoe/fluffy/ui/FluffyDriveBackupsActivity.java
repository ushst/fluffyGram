package org.ushastoe.fluffy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.identity.AuthorizationResult;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.sync.FluffyDriveSyncManager;

import java.util.ArrayList;
import java.util.List;

public class FluffyDriveBackupsActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private static final int ROW_HEADER = 0;
    private static final int ROW_REFRESH = 1;
    private static final int ROW_INFO = 2;
    private static final int BACKUP_ROW_BASE = 100;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<RowItem> items = new ArrayList<>();
    private final ArrayList<FluffyDriveSyncManager.BackupEntry> backups = new ArrayList<>();
    private AuthorizationResult currentAuthorizationResult;
    private AuthorizedAction pendingAuthorizedAction;
    private boolean loading;
    private boolean actionBusy;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyDriveManageBackups));
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
            RowItem item = items.get(position);
            if (item.id == ROW_REFRESH) {
                loadBackups(true);
            } else if (item.backupEntry != null) {
                showBackupActions(item.backupEntry);
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = frameLayout;
        updateItems();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadBackups(false);
    }

    private void loadBackups(boolean forced) {
        if (loading || actionBusy) {
            return;
        }
        loading = true;
        updateItems();
        authorizeAndRun(new AuthorizedAction() {
            @Override
            public void run(@NonNull AuthorizationResult result) {
                currentAuthorizationResult = result;
                FluffyDriveSyncManager.getInstance().listBackups(getParentActivity(), result, (entries, errorMessage) -> {
                    loading = false;
                    if (!TextUtils.isEmpty(errorMessage)) {
                        backups.clear();
                        updateItems();
                        BulletinFactory.of(FluffyDriveBackupsActivity.this)
                                .createErrorBulletin(errorMessage)
                                .show();
                        return;
                    }
                    backups.clear();
                    if (entries != null) {
                        backups.addAll(entries);
                    }
                    updateItems();
                    if (forced) {
                        BulletinFactory.of(FluffyDriveBackupsActivity.this)
                                .createSuccessBulletin(LocaleController.getString(R.string.FluffyDriveBackupsRefreshed))
                                .show();
                    }
                });
            }
        });
    }

    private void showBackupActions(@NonNull FluffyDriveSyncManager.BackupEntry backupEntry) {
        if (getParentActivity() == null || actionBusy) {
            return;
        }
        CharSequence[] items = new CharSequence[]{
                LocaleController.getString(R.string.FluffyDriveRestore),
                LocaleController.getString(R.string.FluffyDriveDeleteBackup)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(formatBackupTitle(backupEntry));
        builder.setItems(items, (dialog, which) -> {
            if (which == 0) {
                restoreBackup(backupEntry);
            } else if (which == 1) {
                confirmDeleteBackup(backupEntry);
            }
        });
        showDialog(builder.create());
    }

    private void restoreBackup(@NonNull FluffyDriveSyncManager.BackupEntry backupEntry) {
        actionBusy = true;
        updateItems();
        BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffyDriveRestoreProgress))
                .show();
        authorizeAndRun(new AuthorizedAction() {
            @Override
            public void run(@NonNull AuthorizationResult result) {
                currentAuthorizationResult = result;
                FluffyDriveSyncManager.getInstance().restoreConfig(getParentActivity(), result, backupEntry.id, (success, errorMessage) -> {
                    actionBusy = false;
                    updateItems();
                    if (success) {
                        if (getParentLayout() != null) {
                            getParentLayout().rebuildAllFragmentViews(true, true);
                        }
                        BulletinFactory.of(FluffyDriveBackupsActivity.this)
                                .createSuccessBulletin(LocaleController.getString(R.string.FluffyDriveRestoreDone))
                                .show();
                    } else {
                        BulletinFactory.of(FluffyDriveBackupsActivity.this)
                                .createErrorBulletin(TextUtils.isEmpty(errorMessage)
                                        ? LocaleController.getString(R.string.FluffyDriveRequestFailed)
                                        : errorMessage)
                                .show();
                    }
                });
            }
        });
    }

    private void confirmDeleteBackup(@NonNull FluffyDriveSyncManager.BackupEntry backupEntry) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyDriveDeleteBackup));
        builder.setMessage(LocaleController.getString(R.string.FluffyDriveDeleteBackupConfirm));
        builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> deleteBackup(backupEntry));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void deleteBackup(@NonNull FluffyDriveSyncManager.BackupEntry backupEntry) {
        actionBusy = true;
        updateItems();
        authorizeAndRun(new AuthorizedAction() {
            @Override
            public void run(@NonNull AuthorizationResult result) {
                currentAuthorizationResult = result;
                FluffyDriveSyncManager.getInstance().deleteBackup(getParentActivity(), result, backupEntry.id, (success, errorMessage) -> {
                    actionBusy = false;
                    if (success) {
                        backups.remove(backupEntry);
                        updateItems();
                        BulletinFactory.of(FluffyDriveBackupsActivity.this)
                                .createSuccessBulletin(LocaleController.getString(R.string.FluffyDriveBackupDeleted))
                                .show();
                    } else {
                        updateItems();
                        BulletinFactory.of(FluffyDriveBackupsActivity.this)
                                .createErrorBulletin(TextUtils.isEmpty(errorMessage)
                                        ? LocaleController.getString(R.string.FluffyDriveRequestFailed)
                                        : errorMessage)
                                .show();
                    }
                });
            }
        });
    }

    private void authorizeAndRun(@NonNull AuthorizedAction action) {
        if (getParentActivity() == null) {
            loading = false;
            actionBusy = false;
            updateItems();
            return;
        }
        if (currentAuthorizationResult != null && !TextUtils.isEmpty(currentAuthorizationResult.getAccessToken())) {
            action.run(currentAuthorizationResult);
            return;
        }
        pendingAuthorizedAction = action;
        FluffyDriveSyncManager.getInstance().authorize(this, new FluffyDriveSyncManager.AuthorizationCallback() {
            @Override
            public void onAuthorized(@NonNull AuthorizationResult result) {
                currentAuthorizationResult = result;
                pendingAuthorizedAction = null;
                action.run(result);
            }

            @Override
            public void onError(String errorMessage) {
                pendingAuthorizedAction = null;
                loading = false;
                actionBusy = false;
                updateItems();
                BulletinFactory.of(FluffyDriveBackupsActivity.this)
                        .createErrorBulletin(TextUtils.isEmpty(errorMessage)
                                ? LocaleController.getString(R.string.FluffyDriveAuthorizationFailed)
                                : errorMessage)
                        .show();
            }
        });
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode == FluffyDriveSyncManager.REQUEST_CODE_AUTHORIZE_DRIVE) {
            if (resultCode != Activity.RESULT_OK || data == null) {
                loading = false;
                actionBusy = false;
                updateItems();
                BulletinFactory.of(this)
                        .createErrorBulletin(LocaleController.getString(R.string.FluffyDriveAuthorizationCancelled))
                        .show();
                return;
            }
            FluffyDriveSyncManager.getInstance().handleAuthorizationResult(getParentActivity(), data, new FluffyDriveSyncManager.AuthorizationCallback() {
                @Override
                public void onAuthorized(@NonNull AuthorizationResult result) {
                    currentAuthorizationResult = result;
                    AuthorizedAction action = pendingAuthorizedAction;
                    pendingAuthorizedAction = null;
                    if (action != null) {
                        action.run(result);
                        return;
                    }
                    updateItems();
                }

                @Override
                public void onError(String errorMessage) {
                    pendingAuthorizedAction = null;
                    loading = false;
                    actionBusy = false;
                    updateItems();
                    BulletinFactory.of(FluffyDriveBackupsActivity.this)
                            .createErrorBulletin(TextUtils.isEmpty(errorMessage)
                                    ? LocaleController.getString(R.string.FluffyDriveAuthorizationFailed)
                                    : errorMessage)
                            .show();
                }
            });
            return;
        }
        super.onActivityResultFragment(requestCode, resultCode, data);
    }

    private void updateItems() {
        items.clear();
        items.add(new RowItem(VIEW_TYPE_HEADER, ROW_HEADER, LocaleController.getString(R.string.FluffyDriveManageBackups), null, null));
        items.add(new RowItem(VIEW_TYPE_TEXT, ROW_REFRESH, LocaleController.getString(R.string.FluffyDriveRefreshBackups), null, null));
        if (loading) {
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO, LocaleController.getString(R.string.FluffyDriveLoadingBackups), null, null));
        } else if (backups.isEmpty()) {
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO, LocaleController.getString(R.string.FluffyDriveNoBackups), null, null));
        } else {
            for (int i = 0; i < backups.size(); i++) {
                FluffyDriveSyncManager.BackupEntry backupEntry = backups.get(i);
                items.add(new RowItem(VIEW_TYPE_TEXT, BACKUP_ROW_BASE + i,
                        formatBackupTitle(backupEntry), formatBackupValue(backupEntry), backupEntry));
            }
        }
        items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO + 1, LocaleController.getString(R.string.FluffyDriveManageBackupsInfo), null, null));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private CharSequence formatBackupTitle(@NonNull FluffyDriveSyncManager.BackupEntry backupEntry) {
        if (backupEntry.legacy || backupEntry.backupAt <= 0L) {
            return LocaleController.getString(R.string.FluffyDriveLegacyBackup);
        }
        String date = LocaleController.getInstance().getFormatterYearMax().format(backupEntry.backupAt);
        String time = LocaleController.getInstance().getFormatterDay().format(backupEntry.backupAt);
        return date + " " + time;
    }

    private CharSequence formatBackupValue(@NonNull FluffyDriveSyncManager.BackupEntry backupEntry) {
        return AndroidUtilities.formatFileSize(Math.max(backupEntry.sizeBytes, 0L));
    }

    private interface AuthorizedAction {
        void run(@NonNull AuthorizationResult result);
    }

    private static class RowItem {
        final int viewType;
        final int id;
        final CharSequence text;
        final CharSequence value;
        final FluffyDriveSyncManager.BackupEntry backupEntry;

        RowItem(int viewType, int id, CharSequence text, CharSequence value, FluffyDriveSyncManager.BackupEntry backupEntry) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.value = value;
            this.backupEntry = backupEntry;
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
            RowItem item = items.get(position);
            if (loading || actionBusy) {
                return false;
            }
            return item.viewType == VIEW_TYPE_TEXT;
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
            RowItem item = items.get(position);
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
