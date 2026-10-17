package org.ushastoe.fluffy.patches;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.voip.Instance;

/** Keeps the existing SOCKS call-proxy preference connected to the native call API. */
public final class CallProxyCompatibilityPatch {
    private CallProxyCompatibilityPatch() {
    }

    public static Instance.Proxy getProxy(SharedPreferences preferences) {
        if (!preferences.getBoolean("proxy_enabled", false)
                || !preferences.getBoolean("proxy_enabled_calls", false)) {
            return null;
        }
        String server = preferences.getString("proxy_ip", null);
        String secret = preferences.getString("proxy_secret", null);
        if (TextUtils.isEmpty(server) || !TextUtils.isEmpty(secret)) {
            return null;
        }
        return new Instance.Proxy(server, preferences.getInt("proxy_port", 0),
                preferences.getString("proxy_user", null), preferences.getString("proxy_pass", null));
    }
}
