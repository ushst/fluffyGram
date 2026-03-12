package org.ushastoe.fluffy.patches;

import android.app.Activity;
import android.content.Context;
import android.view.ViewGroup;

import org.telegram.messenger.BetaUpdate;
import org.telegram.ui.IUpdateLayout;
import org.ushastoe.fluffy.ui.components.FluffyUpdateLayout;

public final class CustomUpdateUiPatch {

    private CustomUpdateUiPatch() {
    }

    public static boolean showCustomUpdateAppPopup(Context context, BetaUpdate update, int account) {
        return false;
    }

    public static IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenuContainer, boolean customUpdateEnabled) {
        if (!customUpdateEnabled) {
            return null;
        }
        return new FluffyUpdateLayout(activity, sideMenuContainer);
    }
}
