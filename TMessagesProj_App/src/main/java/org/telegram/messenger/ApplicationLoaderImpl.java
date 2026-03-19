package org.telegram.messenger;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.IUpdateLayout;
import org.ushastoe.fluffy.hooks.AppFontHook;
import org.ushastoe.fluffy.hooks.CustomUpdateUiHook;
import org.ushastoe.fluffy.hooks.MapsProviderHook;
import org.ushastoe.fluffy.hooks.SyncSettingsHook;
import org.ushastoe.fluffy.updates.FluffyCustomUpdateManager;
import org.telegram.messenger.regular.BuildConfig;
import org.telegram.tgnet.TLRPC;

public class ApplicationLoaderImpl extends ApplicationLoader {

    private final FluffyCustomUpdateManager customUpdateManager = new FluffyCustomUpdateManager();

    @Override
    public void onCreate() {
        super.onCreate();
        AppFontHook.initializeGlobalOverride();
        AppFontHook.onFontChanged();
        SyncSettingsHook.initialize(this);
        customUpdateManager.init(this);
    }

    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }

    @Override
    protected boolean isStandalone() {
        return BuildConfig.APPLICATION_ID.endsWith(".web");
    }

    @Override
    protected boolean isBeta() {
        return BuildConfig.APPLICATION_ID.endsWith(".beta");
    }

    @Override
    protected IMapsProvider onCreateMapsProvider() {
        return MapsProviderHook.createMapsProvider();
    }

    @Override
    public boolean checkApkInstallPermissions(Context context) {
        if (customUpdateManager.checkApkInstallPermissions(context)) {
            return true;
        }
        if (context instanceof Activity) {
            AlertsCreator.createApkRestrictedDialog((Activity) context, null).show();
        }
        return false;
    }

    @Override
    public boolean openApkInstall(Activity activity, TLRPC.Document document) {
        if (customUpdateManager.installDownloadedUpdate(activity)) {
            return true;
        }
        if (document != null) {
            return AndroidUtilities.openForView(document, true, activity);
        }
        return false;
    }

    @Override
    public boolean showCustomUpdateAppPopup(Context context, BetaUpdate update, int account) {
        return CustomUpdateUiHook.showCustomUpdateAppPopup(context, update, account);
    }

    @Override
    public boolean isCustomUpdate() {
        return customUpdateManager.shouldUseCustomUpdate(isStandalone(), isBeta(), BuildConfig.DEBUG);
    }

    @Override
    public void downloadUpdate() {
        customUpdateManager.downloadUpdate();
    }

    @Override
    public void cancelDownloadingUpdate() {
        customUpdateManager.cancelDownloadingUpdate();
    }

    @Override
    public boolean isDownloadingUpdate() {
        return customUpdateManager.isDownloadingUpdate();
    }

    @Override
    public float getDownloadingUpdateProgress() {
        return customUpdateManager.getDownloadingUpdateProgress();
    }

    @Override
    public void checkUpdate(boolean force, Runnable whenDone) {
        customUpdateManager.checkUpdate(force, whenDone);
    }

    @Override
    public BetaUpdate getUpdate() {
        return customUpdateManager.getUpdate();
    }

    @Override
    public java.io.File getDownloadedUpdateFile() {
        return customUpdateManager.getDownloadedUpdateFile();
    }

    @Override
    public IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenuContainer) {
        return CustomUpdateUiHook.takeUpdateLayout(activity, sideMenuContainer, isCustomUpdate());
    }
}
