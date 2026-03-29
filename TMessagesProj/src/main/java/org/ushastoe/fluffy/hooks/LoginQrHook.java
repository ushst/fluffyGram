package org.ushastoe.fluffy.hooks;

import android.view.ViewGroup;

import org.telegram.ui.LoginActivity;
import org.ushastoe.fluffy.patches.LoginQrPatch;

public final class LoginQrHook {
    private LoginQrHook() {
    }

    public static void onCreateView(LoginActivity fragment) {
        LoginQrPatch.attach(fragment);
    }

    public static boolean onMenuItemClick(LoginActivity fragment, int id) {
        return LoginQrPatch.onMenuItemClick(fragment, id);
    }

    public static void onPhoneViewOptionsBuilt(LoginActivity fragment, ViewGroup container, boolean afterTestBackend) {
        LoginQrPatch.attachPhoneViewLink(fragment, container, afterTestBackend);
    }

    public static void onFragmentDestroy(LoginActivity fragment) {
        LoginQrPatch.detach(fragment);
    }
}
