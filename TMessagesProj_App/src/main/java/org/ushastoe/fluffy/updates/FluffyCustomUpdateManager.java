package org.ushastoe.fluffy.updates;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BetaUpdate;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.messenger.regular.BuildConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public final class FluffyCustomUpdateManager {

    private static final String PREFS_NAME = "fluffy_custom_update";
    private static final String KEY_VERSION = "version";
    private static final String KEY_VERSION_CODE = "version_code";
    private static final String KEY_CHANGELOG = "changelog";
    private static final String KEY_APK_URL = "apk_url";
    private static final String KEY_PAGE_URL = "page_url";
    private static final String KEY_SHA256 = "sha256";
    private static final String KEY_FILE_NAME = "file_name";
    private static final String KEY_DOWNLOADED_FILE = "downloaded_file";
    private static final String KEY_DOWNLOADED_VERSION_CODE = "downloaded_version_code";

    private final Object lock = new Object();

    private SharedPreferences preferences;
    private BetaUpdate update;
    private String apkUrl;
    private String pageUrl;
    private String sha256;
    private String fileName;
    private File downloadedFile;

    private boolean downloading;
    private boolean cancelDownload;
    private long downloadedBytes;
    private long totalBytes;
    private Thread downloadThread;
    private HttpURLConnection activeConnection;
    private WeakReference<Activity> installActivityRef;
    private WeakReference<AlertDialog> currentDialogRef;

    public void init(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadState();
    }

    public boolean isEnabled() {
        return !TextUtils.isEmpty(getManifestUrl());
    }

    public boolean shouldUseCustomUpdate(boolean isStandalone, boolean isBeta, boolean isDebugBuild) {
        if (!isEnabled()) {
            return false;
        }
        // Keep updates enabled for the public release channel, while preserving beta/standalone support.
        return !isDebugBuild || isStandalone || isBeta;
    }

    public void checkUpdate(boolean force, Runnable whenDone) {
        if (!isEnabled()) {
            AndroidUtilities.runOnUIThread(whenDone);
            return;
        }
        if (!force && !shouldCheckNow()) {
            AndroidUtilities.runOnUIThread(whenDone);
            return;
        }
        SharedConfig.lastUpdateCheckTime = System.currentTimeMillis();
        SharedConfig.saveConfig();
        Utilities.globalQueue.postRunnable(() -> {
            ParsedUpdate parsed = null;
            boolean failed = false;
            try {
                parsed = parseManifest(fetchUrl(getManifestUrl()));
            } catch (Throwable t) {
                failed = true;
                FileLog.e(t);
            }
            final ParsedUpdate finalParsed = parsed;
            final boolean finalFailed = failed;
            AndroidUtilities.runOnUIThread(() -> {
                if (finalParsed != null) {
                    applyParsedUpdate(finalParsed);
                } else if (!finalFailed) {
                    clearStoredUpdate(true);
                }
                whenDone.run();
            });
        });
    }

    public boolean showUpdateDialog(Context context, BetaUpdate betaUpdate, int account) {
        if (!(context instanceof Activity)) {
            return false;
        }
        final Activity activity = (Activity) context;
        installActivityRef = new WeakReference<>(activity);
        dismissCurrentDialog();

        final String appName = LocaleController.getString(ApplicationLoader.isBetaBuild() ? R.string.AppNameBeta : R.string.AppName);
        final String versionLine = LocaleController.formatString("AppBetaUpdateVersion", R.string.AppBetaUpdateVersion, betaUpdate.version, String.valueOf(betaUpdate.versionCode));
        final boolean readyToInstall = hasDownloadedUpdate(betaUpdate.versionCode);
        final String localApkUrl = apkUrl;
        final String localPageUrl = getResolvedPageUrl();

        StringBuilder message = new StringBuilder(versionLine);
        if (readyToInstall) {
            message.append("\n\n").append(LocaleController.getString(R.string.FluffyUpdateReadyToInstall));
        }
        if (!TextUtils.isEmpty(betaUpdate.changelog)) {
            message.append("\n\n").append(betaUpdate.changelog.trim());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(appName);
        builder.setMessage(message.toString());
        if (readyToInstall) {
            builder.setPositiveButton(LocaleController.getString(R.string.AppUpdateNow), (dialog, which) -> installDownloadedUpdate(activity));
        } else if (!TextUtils.isEmpty(localApkUrl)) {
            builder.setPositiveButton(LocaleController.getString(R.string.AppUpdateDownloadNow), (dialog, which) -> {
                installActivityRef = new WeakReference<>(activity);
                Toast.makeText(activity, LocaleController.getString(R.string.FluffyUpdateDownloadStarted), Toast.LENGTH_SHORT).show();
                downloadUpdate();
            });
        } else {
            builder.setPositiveButton(LocaleController.getString(R.string.FluffyUpdateOpenRelease), (dialog, which) -> openReleasePage(activity, localPageUrl));
        }
        builder.setNegativeButton(LocaleController.getString(R.string.AppUpdateRemindMeLater), null);
        if (!TextUtils.isEmpty(localPageUrl) && !TextUtils.equals(localPageUrl, localApkUrl)) {
            builder.setNeutralButton(LocaleController.getString(R.string.FluffyUpdateOpenRelease), (dialog, which) -> openReleasePage(activity, localPageUrl));
        }

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(d -> clearDialogReference(dialog));
        currentDialogRef = new WeakReference<>(dialog);
        dialog.show();
        return true;
    }

    public void downloadUpdate() {
        final DownloadSnapshot snapshot;
        synchronized (lock) {
            if (downloading || update == null || TextUtils.isEmpty(apkUrl)) {
                return;
            }
            if (hasDownloadedUpdate(update.versionCode)) {
                Activity activity = getInstallActivity();
                if (activity != null) {
                    installDownloadedUpdate(activity);
                }
                return;
            }
            downloading = true;
            cancelDownload = false;
            downloadedBytes = 0;
            totalBytes = 0;
            snapshot = new DownloadSnapshot(update.version, update.versionCode, apkUrl, sha256, buildFileName(fileName, update.version));
        }
        postGlobalNotification(NotificationCenter.appUpdateLoading);
        Thread thread = new Thread(() -> performDownload(snapshot), "fluffy-update-download");
        synchronized (lock) {
            downloadThread = thread;
        }
        thread.start();
    }

    public void cancelDownloadingUpdate() {
        synchronized (lock) {
            cancelDownload = true;
            if (activeConnection != null) {
                activeConnection.disconnect();
            }
            if (downloadThread != null) {
                downloadThread.interrupt();
            }
        }
    }

    public boolean isDownloadingUpdate() {
        synchronized (lock) {
            return downloading;
        }
    }

    public float getDownloadingUpdateProgress() {
        synchronized (lock) {
            if (!downloading || totalBytes <= 0) {
                return 0.0f;
            }
            return Math.min(1.0f, downloadedBytes / (float) totalBytes);
        }
    }

    public BetaUpdate getUpdate() {
        synchronized (lock) {
            return update;
        }
    }

    public File getDownloadedUpdateFile() {
        synchronized (lock) {
            if (downloadedFile != null && downloadedFile.exists()) {
                return downloadedFile;
            }
            return null;
        }
    }

    public boolean checkApkInstallPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
            if (context instanceof Activity) {
                try {
                    context.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + context.getPackageName())));
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            return false;
        }
        return true;
    }

    public boolean installDownloadedUpdate(Activity activity) {
        File file = getDownloadedUpdateFile();
        if (file == null || !file.exists()) {
            return false;
        }
        if (!checkApkInstallPermissions(activity)) {
            AlertsCreator.createApkRestrictedDialog(activity, null).show();
            return false;
        }
        return AndroidUtilities.openForView(file, file.getName(), "application/vnd.android.package-archive", activity, null, false);
    }

    private boolean shouldCheckNow() {
        long delaySeconds = 60L * 60L * 12L;
        try {
            delaySeconds = Math.max(60L, org.telegram.messenger.MessagesController.getInstance(UserConfig.selectedAccount).updateCheckDelay);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return Math.abs(System.currentTimeMillis() - SharedConfig.lastUpdateCheckTime) >= delaySeconds * 1000L;
    }

    private void loadState() {
        synchronized (lock) {
            String version = preferences.getString(KEY_VERSION, null);
            int versionCode = preferences.getInt(KEY_VERSION_CODE, 0);
            if (!TextUtils.isEmpty(version) && versionCode > 0) {
                update = new BetaUpdate(version, versionCode, preferences.getString(KEY_CHANGELOG, null));
                apkUrl = preferences.getString(KEY_APK_URL, null);
                pageUrl = preferences.getString(KEY_PAGE_URL, null);
                sha256 = preferences.getString(KEY_SHA256, null);
                fileName = preferences.getString(KEY_FILE_NAME, null);
            }
            String downloadedPath = preferences.getString(KEY_DOWNLOADED_FILE, null);
            int downloadedVersionCode = preferences.getInt(KEY_DOWNLOADED_VERSION_CODE, 0);
            if (!TextUtils.isEmpty(downloadedPath) && update != null && downloadedVersionCode == update.versionCode) {
                File file = new File(downloadedPath);
                if (file.exists()) {
                    downloadedFile = file;
                }
            }
            if (update != null && !update.higherThan(getCurrentInstalledUpdate())) {
                clearStoredUpdate(true);
            }
        }
    }

    private void applyParsedUpdate(ParsedUpdate parsed) {
        if (!parsed.update.higherThan(getCurrentInstalledUpdate())) {
            clearStoredUpdate(true);
            return;
        }
        synchronized (lock) {
            boolean sameUpdate = update != null
                && update.versionCode == parsed.update.versionCode
                && TextUtils.equals(update.version, parsed.update.version)
                && TextUtils.equals(apkUrl, parsed.apkUrl);
            update = parsed.update;
            apkUrl = parsed.apkUrl;
            pageUrl = parsed.pageUrl;
            sha256 = parsed.sha256;
            fileName = parsed.fileName;
            if (!sameUpdate && downloadedFile != null) {
                deleteFile(downloadedFile);
                downloadedFile = null;
            }
            persistState();
        }
        postGlobalNotification(NotificationCenter.appUpdateAvailable);
    }

    private void clearStoredUpdate(boolean deleteDownloadedFile) {
        synchronized (lock) {
            update = null;
            apkUrl = null;
            pageUrl = null;
            sha256 = null;
            fileName = null;
            if (deleteDownloadedFile && downloadedFile != null) {
                deleteFile(downloadedFile);
            }
            downloadedFile = null;
            persistState();
        }
        postGlobalNotification(NotificationCenter.appUpdateAvailable);
    }

    private void persistState() {
        SharedPreferences.Editor editor = preferences.edit();
        if (update != null) {
            editor.putString(KEY_VERSION, update.version);
            editor.putInt(KEY_VERSION_CODE, update.versionCode);
            editor.putString(KEY_CHANGELOG, update.changelog);
            editor.putString(KEY_APK_URL, apkUrl);
            editor.putString(KEY_PAGE_URL, pageUrl);
            editor.putString(KEY_SHA256, sha256);
            editor.putString(KEY_FILE_NAME, fileName);
        } else {
            editor.remove(KEY_VERSION);
            editor.remove(KEY_VERSION_CODE);
            editor.remove(KEY_CHANGELOG);
            editor.remove(KEY_APK_URL);
            editor.remove(KEY_PAGE_URL);
            editor.remove(KEY_SHA256);
            editor.remove(KEY_FILE_NAME);
        }
        if (downloadedFile != null && downloadedFile.exists() && update != null) {
            editor.putString(KEY_DOWNLOADED_FILE, downloadedFile.getAbsolutePath());
            editor.putInt(KEY_DOWNLOADED_VERSION_CODE, update.versionCode);
        } else {
            editor.remove(KEY_DOWNLOADED_FILE);
            editor.remove(KEY_DOWNLOADED_VERSION_CODE);
        }
        editor.apply();
    }

    private void performDownload(DownloadSnapshot snapshot) {
        File updatesDir = new File(ApplicationLoader.getFilesDirFixed("cache"), "updates");
        updatesDir.mkdirs();
        File targetFile = new File(updatesDir, snapshot.fileName);
        File tempFile = new File(updatesDir, snapshot.fileName + ".download");

        deleteFile(tempFile);
        HttpURLConnection connection = null;
        boolean success = false;
        try {
            connection = (HttpURLConnection) new URL(snapshot.apkUrl).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildVars.BUILD_VERSION_STRING);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Unexpected update download response: " + responseCode);
            }
            synchronized (lock) {
                activeConnection = connection;
                totalBytes = Math.max(0, connection.getContentLengthLong());
            }
            try (InputStream input = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream output = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[32 * 1024];
                long total = 0;
                while (true) {
                    int read = input.read(buffer);
                    if (read == -1) {
                        break;
                    }
                    if (shouldAbortDownload()) {
                        throw new InterruptedIOException("Update download cancelled");
                    }
                    output.write(buffer, 0, read);
                    total += read;
                    synchronized (lock) {
                        downloadedBytes = total;
                    }
                }
                output.getFD().sync();
            }
            if (!TextUtils.isEmpty(snapshot.sha256) && !verifySha256(tempFile, snapshot.sha256)) {
                throw new IOException("Downloaded update hash mismatch");
            }
            deleteFile(targetFile);
            if (!tempFile.renameTo(targetFile)) {
                throw new IOException("Failed to move update file into place");
            }
            synchronized (lock) {
                downloadedFile = targetFile;
                persistState();
            }
            success = true;
        } catch (Throwable t) {
            if (!(t instanceof InterruptedIOException)) {
                FileLog.e(t);
            }
        } finally {
            if (!success) {
                deleteFile(tempFile);
            }
            if (connection != null) {
                connection.disconnect();
            }
            synchronized (lock) {
                activeConnection = null;
                downloadThread = null;
                downloading = false;
                cancelDownload = false;
                if (!success) {
                    downloadedBytes = 0;
                    totalBytes = 0;
                }
            }
            postGlobalNotifications(NotificationCenter.appUpdateAvailable, NotificationCenter.appUpdateLoading);
            if (success) {
                AndroidUtilities.runOnUIThread(() -> {
                    Activity activity = getInstallActivity();
                    if (activity != null) {
                        installDownloadedUpdate(activity);
                    }
                });
            } else {
                AndroidUtilities.runOnUIThread(() -> Toast.makeText(ApplicationLoader.applicationContext, LocaleController.getString(R.string.FluffyUpdateDownloadFailed), Toast.LENGTH_SHORT).show());
            }
        }
    }

    private boolean shouldAbortDownload() {
        synchronized (lock) {
            return cancelDownload || Thread.currentThread().isInterrupted();
        }
    }

    private boolean verifySha256(File file, String expectedHash) throws IOException {
        String normalizedExpected = expectedHash.trim().replace(":", "").toUpperCase();
        try (InputStream input = new BufferedInputStream(new java.io.FileInputStream(file))) {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[32 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            String actualHash = Utilities.bytesToHex(digest.digest());
            return normalizedExpected.equals(actualHash);
        } catch (Exception e) {
            throw new IOException("Failed to verify update checksum", e);
        }
    }

    private ParsedUpdate parseManifest(String response) throws Exception {
        JSONObject root = new JSONObject(response);
        JSONObject updateObject = root.optJSONObject("release");
        if (updateObject == null) {
            updateObject = root;
        }

        String version = normalizeVersion(firstString(updateObject, "version", "versionName", "tag_name", "name"));
        int versionCode = firstInt(updateObject, "versionCode", "version_code", "buildVersionCode", "build_number", "build");
        String changelog = firstString(updateObject, "changelog", "releaseNotes", "body", "notes");
        String parsedApkUrl = firstString(updateObject, "apkUrl", "apk_url", "downloadUrl", "download_url", "browser_download_url");
        String parsedPageUrl = firstString(updateObject, "pageUrl", "page_url", "releaseUrl", "release_url", "htmlUrl", "html_url");
        String parsedSha256 = firstString(updateObject, "sha256", "sha_256", "apkSha256", "apk_sha256");
        String parsedFileName = firstString(updateObject, "fileName", "file_name", "assetName", "asset_name");

        JSONArray assets = updateObject.optJSONArray("assets");
        if (assets != null) {
            for (int i = 0; i < assets.length(); i++) {
                JSONObject asset = assets.optJSONObject(i);
                if (asset == null) {
                    continue;
                }
                String assetName = firstString(asset, "name", "fileName", "file_name");
                String assetUrl = firstString(asset, "browser_download_url", "downloadUrl", "download_url", "url");
                String contentType = firstString(asset, "content_type", "contentType");
                if (TextUtils.isEmpty(parsedPageUrl)) {
                    parsedPageUrl = firstString(asset, "html_url");
                }
                if ((assetName != null && assetName.endsWith(".apk")) || "application/vnd.android.package-archive".equals(contentType)) {
                    if (TextUtils.isEmpty(parsedApkUrl)) {
                        parsedApkUrl = assetUrl;
                    }
                    if (TextUtils.isEmpty(parsedFileName)) {
                        parsedFileName = assetName;
                    }
                    if (TextUtils.isEmpty(parsedSha256)) {
                        parsedSha256 = firstString(asset, "sha256", "sha_256");
                    }
                    break;
                }
            }
        }

        if (TextUtils.isEmpty(parsedPageUrl)) {
            parsedPageUrl = getDefaultPageUrl();
        }
        if (TextUtils.isEmpty(parsedFileName) && !TextUtils.isEmpty(parsedApkUrl)) {
            parsedFileName = Uri.parse(parsedApkUrl).getLastPathSegment();
        }
        if (TextUtils.isEmpty(version) || versionCode <= 0 || (TextUtils.isEmpty(parsedApkUrl) && TextUtils.isEmpty(parsedPageUrl))) {
            return null;
        }
        return new ParsedUpdate(new BetaUpdate(version, versionCode, changelog), parsedApkUrl, parsedPageUrl, parsedSha256, buildFileName(parsedFileName, version));
    }

    private String fetchUrl(String url) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildVars.BUILD_VERSION_STRING);
            connection.connect();
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("Unexpected update manifest response: " + responseCode);
            }
            StringBuilder result = new StringBuilder();
            try (InputStream input = new BufferedInputStream(connection.getInputStream())) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    result.append(new String(buffer, 0, read, java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            return result.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private BetaUpdate getCurrentInstalledUpdate() {
        String versionName = BuildVars.BUILD_VERSION_STRING;
        int versionCode = 0;
        try {
            PackageInfo packageInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            versionName = packageInfo.versionName;
            versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? (int) packageInfo.getLongVersionCode() : packageInfo.versionCode;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new BetaUpdate(versionName, versionCode, null);
    }

    private boolean hasDownloadedUpdate(int expectedVersionCode) {
        synchronized (lock) {
            return downloadedFile != null && downloadedFile.exists() && update != null && update.versionCode == expectedVersionCode;
        }
    }

    private String getResolvedPageUrl() {
        return TextUtils.isEmpty(pageUrl) ? getDefaultPageUrl() : pageUrl;
    }

    private String getManifestUrl() {
        return isBetaChannel() ? BuildConfig.FLUFFY_BETA_UPDATE_MANIFEST_URL : BuildConfig.FLUFFY_UPDATE_MANIFEST_URL;
    }

    private String getDefaultPageUrl() {
        return isBetaChannel() ? BuildConfig.FLUFFY_BETA_UPDATE_PAGE_URL : BuildConfig.FLUFFY_UPDATE_PAGE_URL;
    }

    private boolean isBetaChannel() {
        return BuildConfig.APPLICATION_ID.endsWith(".beta");
    }

    private void openReleasePage(Context context, String url) {
        if (!TextUtils.isEmpty(url)) {
            Browser.openUrl(context, url);
        }
    }

    private void postGlobalNotification(int id) {
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(id));
    }

    private void postGlobalNotifications(int... ids) {
        AndroidUtilities.runOnUIThread(() -> {
            for (int id : ids) {
                NotificationCenter.getGlobalInstance().postNotificationName(id);
            }
        });
    }

    private void dismissCurrentDialog() {
        AlertDialog dialog = currentDialogRef != null ? currentDialogRef.get() : null;
        if (dialog != null) {
            dialog.dismiss();
        }
        currentDialogRef = null;
    }

    private void clearDialogReference(AlertDialog dialog) {
        AlertDialog currentDialog = currentDialogRef != null ? currentDialogRef.get() : null;
        if (currentDialog == dialog) {
            currentDialogRef = null;
        }
    }

    private Activity getInstallActivity() {
        Activity activity = installActivityRef != null ? installActivityRef.get() : null;
        if (activity == null) {
            return null;
        }
        if (activity.isFinishing()) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
            return null;
        }
        return activity;
    }

    private String buildFileName(String candidate, String version) {
        String result = candidate;
        if (TextUtils.isEmpty(result)) {
            result = "fluffyGram-" + version + ".apk";
        }
        if (!result.endsWith(".apk")) {
            result += ".apk";
        }
        return result;
    }

    private String normalizeVersion(String version) {
        if (TextUtils.isEmpty(version)) {
            return null;
        }
        String trimmed = version.trim();
        if (trimmed.startsWith("v") || trimmed.startsWith("V")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
    }

    private String firstString(JSONObject object, String... keys) {
        for (String key : keys) {
            String value = object.optString(key, null);
            if (!TextUtils.isEmpty(value) && !"null".equalsIgnoreCase(value)) {
                return value;
            }
        }
        return null;
    }

    private int firstInt(JSONObject object, String... keys) {
        for (String key : keys) {
            Object value = object.opt(key);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            if (value instanceof String && !TextUtils.isEmpty((String) value)) {
                try {
                    return Integer.parseInt((String) value);
                } catch (Exception ignore) {
                }
            }
        }
        return 0;
    }

    private void deleteFile(File file) {
        if (file != null && file.exists() && !file.delete()) {
            FileLog.d("failed to delete update file " + file);
        }
    }

    private static final class ParsedUpdate {
        final BetaUpdate update;
        final String apkUrl;
        final String pageUrl;
        final String sha256;
        final String fileName;

        ParsedUpdate(BetaUpdate update, String apkUrl, String pageUrl, String sha256, String fileName) {
            this.update = update;
            this.apkUrl = apkUrl;
            this.pageUrl = pageUrl;
            this.sha256 = sha256;
            this.fileName = fileName;
        }
    }

    private static final class DownloadSnapshot {
        final String version;
        final int versionCode;
        final String apkUrl;
        final String sha256;
        final String fileName;

        DownloadSnapshot(String version, int versionCode, String apkUrl, String sha256, String fileName) {
            this.version = version;
            this.versionCode = versionCode;
            this.apkUrl = apkUrl;
            this.sha256 = sha256;
            this.fileName = fileName;
        }
    }
}
