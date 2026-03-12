package org.ushastoe.fluffy.hooks;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import org.telegram.messenger.BetaUpdate;
import org.telegram.ui.IUpdateLayout;
import org.ushastoe.fluffy.patches.CustomUpdateUiPatch;

public final class CustomUpdateUiHook {

    private CustomUpdateUiHook() {
    }

    public static boolean showCustomUpdateAppPopup(Context context, BetaUpdate update, int account) {
        return CustomUpdateUiPatch.showCustomUpdateAppPopup(context, update, account);
    }

    public static IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenuContainer, boolean customUpdateEnabled) {
        return CustomUpdateUiPatch.takeUpdateLayout(activity, sideMenuContainer, customUpdateEnabled);
    }
}
