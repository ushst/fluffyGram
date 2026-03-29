package org.ushastoe.fluffy.patches;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MediaOnlyProxyPatch {
    public static final String PREF_MEDIA_ONLY_PROXY = "fluffy_proxy_media_only_enabled";

    private static final Object LOCK = new Object();
    private static final Set<Object> ACTIVE_OPERATIONS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Object, Integer> OPERATION_ACCOUNTS = new IdentityHashMap<>();
    private static final Map<Integer, Integer> ACTIVE_ACCOUNT_COUNTS = new HashMap<>();
    private static final Set<Integer> RUNTIME_PROXY_ACCOUNTS = new HashSet<>();

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
                if (!RUNTIME_PROXY_ACCOUNTS.isEmpty()) {
                    for (Integer account : new HashSet<>(RUNTIME_PROXY_ACCOUNTS)) {
                        if (account != null) {
                            disableRuntimeProxy(account);
                        }
                    }
                }
                ACTIVE_OPERATIONS.clear();
                OPERATION_ACCOUNTS.clear();
                ACTIVE_ACCOUNT_COUNTS.clear();
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
        int account = resolveAccount(operation);
        if (account < 0) {
            return;
        }
        if (account != UserConfig.selectedAccount) {
            return;
        }
        synchronized (LOCK) {
            if (!ACTIVE_OPERATIONS.add(operation)) {
                return;
            }
            OPERATION_ACCOUNTS.put(operation, account);
            int activeCount = ACTIVE_ACCOUNT_COUNTS.containsKey(account) ? ACTIVE_ACCOUNT_COUNTS.get(account) : 0;
            ACTIVE_ACCOUNT_COUNTS.put(account, activeCount + 1);
            if (!RUNTIME_PROXY_ACCOUNTS.contains(account)) {
                enableRuntimeProxy(preferences, account);
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
            Integer account = OPERATION_ACCOUNTS.remove(operation);
            if (account == null) {
                return;
            }
            Integer activeCount = ACTIVE_ACCOUNT_COUNTS.get(account);
            if (activeCount == null || activeCount <= 1) {
                ACTIVE_ACCOUNT_COUNTS.remove(account);
                if (RUNTIME_PROXY_ACCOUNTS.contains(account)) {
                    disableRuntimeProxy(account);
                }
            } else {
                ACTIVE_ACCOUNT_COUNTS.put(account, activeCount - 1);
            }
        }
    }

    private static int resolveAccount(Object operation) {
        try {
            java.lang.reflect.Field field = operation.getClass().getDeclaredField("currentAccount");
            field.setAccessible(true);
            Object value = field.get(operation);
            if (value instanceof Integer) {
                int account = (Integer) value;
                if (account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT) {
                    return account;
                }
            }
        } catch (Exception ignore) {
        }
        return -1;
    }

    private static void enableRuntimeProxy(SharedPreferences preferences, int account) {
        String address = preferences.getString("proxy_ip", "");
        int port = preferences.getInt("proxy_port", 1080);
        String user = preferences.getString("proxy_user", "");
        String pass = preferences.getString("proxy_pass", "");
        String secret = preferences.getString("proxy_secret", "");
        if (TextUtils.isEmpty(address) || account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
            return;
        }
        ConnectionsManager.native_setProxySettings(account, address, port, user, pass, secret);
        RUNTIME_PROXY_ACCOUNTS.add(account);
    }

    private static void disableRuntimeProxy(int account) {
        if (account < 0 || account >= UserConfig.MAX_ACCOUNT_COUNT) {
            return;
        }
        ConnectionsManager.native_setProxySettings(account, "", 1080, "", "", "");
        RUNTIME_PROXY_ACCOUNTS.remove(account);
    }
}
