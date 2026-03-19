package org.ushastoe.fluffy.patches;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.ConnectionsManager;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class MediaOnlyProxyPatch {
    public static final String PREF_MEDIA_ONLY_PROXY = "fluffy_proxy_media_only_enabled";

    private static final Object LOCK = new Object();
    private static final Set<Object> ACTIVE_OPERATIONS = Collections.newSetFromMap(new IdentityHashMap<>());

    private static boolean runtimeProxyEnabledByPatch;

    private MediaOnlyProxyPatch() {
    }

    public static String exportSettingsJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(PREF_MEDIA_ONLY_PROXY, isEnabled());
        } catch (Exception ignore) {
        }
        return object.toString();
    }

    public static void importSettingsJson(String json) {
        try {
            JSONObject object = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
            setEnabled(object.optBoolean(PREF_MEDIA_ONLY_PROXY, false));
        } catch (Exception ignore) {
        }
    }

    public static boolean isEnabled() {
        return MessagesController.getGlobalMainSettings().getBoolean(PREF_MEDIA_ONLY_PROXY, false);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        preferences.edit().putBoolean(PREF_MEDIA_ONLY_PROXY, enabled).apply();
        if (!enabled) {
            synchronized (LOCK) {
                if (runtimeProxyEnabledByPatch) {
                    disableRuntimeProxy();
                }
                ACTIVE_OPERATIONS.clear();
            }
        }
    }

    public static void onMediaOperationStart(Object operation) {
        if (operation == null || !isEnabled()) {
            return;
        }
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        if (preferences.getBoolean("proxy_enabled", false)) {
            return;
        }
        synchronized (LOCK) {
            if (!ACTIVE_OPERATIONS.add(operation)) {
                return;
            }
            if (!runtimeProxyEnabledByPatch) {
                enableRuntimeProxy(preferences);
            }
        }
    }

    public static void onMediaOperationStop(Object operation) {
        if (operation == null) {
            return;
        }
        synchronized (LOCK) {
            if (!ACTIVE_OPERATIONS.remove(operation)) {
                return;
            }
            if (ACTIVE_OPERATIONS.isEmpty() && runtimeProxyEnabledByPatch) {
                disableRuntimeProxy();
            }
        }
    }

    private static void enableRuntimeProxy(SharedPreferences preferences) {
        String address = preferences.getString("proxy_ip", "");
        int port = preferences.getInt("proxy_port", 1080);
        String user = preferences.getString("proxy_user", "");
        String pass = preferences.getString("proxy_pass", "");
        String secret = preferences.getString("proxy_secret", "");
        if (TextUtils.isEmpty(address)) {
            return;
        }
        ConnectionsManager.setProxySettings(true, address, port, user, pass, secret);
        runtimeProxyEnabledByPatch = true;
    }

    private static void disableRuntimeProxy() {
        ConnectionsManager.setProxySettings(false, "", 1080, "", "", "");
        runtimeProxyEnabledByPatch = false;
    }
}
