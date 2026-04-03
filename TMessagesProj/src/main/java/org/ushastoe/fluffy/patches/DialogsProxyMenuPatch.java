package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.ConnectionsManager;

public final class DialogsProxyMenuPatch {

    private DialogsProxyMenuPatch() {
    }

    public static boolean shouldShowProxyMenuItem(boolean proxyVisible) {
        return true;
    }

    public static CharSequence getProxyMenuSubtext(int currentConnectionState) {
        final SharedPreferences preferences = ApplicationLoader.applicationContext
                .getSharedPreferences("mainconfig", Context.MODE_PRIVATE);
        final boolean proxyEnabled = preferences.getBoolean("proxy_enabled", false);
        final String proxyAddress = preferences.getString("proxy_ip", "");
        final boolean hasConfiguredProxy = !TextUtils.isEmpty(proxyAddress) || !SharedConfig.proxyList.isEmpty();
        if (!proxyEnabled || !hasConfiguredProxy) {
            return LocaleController.getString(R.string.RepeatDisabled);
        }
        final boolean connected = currentConnectionState == ConnectionsManager.ConnectionStateConnected
                || currentConnectionState == ConnectionsManager.ConnectionStateUpdating;
        return LocaleController.getString(connected ? R.string.MenuProxyConnected : R.string.MenuProxyConnecting);
    }
}
