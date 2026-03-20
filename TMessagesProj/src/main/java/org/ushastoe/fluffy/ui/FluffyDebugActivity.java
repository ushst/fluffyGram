package org.ushastoe.fluffy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BetaUpdate;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.PushListenerController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.FluffyLocalLogHook;
import org.ushastoe.fluffy.hooks.UpdateCheckSettingsHook;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;
import org.ushastoe.fluffy.utils.FluffyTextUtils;

import java.io.File;
import java.util.ArrayList;

public class FluffyDebugActivity extends BaseFragment {

    private static final String ARG_TARGET = "fluffy_debug_target";

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;
    private static final int ROW_DEBUG_HEADER = 0;
    private static final int ROW_UPDATE_CHECK_MODE = 1;
    private static final int ROW_CHECK_VERSION = 2;
    private static final int ROW_SHARE_APK = 3;
    private static final int ROW_SAVE_LOG = 4;
    private static final int ROW_SAVE_LOG_INFO = 5;
    private static final int ROW_GOOGLE_CLOUD_HEADER = 6;
    private static final int ROW_GOOGLE_CLOUD_STATUS = 7;
    private static final int ROW_GOOGLE_CLOUD_INFO = 8;
    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    public FluffyDebugActivity() {
        super();
    }

    public FluffyDebugActivity(Bundle args) {
        super(args);
    }

