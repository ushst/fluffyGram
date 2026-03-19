package org.ushastoe.fluffy.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.DateFormat;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;
import org.ushastoe.fluffy.patches.FluffySettingsSyncPatch;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FluffySyncManager {

    public interface Callback {
        void onComplete(boolean success, String errorMessage);
    }

    public interface ActivityLogCallback {
        void onComplete(List<ActivityEntry> items, String role, String errorMessage);
    }

    public static final class ActivityEntry {
        public final String action;
        public final String deviceName;
        public final long createdAtMs;

        public ActivityEntry(String action, String deviceName, long createdAtMs) {
            this.action = action;
            this.deviceName = deviceName;
            this.createdAtMs = createdAtMs;
        }
    }

    private interface SessionCallback {
        void onResult(String token, String errorMessage);
    }

    private static final String PREFS_NAME = "fluffy_sync_settings";
    private static final String KEY_HELPER_BOT = "helper_bot";
    private static final String KEY_BACKEND_URL = "backend_url";
    private static final String KEY_AUTO_SYNC = "auto_sync";
    private static final String KEY_LAST_ACTION = "last_action";
    private static final String KEY_LAST_COMPLETED_AT = "last_completed_at";
    private static final String KEY_LAST_ERROR = "last_error";
    private static final String KEY_NETWORK_CONSENT = "network_consent";
    private static final String KEY_COOLDOWN_UNTIL = "cooldown_until";
    private static final String KEY_STATE_USER_ID = "state_user_id";
    private static final String KEY_SESSION_TOKEN = "session_token";
    private static final String KEY_SESSION_EXPIRES_AT = "session_expires_at";
    private static final String KEY_SESSION_USER_ID = "session_user_id";
    private static final String KEY_SESSION_ROLE = "session_role";

    private static final String ACTION_PUSH = "push";
    private static final String ACTION_PULL = "pull";
    private static final String STORAGE_KEY_SETTINGS = "fluffy_settings_bundle";

    private static final String DEFAULT_HELPER_BOT = "";
    private static final String DEFAULT_BACKEND_URL = "";

    private static final long AUTO_SYNC_DELAY_MS = 1500L;
    private static final long AUTH_POLL_DELAY_MS = 1200L;
    private static final long AUTH_POLL_TIMEOUT_MS = 45000L;
    private static final long SESSION_EXPIRY_MARGIN_MS = 60000L;
    private static final int MAX_SYNC_VALUE_BYTES = 16 * 1024;
    private static final Pattern RETRY_SECONDS_PATTERN = Pattern.compile("(?i)retry in\\s+(\\d+)\\s+seconds");

    private static final FluffySyncManager INSTANCE = new FluffySyncManager();

    private final Object lock = new Object();
    private final Runnable autoSyncRunnable = () -> pushAppearanceSettings(null, null);

    private SharedPreferences preferences;
    private boolean initialized;
    private boolean syncing;
    private boolean applyingRemoteState;

    private FluffySyncManager() {
    }

    public static FluffySyncManager getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        if (context == null) {
            return;
        }
        synchronized (lock) {
            if (initialized) {
                return;
            }
            preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            initialized = true;
        }
        ensureCurrentUserState();
        AppearanceSettingsPatch.addListener(this::onAppearanceSettingsChanged);
    }

    public String getHelperBotUsername() {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return getConfiguredHelperBot();
        }
        String value = normalizeUsername(prefs.getString(KEY_HELPER_BOT, getConfiguredHelperBot()));
        return TextUtils.isEmpty(value) ? getConfiguredHelperBot() : value;
    }

    public void setHelperBotUsername(String username) {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        String value = normalizeUsername(username);
        if (TextUtils.isEmpty(value)) {
            value = getConfiguredHelperBot();
        }
        prefs.edit().putString(KEY_HELPER_BOT, value).apply();
    }

    public String getBackendBaseUrl() {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return getConfiguredBackendUrl();
        }
        String value = normalizeBaseUrl(prefs.getString(KEY_BACKEND_URL, getConfiguredBackendUrl()));
        return TextUtils.isEmpty(value) ? getConfiguredBackendUrl() : value;
    }

    public void setBackendBaseUrl(String value) {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        String normalized = normalizeBaseUrl(value);
        if (TextUtils.isEmpty(normalized)) {
            normalized = getConfiguredBackendUrl();
        }
        prefs.edit().putString(KEY_BACKEND_URL, normalized).apply();
        clearSession();
    }

    public boolean isAutoSyncEnabled() {
        SharedPreferences prefs = getPreferences();
        return prefs != null && prefs.getBoolean(KEY_AUTO_SYNC, false);
    }

    public void setAutoSyncEnabled(boolean enabled) {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit().putBoolean(KEY_AUTO_SYNC, enabled).apply();
        if (!enabled) {
            AndroidUtilities.cancelRunOnUIThread(autoSyncRunnable);
        }
    }

    public boolean isConfigured() {
        return !TextUtils.isEmpty(getHelperBotUsername()) && !TextUtils.isEmpty(getBackendBaseUrl());
    }

    public boolean hasNetworkConsent() {
        SharedPreferences prefs = getPreferences();
        return prefs != null && prefs.getBoolean(KEY_NETWORK_CONSENT, false);
    }

    public void setNetworkConsent(boolean value) {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit().putBoolean(KEY_NETWORK_CONSENT, value).apply();
    }

    public boolean hasValidSession() {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return false;
        }
        String token = prefs.getString(KEY_SESSION_TOKEN, "");
        long expiresAt = prefs.getLong(KEY_SESSION_EXPIRES_AT, 0L);
        String sessionUserId = prefs.getString(KEY_SESSION_USER_ID, "");
        if (TextUtils.isEmpty(token) || expiresAt <= System.currentTimeMillis() + SESSION_EXPIRY_MARGIN_MS) {
            return false;
        }
        long currentUserId = getCurrentUserId();
        return currentUserId <= 0L || TextUtils.equals(sessionUserId, String.valueOf(currentUserId));
    }

    public boolean isSyncing() {
        synchronized (lock) {
            return syncing;
        }
    }

    public String getStatusText() {
        if (!isConfigured()) {
            return LocaleController.getString(R.string.FluffySyncBotNotConfigured);
        }
        if (isSyncing()) {
            return LocaleController.getString(R.string.FluffySyncStatusSyncing);
        }
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return LocaleController.getString(R.string.FluffySyncStatusIdle);
        }
        String error = prefs.getString(KEY_LAST_ERROR, "");
        if (!TextUtils.isEmpty(error)) {
            return LocaleController.formatString("FluffySyncStatusError", R.string.FluffySyncStatusError, error);
        }
        if (!hasValidSession()) {
            return LocaleController.getString(R.string.FluffySyncStatusAuthRequired);
        }
        String action = prefs.getString(KEY_LAST_ACTION, "");
        long timestamp = prefs.getLong(KEY_LAST_COMPLETED_AT, 0L);
        if (timestamp > 0L) {
            String time = DateFormat.getTimeFormat(ApplicationLoader.applicationContext).format(new Date(timestamp));
            if (ACTION_PULL.equals(action)) {
                return LocaleController.formatString("FluffySyncStatusPullSuccess", R.string.FluffySyncStatusPullSuccess, time);
            }
            if (ACTION_PUSH.equals(action)) {
                return LocaleController.formatString("FluffySyncStatusPushSuccess", R.string.FluffySyncStatusPushSuccess, time);
            }
        }
        return LocaleController.getString(R.string.FluffySyncStatusIdle);
    }

    public String getRoleText() {
        SharedPreferences prefs = getPreferences();
        String role = prefs != null ? prefs.getString(KEY_SESSION_ROLE, "") : "";
        if ("admin".equalsIgnoreCase(role)) {
            return LocaleController.getString(R.string.FluffySyncRoleAdmin);
        }
        if ("premium".equalsIgnoreCase(role)) {
            return LocaleController.getString(R.string.FluffySyncRolePremium);
        }
        return LocaleController.getString(R.string.FluffySyncRoleUser);
    }

    public boolean hasServerPremiumAccess() {
        SharedPreferences prefs = getPreferences();
        String role = prefs != null ? prefs.getString(KEY_SESSION_ROLE, "") : "";
        return "admin".equalsIgnoreCase(role) || "premium".equalsIgnoreCase(role);
    }

    public long getCooldownRemainingSeconds() {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return 0L;
        }
        long until = prefs.getLong(KEY_COOLDOWN_UNTIL, 0L);
        long remainingMs = until - System.currentTimeMillis();
        if (remainingMs <= 0L) {
            clearCooldown();
            return 0L;
        }
        return Math.max(1L, (remainingMs + 999L) / 1000L);
    }

    public boolean isCooldownActive() {
        return getCooldownRemainingSeconds() > 0L;
    }

    public void pushAppearanceSettings(Callback callback) {
        pushAppearanceSettings(null, callback);
    }

    public void pushAppearanceSettings(BaseFragment fragment, Callback callback) {
        if (!canStartSync(callback)) {
            return;
        }
        ensureSession(fragment, (token, errorMessage) -> {
            if (TextUtils.isEmpty(token)) {
                finishSync(false, ACTION_PUSH, errorMessage, callback);
                return;
            }
            try {
                String settingsJson = FluffySettingsSyncPatch.exportSettingsJson();
                if (!AppearanceSettingsPatch.isSyncJsonValid(settingsJson) || settingsJson.getBytes(StandardCharsets.UTF_8).length > MAX_SYNC_VALUE_BYTES) {
                    finishSync(false, ACTION_PUSH, LocaleController.getString(R.string.FluffySyncPayloadTooLarge), callback);
                    return;
                }
                JSONObject params = new JSONObject();
                params.put("key", STORAGE_KEY_SETTINGS);
                params.put("value", settingsJson);
                performCustomMethod(token, "saveStorageValue", params, ACTION_PUSH, callback, response -> {
                    finishSync(true, ACTION_PUSH, null, callback);
                });
            } catch (Exception e) {
                FileLog.e(e);
                finishSync(false, ACTION_PUSH, getReadableError(e), callback);
            }
        });
    }

    public void pullAppearanceSettings(Callback callback) {
        pullAppearanceSettings(null, callback);
    }

    public void pullAppearanceSettings(BaseFragment fragment, Callback callback) {
        if (!canStartSync(callback)) {
            return;
        }
        ensureSession(fragment, (token, errorMessage) -> {
            if (TextUtils.isEmpty(token)) {
                finishSync(false, ACTION_PULL, errorMessage, callback);
                return;
            }
            try {
                JSONObject params = new JSONObject();
                JSONArray keys = new JSONArray();
                keys.put(STORAGE_KEY_SETTINGS);
                params.put("keys", keys);
                performCustomMethod(token, "getStorageValues", params, ACTION_PULL, callback, response -> {
                    try {
                        String settingsJson = response != null ? response.optString(STORAGE_KEY_SETTINGS, "") : "";
                        if (!AppearanceSettingsPatch.isSyncJsonValid(settingsJson)) {
                            finishSync(false, ACTION_PULL, LocaleController.getString(R.string.FluffySyncPayloadTooLarge), callback);
                            return;
                        }
                        applyingRemoteState = true;
                        try {
                            FluffySettingsSyncPatch.importSettingsJson(settingsJson);
                        } finally {
                            applyingRemoteState = false;
                        }
                        finishSync(true, ACTION_PULL, null, callback);
                    } catch (Exception e) {
                        FileLog.e(e);
                        finishSync(false, ACTION_PULL, getReadableError(e), callback);
                    }
                });
            } catch (Exception e) {
                FileLog.e(e);
                finishSync(false, ACTION_PULL, getReadableError(e), callback);
            }
        });
    }

    private void ensureSession(BaseFragment fragment, SessionCallback callback) {
        if (!hasNetworkConsent()) {
            callback.onResult(null, LocaleController.getString(R.string.FluffySyncConsentRequired));
            return;
        }
        if (hasValidSession()) {
            SharedPreferences prefs = getPreferences();
            if (prefs != null) {
                callback.onResult(prefs.getString(KEY_SESSION_TOKEN, ""), null);
                return;
            }
        }
        if (fragment == null) {
            callback.onResult(null, LocaleController.getString(R.string.FluffySyncAuthRequired));
            return;
        }
        requestSessionViaMiniApp(fragment, callback);
    }

    private void requestSessionViaMiniApp(BaseFragment fragment, SessionCallback callback) {
        FluffySyncBackendClient.postJson(buildUrl("/auth/telegram/request"), new JSONObject(), null, (response, errorMessage) -> {
            if (!TextUtils.isEmpty(errorMessage) || response == null || !response.optBoolean("ok")) {
                callback.onResult(null, firstNonEmpty(errorMessage, readBackendError(response), LocaleController.getString(R.string.FluffySyncFailed)));
                return;
            }
            String requestId = response.optString("request_id", "");
            if (TextUtils.isEmpty(requestId)) {
                callback.onResult(null, LocaleController.getString(R.string.FluffySyncFailed));
                return;
            }
            openHelperMiniApp(fragment, requestId, callback);
        });
    }

    private void openHelperMiniApp(BaseFragment fragment, String requestId, SessionCallback callback) {
        MessagesController messagesController = MessagesController.getInstance(UserConfig.selectedAccount);
        String botUsername = getHelperBotUsername();
        TLRPC.User cachedUser = messagesController.getUser(botUsername);
        if (cachedUser != null) {
            openHelperMiniAppUser(fragment, cachedUser, requestId, callback);
            return;
        }
        messagesController.getUserNameResolver().resolve(botUsername, peerId -> AndroidUtilities.runOnUIThread(() -> {
            if (peerId == null || peerId <= 0L) {
                callback.onResult(null, LocaleController.getString(R.string.FluffySyncBotResolveFailed));
                return;
            }
            TLRPC.User resolvedUser = messagesController.getUser(peerId);
            openHelperMiniAppUser(fragment, resolvedUser, requestId, callback);
        }));
    }

    private void openHelperMiniAppUser(BaseFragment fragment, TLRPC.User user, String requestId, SessionCallback callback) {
        if (fragment == null || user == null || !user.bot) {
            callback.onResult(null, LocaleController.getString(R.string.FluffySyncBotResolveFailed));
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            try {
                MessagesController.getInstance(UserConfig.selectedAccount).openApp(fragment, user, requestId, fragment.getClassGuid(), null);
                pollAuthRequestStatus(requestId, System.currentTimeMillis() + AUTH_POLL_TIMEOUT_MS, callback);
            } catch (Exception e) {
                FileLog.e(e);
                callback.onResult(null, getReadableError(e));
            }
        });
    }

    private void pollAuthRequestStatus(String requestId, long deadlineMs, SessionCallback callback) {
        if (System.currentTimeMillis() >= deadlineMs) {
            callback.onResult(null, LocaleController.getString(R.string.FluffySyncAuthTimedOut));
            return;
        }
        String encodedRequestId = URLEncoder.encode(requestId, StandardCharsets.UTF_8);
        FluffySyncBackendClient.getJson(buildUrl("/auth/telegram/request-status?id=" + encodedRequestId), null, (response, errorMessage) -> {
            if (!TextUtils.isEmpty(errorMessage)) {
                callback.onResult(null, errorMessage);
                return;
            }
            if (response == null || !response.optBoolean("ok")) {
                callback.onResult(null, firstNonEmpty(readBackendError(response), LocaleController.getString(R.string.FluffySyncFailed)));
                return;
            }
            if (!response.optBoolean("done")) {
                AndroidUtilities.runOnUIThread(() -> pollAuthRequestStatus(requestId, deadlineMs, callback), AUTH_POLL_DELAY_MS);
                return;
            }
            String token = response.optString("token", "");
            String userId = response.optString("user_id", "");
            long expiresAt = response.optLong("expires_at", 0L);
            long currentUserId = getCurrentUserId();
            if (currentUserId > 0L && !TextUtils.equals(userId, String.valueOf(currentUserId))) {
                clearSession();
                callback.onResult(null, LocaleController.getString(R.string.FluffySyncWrongAccount));
                return;
            }
            if (TextUtils.isEmpty(token) || expiresAt <= 0L) {
                callback.onResult(null, LocaleController.getString(R.string.FluffySyncFailed));
                return;
            }
            storeSession(token, userId, expiresAt * 1000L);
            callback.onResult(token, null);
        });
    }

    public void loadActivityLog(BaseFragment fragment, int limit, ActivityLogCallback callback) {
        ensureSession(fragment, (token, errorMessage) -> {
            if (TextUtils.isEmpty(token)) {
                if (callback != null) {
                    callback.onComplete(new ArrayList<>(), getRoleText(), errorMessage);
                }
                return;
            }
            requestActivityLog(token, limit, callback);
        });
    }

    public void refreshCloudState(ActivityLogCallback callback) {
        SharedPreferences prefs = getPreferences();
        if (!hasNetworkConsent() || prefs == null || !hasValidSession()) {
            if (callback != null) {
                callback.onComplete(new ArrayList<>(), getRoleText(), null);
            }
            return;
        }
        requestActivityLog(prefs.getString(KEY_SESSION_TOKEN, ""), 1, callback);
    }

    private void requestActivityLog(String token, int limit, ActivityLogCallback callback) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        FluffySyncBackendClient.getJson(buildUrl("/sync/activity?limit=" + safeLimit), token, (response, requestError) -> {
            if (callback == null) {
                return;
            }
            if (!TextUtils.isEmpty(requestError)) {
                callback.onComplete(new ArrayList<>(), getRoleText(), requestError);
                return;
            }
            if (response == null || !response.optBoolean("ok")) {
                callback.onComplete(new ArrayList<>(), getRoleText(), firstNonEmpty(readBackendError(response), LocaleController.getString(R.string.FluffySyncFailed)));
                return;
            }
            storeRole(response.optString("role", ""));
            List<ActivityEntry> result = new ArrayList<>();
            JSONArray items = response.optJSONArray("items");
            if (items != null) {
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item == null) {
                        continue;
                    }
                    result.add(new ActivityEntry(
                            item.optString("action", ""),
                            item.optString("device_name", LocaleController.getString(R.string.FluffySyncUnknownDevice)),
                            item.optLong("created_at", 0L) * 1000L
                    ));
                }
            }
            callback.onComplete(result, getRoleText(), null);
        });
    }

    private void performCustomMethod(String token, String method, JSONObject params, String action, Callback callback, Utilities.Callback<JSONObject> successCallback) {
        try {
            JSONObject body = new JSONObject();
            body.put("custom_method", method);
            body.put("params", params);
            body.put("device_name", getDeviceName());
            FluffySyncBackendClient.postJson(buildUrl("/sync/custom-method"), body, token, (response, errorMessage) -> {
                if (!TextUtils.isEmpty(errorMessage)) {
                    if (errorMessage.toLowerCase().contains("unauthorized")) {
                        clearSession();
                    }
                    finishSync(false, action, errorMessage, callback);
                    return;
                }
                if (response == null) {
                    finishSync(false, action, LocaleController.getString(R.string.FluffySyncFailed), callback);
                    return;
                }
                if (response.has("ok") && !response.optBoolean("ok", true)) {
                    finishSync(false, action, readBackendError(response), callback);
                    return;
                }
                successCallback.run(response);
            });
        } catch (Exception e) {
            FileLog.e(e);
            finishSync(false, action, getReadableError(e), callback);
        }
    }

    private void onAppearanceSettingsChanged() {
        if (applyingRemoteState || !isAutoSyncEnabled() || !isConfigured() || !hasValidSession()) {
            return;
        }
        AndroidUtilities.cancelRunOnUIThread(autoSyncRunnable);
        AndroidUtilities.runOnUIThread(autoSyncRunnable, AUTO_SYNC_DELAY_MS);
    }

    private boolean canStartSync(Callback callback) {
        if (!isConfigured()) {
            finishSync(false, "", LocaleController.getString(R.string.FluffySyncBotNotConfigured), callback);
            return false;
        }
        synchronized (lock) {
            if (syncing) {
                if (callback != null) {
                    AndroidUtilities.runOnUIThread(() -> callback.onComplete(false, LocaleController.getString(R.string.FluffySyncStatusSyncing)));
                }
                return false;
            }
            syncing = true;
        }
        return true;
    }

    private void finishSync(boolean success, String action, String errorMessage, Callback callback) {
        SharedPreferences prefs = getPreferences();
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            if (success) {
                editor.putString(KEY_LAST_ACTION, action != null ? action : "");
                editor.putLong(KEY_LAST_COMPLETED_AT, System.currentTimeMillis());
                editor.remove(KEY_LAST_ERROR);
                editor.remove(KEY_COOLDOWN_UNTIL);
            } else if (!TextUtils.isEmpty(errorMessage)) {
                editor.putString(KEY_LAST_ACTION, action != null ? action : "");
                editor.putString(KEY_LAST_ERROR, errorMessage);
            }
            editor.apply();
        }
        updateCooldownFromError(errorMessage);
        synchronized (lock) {
            syncing = false;
        }
        if (callback != null) {
            AndroidUtilities.runOnUIThread(() -> callback.onComplete(success, errorMessage));
        }
    }

    private SharedPreferences getPreferences() {
        SharedPreferences prefs = preferences;
        if (prefs != null) {
            ensureCurrentUserState();
            return prefs;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        init(context);
        ensureCurrentUserState();
        return preferences;
    }

    private void ensureCurrentUserState() {
        SharedPreferences prefs = preferences;
        if (prefs == null) {
            return;
        }
        long currentUserId = getCurrentUserId();
        if (currentUserId <= 0L) {
            return;
        }
        String currentUserIdString = String.valueOf(currentUserId);
        String storedUserId = prefs.getString(KEY_STATE_USER_ID, "");
        if (TextUtils.equals(storedUserId, currentUserIdString)) {
            return;
        }
        prefs.edit()
                .putString(KEY_STATE_USER_ID, currentUserIdString)
                .remove(KEY_LAST_ACTION)
                .remove(KEY_LAST_COMPLETED_AT)
                .remove(KEY_LAST_ERROR)
                .remove(KEY_COOLDOWN_UNTIL)
                .remove(KEY_SESSION_TOKEN)
                .remove(KEY_SESSION_EXPIRES_AT)
                .remove(KEY_SESSION_USER_ID)
                .remove(KEY_SESSION_ROLE)
                .apply();
    }

    private void storeSession(String token, String userId, long expiresAtMs) {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit()
                .putString(KEY_SESSION_TOKEN, token)
                .putString(KEY_SESSION_USER_ID, userId != null ? userId : "")
                .putLong(KEY_SESSION_EXPIRES_AT, expiresAtMs)
                .remove(KEY_LAST_ERROR)
                .apply();
    }

    private void storeRole(String role) {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit().putString(KEY_SESSION_ROLE, role != null ? role.trim() : "").apply();
    }

    private void clearSession() {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit()
                .remove(KEY_SESSION_TOKEN)
                .remove(KEY_SESSION_USER_ID)
                .remove(KEY_SESSION_EXPIRES_AT)
                .remove(KEY_SESSION_ROLE)
                .apply();
    }

    private void updateCooldownFromError(String errorMessage) {
        long seconds = parseRetryAfterSeconds(errorMessage);
        if (seconds <= 0L) {
            return;
        }
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit().putLong(KEY_COOLDOWN_UNTIL, System.currentTimeMillis() + seconds * 1000L).apply();
    }

    private void clearCooldown() {
        SharedPreferences prefs = getPreferences();
        if (prefs == null) {
            return;
        }
        prefs.edit().remove(KEY_COOLDOWN_UNTIL).apply();
    }

    private static long parseRetryAfterSeconds(String message) {
        if (TextUtils.isEmpty(message)) {
            return 0L;
        }
        Matcher matcher = RETRY_SECONDS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return 0L;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private long getCurrentUserId() {
        try {
            return UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        } catch (Exception e) {
            FileLog.e(e);
            return 0L;
        }
    }

    private String buildUrl(String path) {
        String baseUrl = getBackendBaseUrl();
        if (TextUtils.isEmpty(baseUrl)) {
            baseUrl = getConfiguredBackendUrl();
        }
        if (path.startsWith("/")) {
            return baseUrl + path;
        }
        return baseUrl + "/" + path;
    }

    private String getReadableError(Exception error) {
        String message = error != null ? error.getMessage() : null;
        if (TextUtils.isEmpty(message)) {
            return LocaleController.getString(R.string.FluffySyncFailed);
        }
        return message;
    }

    private String readBackendError(JSONObject response) {
        if (response == null) {
            return LocaleController.getString(R.string.FluffySyncFailed);
        }
        String message = response.optString("message", "");
        if (TextUtils.isEmpty(message)) {
            message = response.optString("error", "");
        }
        if (TextUtils.isEmpty(message)) {
            message = LocaleController.getString(R.string.FluffySyncFailed);
        }
        return message;
    }

    private static String normalizeUsername(String value) {
        String username = value != null ? value.trim() : "";
        while (username.startsWith("@")) {
            username = username.substring(1);
        }
        if (username.startsWith("https://t.me/")) {
            username = username.substring("https://t.me/".length());
        } else if (username.startsWith("http://t.me/")) {
            username = username.substring("http://t.me/".length());
        } else if (username.startsWith("t.me/")) {
            username = username.substring("t.me/".length());
        }
        int slashIndex = username.indexOf('/');
        if (slashIndex >= 0) {
            username = username.substring(0, slashIndex);
        }
        int queryIndex = username.indexOf('?');
        if (queryIndex >= 0) {
            username = username.substring(0, queryIndex);
        }
        return username.trim();
    }

    private static String normalizeBaseUrl(String value) {
        String url = value != null ? value.trim() : "";
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String getConfiguredBackendUrl() {
        return normalizeBaseUrl(BuildConfig.FLUFFY_SYNC_BACKEND_URL);
    }

    private static String getConfiguredHelperBot() {
        return normalizeUsername(BuildConfig.FLUFFY_SYNC_HELPER_BOT);
    }

    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.trim() : "";
        String model = Build.MODEL != null ? Build.MODEL.trim() : "";
        String deviceName = (manufacturer + " " + model).trim();
        if (deviceName.isEmpty()) {
            return "android device";
        }
        return deviceName.toLowerCase(Locale.US);
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }
}
