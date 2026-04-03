package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.DialogsProxyMenuPatch;

public final class DialogsProxyMenuHook {

    private DialogsProxyMenuHook() {
    }

    public static boolean shouldShowProxyMenuItem(boolean proxyVisible) {
        return DialogsProxyMenuPatch.shouldShowProxyMenuItem(proxyVisible);
    }

    public static CharSequence getProxyMenuSubtext(int currentConnectionState) {
        return DialogsProxyMenuPatch.getProxyMenuSubtext(currentConnectionState);
    }
}
