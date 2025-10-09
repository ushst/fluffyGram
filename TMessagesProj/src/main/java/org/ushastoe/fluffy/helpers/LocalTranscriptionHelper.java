package org.ushastoe.fluffy.helpers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public final class LocalTranscriptionHelper implements NotificationCenter.NotificationCenterDelegate {
    private static final String TARGET_PACKAGE = "com.ushst.voskhelper";
    private static final String TARGET_ACTIVITY = "com.ushst.voskhelper.TranscribeActivity";
    private static final long TIMEOUT_MS = 120_000L;
    private static final long RETRY_DELAY_MS = 500L;

    public interface Callback {
        void onSuccess(Result result);
        void onError(String errorCode);
    }

    public static final class Result {
        public final String transcription;
        public final Bundle metadata;

        Result(String transcription, Bundle metadata) {
            this.transcription = transcription;
            this.metadata = metadata;
        }
    }

    private static final AtomicInteger NEXT_REQUEST_CODE = new AtomicInteger(0x5300);
    private static final ConcurrentHashMap<Integer, LocalTranscriptionHelper> SESSIONS = new ConcurrentHashMap<>();

    private final int requestCode;
    private final LaunchActivity activity;
    private final Uri uri;
    private final String mimeType;
    private final long fileSize;
    private final Callback callback;

    private boolean finished;
    private int retriesRemaining = 1;
    private long startTime;

    private LocalTranscriptionHelper(LaunchActivity activity, File file, Uri uri, String mimeType, Callback callback) {
        this.activity = activity;
        this.uri = uri;
        this.mimeType = mimeType;
        this.fileSize = file.length();
        this.callback = callback;
        this.requestCode = NEXT_REQUEST_CODE.getAndIncrement();
    }

    public static void request(MessageObject messageObject, String path, Callback callback) {
        LaunchActivity activity = LaunchActivity.instance;
        if (activity == null) {
            callback.onError("no_activity");
            return;
        }
        if (TextUtils.isEmpty(path)) {
            callback.onError("invalid_uri");
            return;
        }
        File file = new File(path);
        if (!file.exists() || file.length() == 0) {
            callback.onError("invalid_uri");
            return;
        }
        String mimeType = messageObject != null ? messageObject.getMimeType() : null;
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "audio/*";
        }
        Uri uri = FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", file);
        if (!verifyUriAccessible(uri)) {
            FileLog.d("Local transcription invalid_uri metadata: uri=" + uri + ", mimeType=" + mimeType + ", fileSize=" + file.length() + ", latency=0ms");
            callback.onError("invalid_uri");
            return;
        }

        LocalTranscriptionHelper session = new LocalTranscriptionHelper(activity, file, uri, mimeType, callback);
        SESSIONS.put(session.requestCode, session);
        NotificationCenter.getGlobalInstance().addObserver(session, NotificationCenter.onActivityResultReceived);
        session.start();
    }

    private static boolean verifyUriAccessible(Uri uri) {
        if (uri == null) {
            return false;
        }
        try (InputStream ignored = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri)) {
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private void start() {
        AndroidUtilities.runOnUIThread(() -> sendIntent(false));
    }

    private void sendIntent(boolean isRetry) {
        if (finished) {
            return;
        }
        startTime = SystemClock.elapsedRealtime();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setComponent(new ComponentName(TARGET_PACKAGE, TARGET_ACTIVITY));
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.grantUriPermission(TARGET_PACKAGE, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            activity.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            finishWithError("app_missing");
            return;
        } catch (Exception e) {
            FileLog.e(e);
            finishWithError("invalid_uri");
            return;
        }
        AndroidUtilities.runOnUIThread(this::checkTimeout, TIMEOUT_MS);
    }

    private void checkTimeout() {
        if (finished) {
            return;
        }
        finishWithError("timeout");
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id != NotificationCenter.onActivityResultReceived || finished) {
            return;
        }
        int req = (Integer) args[0];
        if (req != requestCode) {
            return;
        }
        int resultCode = (Integer) args[1];
        Intent data = (Intent) args[2];
        if (resultCode == Activity.RESULT_OK) {
            String transcription = data != null ? data.getStringExtra("transcription") : null;
            if (transcription == null) {
                transcription = "";
            }
            Bundle metadata = extractMetadata(data);
            logMetadata("success");
            finish();
            Result result = new Result(transcription, metadata);
            AndroidUtilities.runOnUIThread(() -> callback.onSuccess(result));
            return;
        }
        String error = data != null ? data.getStringExtra("error") : null;
        if (TextUtils.isEmpty(error)) {
            error = "cancelled";
        }
        if ("io_error".equals(error) && retriesRemaining > 0) {
            retriesRemaining--;
            AndroidUtilities.runOnUIThread(() -> sendIntent(true), RETRY_DELAY_MS);
            return;
        }
        finishWithError(error);
    }

    private void finishWithError(String error) {
        logMetadata(error);
        finish();
        AndroidUtilities.runOnUIThread(() -> callback.onError(error));
    }

    private void logMetadata(String status) {
        long latency = SystemClock.elapsedRealtime() - startTime;
        FileLog.d("Local transcription " + status + " metadata: uri=" + uri + ", mimeType=" + mimeType + ", fileSize=" + fileSize + ", latency=" + latency + "ms");
    }

    private static Bundle extractMetadata(Intent data) {
        if (data == null) {
            return null;
        }
        Bundle extras = data.getBundleExtra("metadata");
        if (extras != null) {
            return new Bundle(extras);
        }
        Bundle allExtras = data.getExtras();
        return allExtras != null ? new Bundle(allExtras) : null;
    }

    private void finish() {
        if (finished) {
            return;
        }
        finished = true;
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.onActivityResultReceived);
        SESSIONS.remove(requestCode);
        try {
            activity.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static void showError(String errorCode) {
        String message;
        switch (errorCode) {
            case "invalid_uri":
                message = LocaleController.getString("FG_LocalTranscribeInvalidUri", R.string.FG_LocalTranscribeInvalidUri);
                break;
            case "timeout":
                message = LocaleController.getString("FG_LocalTranscribeTimeout", R.string.FG_LocalTranscribeTimeout);
                break;
            case "io_error":
                message = LocaleController.getString("FG_LocalTranscribeIoError", R.string.FG_LocalTranscribeIoError);
                break;
            case "app_missing":
                message = LocaleController.getString("FG_LocalTranscribeAppMissing", R.string.FG_LocalTranscribeAppMissing);
                break;
            case "cancelled":
                message = LocaleController.getString("FG_LocalTranscribeCancelled", R.string.FG_LocalTranscribeCancelled);
                break;
            default:
                message = LocaleController.getString(R.string.ErrorOccurred);
                break;
        }
        if (TextUtils.isEmpty(message)) {
            return;
        }
        var fragment = LaunchActivity.getSafeLastFragment();
        if (!BulletinFactory.canShowBulletin(fragment)) {
            return;
        }
        BulletinFactory.of(fragment).createErrorBulletin(message).show();
    }
}