    public static FluffyDebugActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffyDebugActivity(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyDebug));
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
            if (item.id == ROW_SAVE_LOG) {
                FluffyLocalLogHook.onSaveLogClicked(this);
            } else if (item.id == ROW_UPDATE_CHECK_MODE) {
                showUpdateCheckModeDialog();
            } else if (item.id == ROW_CHECK_VERSION) {
                checkForUpdates();
            } else if (item.id == ROW_SHARE_APK) {
                shareCurrentApk();
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
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_DEBUG_HEADER,
                LocaleController.getString(R.string.FluffyDebugSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_UPDATE_CHECK_MODE,
            LocaleController.getString(R.string.FluffyUpdateCheckMode), getUpdateCheckModeValue()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_CHECK_VERSION,
            LocaleController.getString(R.string.FluffyCheckVersion), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SHARE_APK,
                LocaleController.getString(R.string.FluffyShareApk), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SAVE_LOG,
                LocaleController.getString(R.string.FluffySaveLog), null));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_SAVE_LOG_INFO,
                LocaleController.getString(R.string.FluffySaveLogInfo), null));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_GOOGLE_CLOUD_HEADER,
                LocaleController.getString(R.string.FluffyGoogleCloudSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GOOGLE_CLOUD_STATUS,
                LocaleController.getString(R.string.FluffyGoogleCloudStatus), getGoogleCloudStatusValue()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_GOOGLE_CLOUD_INFO,
                getGoogleCloudStatusDetails(), null));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private CharSequence getUpdateCheckModeValue() {
        int mode = UpdateCheckSettingsHook.getAutoCheckMode();
        if (mode == UpdateCheckSettingsHook.AUTO_CHECK_NEVER) {
            return LocaleController.getString(R.string.FluffyUpdateCheckModeNever);
        }
        return LocaleController.getString(R.string.FluffyUpdateCheckModeOnLaunch);
    }

    private void showUpdateCheckModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[] {
                LocaleController.getString(R.string.FluffyUpdateCheckModeNever),
                LocaleController.getString(R.string.FluffyUpdateCheckModeOnLaunch)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyUpdateCheckMode));
        builder.setItems(options, (dialog, which) -> {
            int mode = which == 0 ? UpdateCheckSettingsHook.AUTO_CHECK_NEVER : UpdateCheckSettingsHook.AUTO_CHECK_ON_LAUNCH;
            UpdateCheckSettingsHook.setAutoCheckMode(mode);
            updateItems();
        });
        showDialog(builder.create());
    }

    private String getGoogleCloudStatusValue() {
        boolean hasServices = PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices();
        if (!hasServices) {
            return LocaleController.getString(R.string.FluffyStatusUnavailable);
        }
        if (SharedConfig.pushType != PushListenerController.PUSH_TYPE_FIREBASE) {
            return LocaleController.getString(R.string.FluffyStatusInactive);
        }
        if (!TextUtils.isEmpty(SharedConfig.pushString)) {
            return LocaleController.getString(R.string.FluffyStatusConnected);
        }
        if (TextUtils.equals(SharedConfig.pushStringStatus, "__FIREBASE_FAILED__")) {
            return LocaleController.getString(R.string.FluffyStatusError);
        }
        if (!TextUtils.isEmpty(SharedConfig.pushStringStatus) && SharedConfig.pushStringStatus.contains("GENERATING")) {
            return LocaleController.getString(R.string.FluffyStatusConnecting);
        }
        return LocaleController.getString(R.string.FluffyStatusWaiting);
    }

    private CharSequence getGoogleCloudStatusDetails() {
        boolean hasServices = PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices();
        String pushType;
        if (SharedConfig.pushType == PushListenerController.PUSH_TYPE_FIREBASE) {
            pushType = "Firebase";
        } else if (SharedConfig.pushType == PushListenerController.PUSH_TYPE_HUAWEI) {
            pushType = "Huawei";
        } else {
            pushType = String.valueOf(SharedConfig.pushType);
        }

        String token = TextUtils.isEmpty(SharedConfig.pushString)
                ? LocaleController.getString(R.string.FluffyGoogleCloudTokenMissing)
                : maskToken(SharedConfig.pushString);
        String state = TextUtils.isEmpty(SharedConfig.pushStringStatus)
                ? LocaleController.getString(R.string.FluffyGoogleCloudStateEmpty)
                : SharedConfig.pushStringStatus;

        return String.format(
                java.util.Locale.US,
                "%s: %s\n%s: %s\n%s: %s\n%s: %s",
                LocaleController.getString(R.string.FluffyGoogleCloudServices),
                hasServices ? LocaleController.getString(R.string.FluffyStatusAvailable) : LocaleController.getString(R.string.FluffyStatusUnavailable),
                LocaleController.getString(R.string.FluffyGoogleCloudPushType),
                pushType,
                LocaleController.getString(R.string.FluffyGoogleCloudToken),
                token,
                LocaleController.getString(R.string.FluffyGoogleCloudState),
                state
        );
    }

    private void checkForUpdates() {
        if (getParentActivity() == null) {
            return;
        }
        BulletinFactory.of(this)
                .createSimpleBulletin(R.raw.ic_download, LocaleController.getString(R.string.FluffyCheckingUpdates))
                .show();
        ApplicationLoader.applicationLoaderInstance.checkUpdate(true, () -> AndroidUtilities.runOnUIThread(() -> {
            Activity currentActivity = getParentActivity();
            if (currentActivity == null) {
                return;
            }
            BetaUpdate update = ApplicationLoader.applicationLoaderInstance.getUpdate();
            if (update != null) {
                ApplicationLoader.applicationLoaderInstance.showCustomUpdateAppPopup(currentActivity, update, currentAccount);
            } else {
                BulletinFactory.of(this)
                        .createSuccessBulletin(LocaleController.getString(R.string.FluffyNoUpdatesFound))
                        .show();
            }
                }));
    }

    private void shareCurrentApk() {
        if (getParentActivity() == null) {
            return;
        }
        try {
            File sourceFile = new File(getParentActivity().getApplicationInfo().sourceDir);
            if (!sourceFile.isFile()) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyShareApkFailed)).show();
                return;
            }

            File sharingDir = AndroidUtilities.getSharingDirectory();
            if (!sharingDir.exists() && !sharingDir.mkdirs()) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyShareApkFailed)).show();
                return;
            }

            String versionName = getParentActivity().getPackageManager().getPackageInfo(getParentActivity().getPackageName(), 0).versionName;
            if (TextUtils.isEmpty(versionName)) {
                versionName = "debug";
            }
            String apkName = getParentActivity().getPackageName() + "-" + versionName + ".apk";
            File shareFile = new File(sharingDir, apkName);
            if (!AndroidUtilities.copyFileSafe(sourceFile, shareFile)) {
                BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyShareApkFailed)).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM,
                    FileProvider.getUriForFile(getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", shareFile));
            intent.putExtra(Intent.EXTRA_SUBJECT, apkName);
            intent.putExtra(Intent.EXTRA_TEXT, LocaleController.getString(R.string.FluffyShareApkText));
            startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.FluffyShareApk)), 500);
        } catch (Exception e) {
            FileLog.e(e);
            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyShareApkFailed)).show();
        }
    }

    private String maskToken(String token) {
        if (TextUtils.isEmpty(token) || token.length() <= 8) {
            return token;
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private boolean copyDeepLinkForPosition(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        ItemInner item = items.get(position);
        String link;
        if (item.id == ROW_UPDATE_CHECK_MODE) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("debug", "update-check-mode");
        } else if (item.id == ROW_CHECK_VERSION) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("debug", "check-updates");
        } else if (item.id == ROW_SHARE_APK) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("debug", "share-apk");
        } else if (item.id == ROW_SAVE_LOG || item.id == ROW_SAVE_LOG_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("debug", "save-log");
        } else if (item.id == ROW_GOOGLE_CLOUD_HEADER || item.id == ROW_GOOGLE_CLOUD_STATUS || item.id == ROW_GOOGLE_CLOUD_INFO) {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("debug", "google-cloud");
        } else {
            link = FluffySettingsDeepLinkPatch.buildSettingsLink("debug");
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
            case "update-check-mode":
                return ROW_UPDATE_CHECK_MODE;
            case "check-updates":
                return ROW_CHECK_VERSION;
            case "share-apk":
                return ROW_SHARE_APK;
            case "save-log":
                return ROW_SAVE_LOG;
            case "google-cloud":
                return ROW_GOOGLE_CLOUD_STATUS;
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
            int type = holder.getItemViewType();
            return type == VIEW_TYPE_TEXT;
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
            if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.getItemViewType() == VIEW_TYPE_TEXT) {
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, FluffyTextUtils.truncateParameterValue((CharSequence) item.value), false);
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setFixedSize(0);
                cell.setText(item.text);
            }
        }
    }
}
