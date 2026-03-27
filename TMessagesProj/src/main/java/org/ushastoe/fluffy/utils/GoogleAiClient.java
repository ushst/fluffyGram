package org.ushastoe.fluffy.utils;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GoogleAiClient {

    private static final String DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String MODEL_PLACEHOLDER = "{model}";

    public interface ResultCallback {
        void onComplete(String responseText, String errorMessage);
    }

    public interface ModelsCallback {
        void onComplete(ArrayList<ModelInfo> models, String errorMessage);
    }

    public static final class ModelInfo {
        public final String name;
        public final String displayName;
        public final String description;
        public final ArrayList<String> supportedGenerationMethods;
        public final int inputTokenLimit;
        public final int outputTokenLimit;

        public ModelInfo(String name, String displayName, String description, ArrayList<String> supportedGenerationMethods,
                int inputTokenLimit, int outputTokenLimit) {
            this.name = name;
            this.displayName = displayName;
            this.description = description;
            this.supportedGenerationMethods = supportedGenerationMethods;
            this.inputTokenLimit = inputTokenLimit;
            this.outputTokenLimit = outputTokenLimit;
        }

        public boolean supportsGenerateContent() {
            if (supportedGenerationMethods == null) {
                return false;
            }
            for (int i = 0; i < supportedGenerationMethods.size(); i++) {
                if ("generateContent".equals(supportedGenerationMethods.get(i))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 30000;

    private GoogleAiClient() {
    }

    public static void generateContent(String baseUrl, String apiKey, String model, String prompt, ResultCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            String responseText = null;
            String errorText = null;
            HttpURLConnection connection = null;
            try {
                String normalizedModel = normalizeModel(model);
                String url = buildRequestUrl(baseUrl, normalizedModel);
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setDoOutput(true);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setRequestProperty("x-goog-api-key", apiKey);

                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);
                parts.put(textPart);
                content.put("parts", parts);
                contents.put(content);
                body.put("contents", contents);

                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(payload);
                }

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                String rawText = readAll(stream);
                if (code >= 200 && code < 300) {
                    JSONObject json = new JSONObject(rawText);
                    responseText = extractResponseText(json);
                    if (TextUtils.isEmpty(responseText)) {
                        errorText = "Empty response";
                    }
                } else {
                    errorText = extractErrorText(rawText, code);
                }
            } catch (Exception e) {
                FileLog.e(e);
                errorText = e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            String finalResponseText = responseText;
            String finalErrorText = errorText;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onComplete(finalResponseText, finalErrorText);
                }
            });
        });
    }

    public static void validateApiKey(String baseUrl, String apiKey, ResultCallback callback) {
        validateModel(baseUrl, apiKey, "gemini-2.5-flash", callback);
    }

    public static void validateModel(String baseUrl, String apiKey, String model, ResultCallback callback) {
        generateContent(baseUrl, apiKey, model, "Reply with OK.", callback);
    }

    public static void listModels(String baseUrl, String apiKey, ModelsCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<ModelInfo> models = null;
            String errorText = null;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(buildModelsListUrl(baseUrl)).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("x-goog-api-key", apiKey);

                int code = connection.getResponseCode();
                InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
                String rawText = readAll(stream);
                if (code >= 200 && code < 300) {
                    models = extractModels(new JSONObject(rawText));
                } else {
                    errorText = extractErrorText(rawText, code);
                }
            } catch (Exception e) {
                FileLog.e(e);
                errorText = e.getMessage();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            ArrayList<ModelInfo> finalModels = models;
            String finalErrorText = errorText;
            AndroidUtilities.runOnUIThread(() -> {
                if (callback != null) {
                    callback.onComplete(finalModels, finalErrorText);
                }
            });
        });
    }

    private static String normalizeModel(String model) {
        String value = model == null ? "" : model.trim();
        if (TextUtils.isEmpty(value)) {
            value = "gemini-2.5-flash";
        }
        if (!value.startsWith("models/")) {
            value = "models/" + value;
        }
        return value;
    }

    private static String buildRequestUrl(String baseUrl, String normalizedModel) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (TextUtils.isEmpty(value)) {
            value = DEFAULT_API_BASE_URL;
        }
        if (value.contains(MODEL_PLACEHOLDER)) {
            return value.replace(MODEL_PLACEHOLDER, normalizedModel);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.contains(":generateContent")) {
            return value;
        }
        if (value.endsWith("/v1beta")) {
            return value + "/" + normalizedModel + ":generateContent";
        }
        if (value.contains("/v1beta/")) {
            return value.endsWith("/") ? value + normalizedModel + ":generateContent" : value + "/" + normalizedModel + ":generateContent";
        }
        return value + "/v1beta/" + normalizedModel + ":generateContent";
    }

    private static String buildModelsListUrl(String baseUrl) {
        String value = baseUrl == null ? "" : baseUrl.trim();
        if (TextUtils.isEmpty(value)) {
            value = DEFAULT_API_BASE_URL;
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.contains(MODEL_PLACEHOLDER)) {
            value = value.substring(0, value.indexOf(MODEL_PLACEHOLDER));
        }
        int generateContentIndex = value.indexOf(":generateContent");
        if (generateContentIndex >= 0) {
            value = value.substring(0, generateContentIndex);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        int modelsIndex = value.indexOf("/models/");
        if (modelsIndex >= 0) {
            return value.substring(0, modelsIndex) + "/models";
        }
        if (value.endsWith("/models")) {
            return value;
        }
        if (value.endsWith("/v1beta")) {
            return value + "/models";
        }
        if (value.contains("/v1beta/")) {
            return value + "/models";
        }
        return value + "/v1beta/models";
    }

    private static ArrayList<ModelInfo> extractModels(JSONObject response) {
        ArrayList<ModelInfo> result = new ArrayList<>();
        JSONArray models = response.optJSONArray("models");
        if (models == null) {
            return result;
        }
        for (int i = 0; i < models.length(); i++) {
            JSONObject item = models.optJSONObject(i);
            if (item == null) {
                continue;
            }
            ArrayList<String> methods = new ArrayList<>();
            JSONArray supportedMethods = item.optJSONArray("supportedGenerationMethods");
            if (supportedMethods != null) {
                for (int j = 0; j < supportedMethods.length(); j++) {
                    String method = supportedMethods.optString(j);
                    if (!TextUtils.isEmpty(method)) {
                        methods.add(method);
                    }
                }
            }
            result.add(new ModelInfo(
                    item.optString("name"),
                    item.optString("displayName"),
                    item.optString("description"),
                    methods,
                    item.optInt("inputTokenLimit"),
                    item.optInt("outputTokenLimit")));
        }
        return result;
    }

    private static String extractResponseText(JSONObject response) {
        JSONArray candidates = response.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < candidates.length(); i++) {
            JSONObject candidate = candidates.optJSONObject(i);
            if (candidate == null) {
                continue;
            }
            JSONObject content = candidate.optJSONObject("content");
            if (content == null) {
                continue;
            }
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null) {
                continue;
            }
            for (int j = 0; j < parts.length(); j++) {
                JSONObject part = parts.optJSONObject(j);
                if (part == null) {
                    continue;
                }
                String text = part.optString("text");
                if (TextUtils.isEmpty(text)) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(text.trim());
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private static String extractErrorText(String rawText, int code) {
        if (!TextUtils.isEmpty(rawText)) {
            try {
                JSONObject json = new JSONObject(rawText);
                JSONObject error = json.optJSONObject("error");
                if (error != null) {
                    String message = error.optString("message");
                    if (!TextUtils.isEmpty(message)) {
                        return message;
                    }
                }
                String message = json.optString("message");
                if (!TextUtils.isEmpty(message)) {
                    return message;
                }
            } catch (Exception ignore) {
                return rawText;
            }
            return rawText;
        }
        return "HTTP " + code;
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
