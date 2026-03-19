package org.ushastoe.fluffy.sync;

import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class FluffySyncBackendClient {

    public interface JsonCallback {
        void onComplete(JSONObject response, String errorMessage);
    }

    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 8000;

    private FluffySyncBackendClient() {
    }

    public static void postJson(String url, JSONObject body, String bearerToken, JsonCallback callback) {
        request("POST", url, body, bearerToken, callback);
    }

    public static void getJson(String url, String bearerToken, JsonCallback callback) {
        request("GET", url, null, bearerToken, callback);
    }

    private static void request(String method, String url, JSONObject body, String bearerToken, JsonCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            JSONObject response = null;
            String errorMessage = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                if (!TextUtils.isEmpty(bearerToken)) {
                    connection.setRequestProperty("Authorization", "Bearer " + bearerToken);
                }
                if (body != null) {
                    byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    connection.setFixedLengthStreamingMode(payload.length);
                    try (OutputStream outputStream = connection.getOutputStream()) {
                        outputStream.write(payload);
                    }
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                String text = readAll(stream);
                if (!TextUtils.isEmpty(text)) {
                    try {
                        response = new JSONObject(text);
                    } catch (Exception e) {
                        if (code >= 200 && code < 300) {
                            throw e;
                        }
                        errorMessage = text;
                    }
                }
                if (code < 200 || code >= 300) {
                    if (TextUtils.isEmpty(errorMessage) && response != null) {
                        errorMessage = response.optString("message");
                        if (TextUtils.isEmpty(errorMessage)) {
                            errorMessage = response.optString("error");
                        }
                    }
                    if (TextUtils.isEmpty(errorMessage)) {
                        errorMessage = "HTTP " + code;
                    }
                    response = null;
                }
            } catch (Exception e) {
                FileLog.e(e);
                errorMessage = e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            JSONObject finalResponse = response;
            String finalErrorMessage = errorMessage;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onComplete(finalResponse, finalErrorMessage);
                }
            });
        });
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
}
