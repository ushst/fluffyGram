package org.ushastoe.fluffy.patches;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.BaseFragment;

public final class ThemeResetPatch {

    private ThemeResetPatch() {
    }

    public static boolean openSystemAppSettings(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return false;
        }
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + fragment.getParentActivity().getPackageName()));
            fragment.getParentActivity().startActivity(intent);
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
}
