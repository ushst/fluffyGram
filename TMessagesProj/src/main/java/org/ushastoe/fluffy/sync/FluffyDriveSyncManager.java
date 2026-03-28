package org.ushastoe.fluffy.sync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.utils.FluffyConfigFileStore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FluffyDriveSyncManager {

    public interface AuthorizationCallback {
        void onAuthorized(@NonNull AuthorizationResult result);
        void onError(String errorMessage);
    }

    public interface SyncCallback {
        void onComplete(boolean success, String errorMessage);
    }

    public interface BackupListCallback {
        void onComplete(@NonNull List<BackupEntry> backups, String errorMessage);
    }

    public static final class BackupEntry {
        public final String id;
        public final String name;
        public final long backupAt;
        public final long sizeBytes;
        public final boolean legacy;

        public BackupEntry(String id, String name, long backupAt, long sizeBytes, boolean legacy) {
            this.id = id;
            this.name = name;
            this.backupAt = backupAt;
            this.sizeBytes = sizeBytes;
            this.legacy = legacy;
        }
    }

    public static final String DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata";
    public static final int REQUEST_CODE_AUTHORIZE_DRIVE = 6201;

    private static final String PREFS_NAME = "fluffy_sync_settings";
    private static final String KEY_DRIVE_AUTHORIZED = "drive_authorized";
    private static final String KEY_DRIVE_FILE_ID = "drive_file_id";
    private static final String KEY_DRIVE_LAST_BACKUP_AT = "drive_last_backup_at";
    private static final String KEY_DRIVE_LAST_RESTORE_AT = "drive_last_restore_at";
    private static final String KEY_DRIVE_BACKUP_HISTORY_LIMIT = "drive_backup_history_limit";

    private static final String DRIVE_FILE_NAME = "fluffy_config.json";
    private static final String DRIVE_BACKUP_PREFIX = "fluffy_config_backup_";
    private static final String DRIVE_BACKUP_SUFFIX = ".json";
    private static final int DEFAULT_BACKUP_HISTORY_LIMIT = 20;
    private static final String DRIVE_API_BASE = "https://www.googleapis.com/drive/v3/files";
    private static final String DRIVE_UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3/files";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 15000;

    private static final FluffyDriveSyncManager INSTANCE = new FluffyDriveSyncManager();

    private FluffyDriveSyncManager() {
    }

    public static FluffyDriveSyncManager getInstance() {
        return INSTANCE;
    }

    public boolean isAuthorized(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences != null && preferences.getBoolean(KEY_DRIVE_AUTHORIZED, false);
    }

    public String getStatusText(Context context) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null || !preferences.getBoolean(KEY_DRIVE_AUTHORIZED, false)) {
            return LocaleController.getString(R.string.FluffyDriveNotConnected);
        }
        long backupAt = preferences.getLong(KEY_DRIVE_LAST_BACKUP_AT, 0L);
        if (backupAt > 0L) {
            return LocaleController.formatString("FluffyDriveLastBackupValue", R.string.FluffyDriveLastBackupValue,
                    LocaleController.getInstance().getFormatterYearMax().format(backupAt),
                    LocaleController.getInstance().getFormatterDay().format(backupAt));
        }
        return LocaleController.getString(R.string.FluffyDriveConnected);
    }

    public int getBackupHistoryLimit(Context context) {
        SharedPreferences preferences = getPreferences(context);
        int limit = preferences != null ? preferences.getInt(KEY_DRIVE_BACKUP_HISTORY_LIMIT, DEFAULT_BACKUP_HISTORY_LIMIT) : DEFAULT_BACKUP_HISTORY_LIMIT;
        return sanitizeBackupHistoryLimit(limit);
    }

    public void setBackupHistoryLimit(Context context, int limit) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DRIVE_BACKUP_HISTORY_LIMIT, sanitizeBackupHistoryLimit(limit)).apply();
    }

    public void forgetLocalState(Context context) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putBoolean(KEY_DRIVE_AUTHORIZED, false)
                .remove(KEY_DRIVE_FILE_ID)
                .apply();
    }

    public void authorize(BaseFragment fragment, AuthorizationCallback callback) {
        if (fragment == null || fragment.getParentActivity() == null) {
            if (callback != null) {
                callback.onError(LocaleController.getString(R.string.FluffyDriveAuthorizationFailed));
            }
            return;
        }
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(DRIVE_APPDATA_SCOPE)))
                .build();
        Identity.getAuthorizationClient(fragment.getParentActivity())
                .authorize(request)
                .addOnSuccessListener(result -> {
                    if (result == null) {
                        if (callback != null) {
                            callback.onError(LocaleController.getString(R.string.FluffyDriveAuthorizationFailed));
                        }
                        return;
                    }
                    if (result.hasResolution()) {
                        try {
                            fragment.getParentActivity().startIntentSenderForResult(
                                    result.getPendingIntent().getIntentSender(),
                                    REQUEST_CODE_AUTHORIZE_DRIVE,
                                    null,
                                    0,
                                    0,
                                    0
                            );
                        } catch (Exception e) {
                            FileLog.e(e);
                            if (callback != null) {
                                callback.onError(buildErrorMessage(e));
                            }
                        }
                    } else if (callback != null) {
                        persistAuthorized(fragment.getParentActivity(), true);
                        callback.onAuthorized(result);
                    }
                })
                .addOnFailureListener(e -> {
                    FileLog.e(e);
                    if (callback != null) {
                        callback.onError(buildErrorMessage(e));
                    }
                });
    }

    public void handleAuthorizationResult(Context context, Intent data, AuthorizationCallback callback) {
        if (context == null) {
            if (callback != null) {
                callback.onError(LocaleController.getString(R.string.FluffyDriveAuthorizationFailed));
            }
            return;
        }
        try {
            AuthorizationResult result = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data);
            if (result == null) {
                if (callback != null) {
                    callback.onError(LocaleController.getString(R.string.FluffyDriveAuthorizationFailed));
                }
                return;
            }
            persistAuthorized(context, true);
            if (callback != null) {
                callback.onAuthorized(result);
            }
        } catch (ApiException e) {
            FileLog.e(e);
            if (callback != null) {
                callback.onError(buildErrorMessage(e));
            }
        }
    }

    public void backupConfig(Context context, AuthorizationResult authorizationResult, SyncCallback callback) {
        String accessToken = authorizationResult != null ? authorizationResult.getAccessToken() : null;
        if (context == null || TextUtils.isEmpty(accessToken)) {
            if (callback != null) {
                callback.onComplete(false, LocaleController.getString(R.string.FluffyDriveAuthorizationFailed));
            }
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            String errorMessage = null;
            boolean success = false;
            try {
                String payload = FluffyConfigFileStore.exportToJsonString(context);
                String fileId = createDriveBackupFile(accessToken, payload, System.currentTimeMillis());
                trimBackupHistory(context, accessToken);
                persistBackupState(context, fileId);
                success = true;
            } catch (Exception e) {
                FileLog.e(e);
                errorMessage = buildErrorMessage(e);
            }
            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onComplete(finalSuccess, finalErrorMessage);
                }
            });
        });
    }

    public void deleteBackup(Context context, AuthorizationResult authorizationResult, String backupFileId, SyncCallback callback) {
        String accessToken = authorizationResult != null ? authorizationResult.getAccessToken() : null;
        if (context == null || TextUtils.isEmpty(accessToken) || TextUtils.isEmpty(backupFileId)) {
            if (callback != null) {
                callback.onComplete(false, LocaleController.getString(R.string.FluffyDriveRequestFailed));
            }
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            String errorMessage = null;
            boolean success = false;
            try {
                deleteDriveFile(accessToken, backupFileId);
                clearCachedFileIdIfMatches(context, backupFileId);
                success = true;
            } catch (Exception e) {
                FileLog.e(e);
                errorMessage = buildErrorMessage(e);
            }
            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onComplete(finalSuccess, finalErrorMessage);
                }
            });
        });
    }

    public void restoreConfig(Context context, AuthorizationResult authorizationResult, SyncCallback callback) {
        restoreConfig(context, authorizationResult, null, callback);
    }

    public void restoreConfig(Context context, AuthorizationResult authorizationResult, String backupFileId, SyncCallback callback) {
        String accessToken = authorizationResult != null ? authorizationResult.getAccessToken() : null;
        if (context == null || TextUtils.isEmpty(accessToken)) {
            if (callback != null) {
                callback.onComplete(false, LocaleController.getString(R.string.FluffyDriveAuthorizationFailed));
            }
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            String errorMessage = null;
            boolean success = false;
            try {
                String fileId = backupFileId;
                if (TextUtils.isEmpty(fileId)) {
                    fileId = resolveLatestBackupFileId(context, accessToken);
                }
                if (TextUtils.isEmpty(fileId)) {
                    throw new IllegalStateException(LocaleController.getString(R.string.FluffyDriveFileMissing));
                }
                String payload = downloadDriveFile(accessToken, fileId);
                if (!FluffyConfigFileStore.importFromJsonString(context, payload)) {
                    throw new IllegalStateException(LocaleController.getString(R.string.FluffyImportConfigFailed));
                }
                persistRestoreState(context, fileId);
                success = true;
            } catch (Exception e) {
                FileLog.e(e);
                errorMessage = buildErrorMessage(e);
            }
            boolean finalSuccess = success;
            String finalErrorMessage = errorMessage;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onComplete(finalSuccess, finalErrorMessage);
                }
            });
        });
    }

    public void listBackups(Context context, AuthorizationResult authorizationResult, BackupListCallback callback) {
        String accessToken = authorizationResult != null ? authorizationResult.getAccessToken() : null;
        if (context == null || TextUtils.isEmpty(accessToken)) {
            if (callback != null) {
                callback.onComplete(Collections.emptyList(), LocaleController.getString(R.string.FluffyDriveAuthorizationFailed));
            }
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<BackupEntry> backups = new ArrayList<>();
            String errorMessage = null;
            try {
                backups.addAll(queryBackupEntries(accessToken));
            } catch (Exception e) {
                FileLog.e(e);
                errorMessage = buildErrorMessage(e);
            }
            String finalErrorMessage = errorMessage;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onComplete(backups, finalErrorMessage);
                }
            });
        });
    }

    private String resolveLatestBackupFileId(Context context, String accessToken) throws Exception {
        SharedPreferences preferences = getPreferences(context);
        String fileId = preferences != null ? preferences.getString(KEY_DRIVE_FILE_ID, "") : "";
        if (!TextUtils.isEmpty(fileId)) {
            return fileId;
        }
        List<BackupEntry> backups = queryBackupEntries(accessToken);
        if (backups.isEmpty()) {
            return "";
        }
        return backups.get(0).id;
    }

    private ArrayList<BackupEntry> queryBackupEntries(String accessToken) throws Exception {
        String query = "trashed = false and (name = '" + DRIVE_FILE_NAME + "' or name contains '" + DRIVE_BACKUP_PREFIX + "')";
        String url = DRIVE_API_BASE
                + "?spaces=appDataFolder&fields=files(id,name,size,modifiedTime)&orderBy=modifiedTime desc&pageSize=100&q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8.name());
        JSONObject response = requestJson("GET", url, null, accessToken, false);
        JSONArray files = response.optJSONArray("files");
        ArrayList<BackupEntry> entries = new ArrayList<>();
        if (files == null) {
            return entries;
        }
        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.optJSONObject(i);
            if (file == null) {
                continue;
            }
            String id = file.optString("id", "");
            String name = file.optString("name", "");
            if (TextUtils.isEmpty(id) || TextUtils.isEmpty(name)) {
                continue;
            }
            boolean legacy = DRIVE_FILE_NAME.equals(name);
            long backupAt = parseBackupTimestamp(name);
            if (!legacy && backupAt <= 0L) {
                continue;
            }
            entries.add(new BackupEntry(id, name, backupAt, file.optLong("size", 0L), legacy));
        }
        return entries;
    }

    private String createDriveBackupFile(String accessToken, String payload, long backupAt) throws Exception {
        JSONObject metadata = new JSONObject();
        metadata.put("name", buildBackupFileName(backupAt));
        JSONArray parents = new JSONArray();
        parents.put("appDataFolder");
        metadata.put("parents", parents);
        String boundary = "fluffyDriveBoundary" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(boundary, metadata.toString(), payload);
        String url = DRIVE_UPLOAD_BASE + "?uploadType=multipart&fields=id";
        JSONObject response = requestJson("POST", url, body, accessToken, true, boundary);
        return response.optString("id", "");
    }

    private void trimBackupHistory(Context context, String accessToken) throws Exception {
        List<BackupEntry> backups = queryBackupEntries(accessToken);
        int limit = getBackupHistoryLimit(context);
        int kept = 0;
        for (BackupEntry backup : backups) {
            if (backup.legacy) {
                continue;
            }
            kept++;
            if (kept <= limit) {
                continue;
            }
            deleteDriveFile(accessToken, backup.id);
        }
    }

    private String downloadDriveFile(String accessToken, String fileId) throws Exception {
        String url = DRIVE_API_BASE + "/" + fileId + "?alt=media";
        return requestText("GET", url, null, accessToken, null);
    }

    private void deleteDriveFile(String accessToken, String fileId) throws Exception {
        String url = DRIVE_API_BASE + "/" + fileId;
        requestText("DELETE", url, null, accessToken, null);
    }

    private JSONObject requestJson(String method, String url, byte[] body, String accessToken, boolean multipart) throws Exception {
        return requestJson(method, url, body, accessToken, multipart, null);
    }

    private JSONObject requestJson(String method, String url, byte[] body, String accessToken, boolean multipart, String boundary) throws Exception {
        String text = requestText(method, url, body, accessToken,
                multipart ? "multipart/related; boundary=" + boundary : "application/json; charset=UTF-8");
        return TextUtils.isEmpty(text) ? new JSONObject() : new JSONObject(text);
    }

    private String requestText(String method, String url, byte[] body, String accessToken, String contentType) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("Accept", "application/json");
            if (!TextUtils.isEmpty(contentType)) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(body.length);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body);
                }
            }
            int code = connection.getResponseCode();
            InputStream inputStream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String text = readAll(inputStream);
            if (code < 200 || code >= 300) {
                throw new IllegalStateException(!TextUtils.isEmpty(text) ? text : "HTTP " + code);
            }
            return text;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] buildMultipartBody(String boundary, String metadata, String payload) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeMultipartPart(outputStream, boundary, "application/json; charset=UTF-8", metadata);
        writeMultipartPart(outputStream, boundary, "application/json; charset=UTF-8", payload);
        outputStream.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));
        return outputStream.toByteArray();
    }

    private static void writeMultipartPart(ByteArrayOutputStream outputStream, String boundary, String contentType, String payload) throws Exception {
        outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(payload.getBytes(StandardCharsets.UTF_8));
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String readAll(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        try (InputStream inputStream = stream; ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void persistAuthorized(Context context, boolean authorized) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_DRIVE_AUTHORIZED, authorized).apply();
    }

    private void persistBackupState(Context context, String fileId) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putBoolean(KEY_DRIVE_AUTHORIZED, true)
                .putString(KEY_DRIVE_FILE_ID, fileId)
                .putLong(KEY_DRIVE_LAST_BACKUP_AT, System.currentTimeMillis())
                .apply();
    }

    private void persistRestoreState(Context context, String fileId) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putBoolean(KEY_DRIVE_AUTHORIZED, true)
                .putString(KEY_DRIVE_FILE_ID, fileId)
                .putLong(KEY_DRIVE_LAST_RESTORE_AT, System.currentTimeMillis())
                .apply();
    }

    private SharedPreferences getPreferences(Context context) {
        if (context == null) {
            return null;
        }
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static String buildBackupFileName(long backupAt) {
        return DRIVE_BACKUP_PREFIX + backupAt + DRIVE_BACKUP_SUFFIX;
    }

    private static long parseBackupTimestamp(String name) {
        if (TextUtils.isEmpty(name) || !name.startsWith(DRIVE_BACKUP_PREFIX) || !name.endsWith(DRIVE_BACKUP_SUFFIX)) {
            return 0L;
        }
        int start = DRIVE_BACKUP_PREFIX.length();
        int end = name.length() - DRIVE_BACKUP_SUFFIX.length();
        if (end <= start) {
            return 0L;
        }
        try {
            return Long.parseLong(name.substring(start, end));
        } catch (Exception e) {
            FileLog.e(e);
            return 0L;
        }
    }

    private static String buildErrorMessage(Throwable throwable) {
        String message = throwable != null ? throwable.getMessage() : null;
        if (TextUtils.isEmpty(message)) {
            return LocaleController.getString(R.string.FluffyDriveRequestFailed);
        }
        return message;
    }

    private void clearCachedFileIdIfMatches(Context context, String fileId) {
        SharedPreferences preferences = getPreferences(context);
        if (preferences == null) {
            return;
        }
        if (!TextUtils.equals(preferences.getString(KEY_DRIVE_FILE_ID, ""), fileId)) {
            return;
        }
        preferences.edit().remove(KEY_DRIVE_FILE_ID).apply();
    }

    private static int sanitizeBackupHistoryLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_BACKUP_HISTORY_LIMIT;
        }
        return Math.min(limit, 100);
    }
}
