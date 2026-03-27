package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.UUID;

public final class GoogleAiSettingsPatch {

    public static final String PREFS_NAME = "fluffy_google_ai_settings";

    private static final String KEY_API_KEY = "google_ai_api_key";
    private static final String KEY_ENABLED = "google_ai_enabled";
    private static final String KEY_API_KEY_VALIDATED = "google_ai_api_key_validated";
    private static final String KEY_API_BASE_URL = "google_ai_api_base_url";
    private static final String KEY_MODEL = "google_ai_model";
    private static final String KEY_PROMPT_PRESETS_JSON = "google_ai_prompt_presets_json";
    private static final String KEY_COMPOSE_PROMPT_PRESETS_JSON = "google_ai_compose_prompt_presets_json";

    private static final String LEGACY_KEY_PROMPT_LIBRARY_LANGUAGE = "google_ai_prompt_library_language";
    private static final String LEGACY_KEY_TRANSLATE_PROMPT = "google_ai_translate_prompt";
    private static final String LEGACY_KEY_EXPLAIN_PROMPT = "google_ai_explain_prompt";
    private static final String LEGACY_KEY_IDENTIFY_PROMPT = "google_ai_identify_prompt";
    private static final String LEGACY_KEY_CUSTOM_PROMPT = "google_ai_custom_prompt";

    private static final String DEFAULT_MODEL = "gemini-2.5-flash";
    private static final String DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com";

    public static final class PromptPreset {
        public final String id;
        public final String title;
        public final String prompt;

        public PromptPreset(String id, String title, String prompt) {
            this.id = id;
            this.title = title;
            this.prompt = prompt;
        }
    }

    private GoogleAiSettingsPatch() {
    }

    private static SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static String getApiKey() {
        return getPreferences().getString(KEY_API_KEY, "");
    }

    public static boolean isEnabled() {
        return getPreferences().getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        getPreferences().edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void setApiKey(String value) {
        getPreferences().edit().putString(KEY_API_KEY, sanitize(value)).apply();
    }

    public static boolean hasApiKey() {
        return !TextUtils.isEmpty(getApiKey());
    }

    public static boolean isApiKeyValidated() {
        return hasApiKey() && getPreferences().getBoolean(KEY_API_KEY_VALIDATED, false);
    }

    public static void setApiKeyValidated(boolean validated) {
        getPreferences().edit().putBoolean(KEY_API_KEY_VALIDATED, validated).apply();
    }

    public static void clearApiKey() {
        getPreferences().edit()
                .putString(KEY_API_KEY, "")
                .putBoolean(KEY_API_KEY_VALIDATED, false)
                .apply();
    }

    public static String getApiBaseUrl() {
        String value = sanitize(getPreferences().getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL));
        return TextUtils.isEmpty(value) ? DEFAULT_API_BASE_URL : value;
    }

    public static void setApiBaseUrl(String value) {
        String sanitized = sanitize(value);
        getPreferences().edit().putString(KEY_API_BASE_URL, TextUtils.isEmpty(sanitized) ? DEFAULT_API_BASE_URL : sanitized).apply();
    }

    public static String getModel() {
        String value = sanitize(getPreferences().getString(KEY_MODEL, DEFAULT_MODEL));
        return TextUtils.isEmpty(value) ? DEFAULT_MODEL : value;
    }

    public static void setModel(String value) {
        String sanitized = sanitize(value);
        getPreferences().edit().putString(KEY_MODEL, TextUtils.isEmpty(sanitized) ? DEFAULT_MODEL : sanitized).apply();
    }

    public static ArrayList<PromptPreset> getPromptPresets() {
        SharedPreferences preferences = getPreferences();
        String raw = preferences.getString(KEY_PROMPT_PRESETS_JSON, null);
        if (raw == null) {
            ArrayList<PromptPreset> migrated = migrateLegacyPromptPresets(preferences);
            savePromptPresetsInternal(preferences, KEY_PROMPT_PRESETS_JSON, migrated);
            return migrated;
        }
        return parsePromptPresets(raw);
    }

    public static ArrayList<PromptPreset> getComposePromptPresets() {
        SharedPreferences preferences = getPreferences();
        String raw = preferences.getString(KEY_COMPOSE_PROMPT_PRESETS_JSON, null);
        if (raw == null) {
            ArrayList<PromptPreset> defaults = createDefaultComposePromptPresets();
            savePromptPresetsInternal(preferences, KEY_COMPOSE_PROMPT_PRESETS_JSON, defaults);
            return defaults;
        }
        return parsePromptPresets(raw);
    }

    public static void upsertPromptPreset(String id, String title, String prompt) {
        ArrayList<PromptPreset> presets = getPromptPresets();
        upsertPromptPresetInternal(presets, id, title, prompt);
        savePromptPresets(presets);
    }

    public static void deletePromptPreset(String id) {
        if (TextUtils.isEmpty(id)) {
            return;
        }
        ArrayList<PromptPreset> presets = getPromptPresets();
        for (int i = presets.size() - 1; i >= 0; i--) {
            if (id.equals(presets.get(i).id)) {
                presets.remove(i);
            }
        }
        savePromptPresets(presets);
    }

    public static void upsertComposePromptPreset(String id, String title, String prompt) {
        ArrayList<PromptPreset> presets = getComposePromptPresets();
        upsertPromptPresetInternal(presets, id, title, prompt);
        saveComposePromptPresets(presets);
    }

    public static void deleteComposePromptPreset(String id) {
        if (TextUtils.isEmpty(id)) {
            return;
        }
        ArrayList<PromptPreset> presets = getComposePromptPresets();
        for (int i = presets.size() - 1; i >= 0; i--) {
            if (id.equals(presets.get(i).id)) {
                presets.remove(i);
            }
        }
        saveComposePromptPresets(presets);
    }

    public static void savePromptPresets(ArrayList<PromptPreset> presets) {
        savePromptPresetsInternal(getPreferences(), KEY_PROMPT_PRESETS_JSON, presets);
    }

    public static void saveComposePromptPresets(ArrayList<PromptPreset> presets) {
        savePromptPresetsInternal(getPreferences(), KEY_COMPOSE_PROMPT_PRESETS_JSON, presets);
    }

    private static void savePromptPresetsInternal(SharedPreferences preferences, String key, ArrayList<PromptPreset> presets) {
        JSONArray array = new JSONArray();
        if (presets != null) {
            for (int i = 0; i < presets.size(); i++) {
                PromptPreset preset = presets.get(i);
                if (preset == null || TextUtils.isEmpty(preset.title) || TextUtils.isEmpty(preset.prompt)) {
                    continue;
                }
                JSONObject object = new JSONObject();
                try {
                    object.put("id", TextUtils.isEmpty(preset.id) ? UUID.randomUUID().toString() : preset.id);
                    object.put("title", sanitize(preset.title));
                    object.put("prompt", sanitize(preset.prompt));
                    array.put(object);
                } catch (Exception ignore) {
                }
            }
        }
        preferences.edit().putString(key, array.toString()).apply();
    }

    private static void upsertPromptPresetInternal(ArrayList<PromptPreset> presets, String id, String title, String prompt) {
        String normalizedTitle = sanitize(title);
        String normalizedPrompt = sanitize(prompt);
        if (TextUtils.isEmpty(normalizedTitle) || TextUtils.isEmpty(normalizedPrompt)) {
            return;
        }
        String normalizedId = TextUtils.isEmpty(id) ? UUID.randomUUID().toString() : id;
        boolean replaced = false;
        for (int i = 0; i < presets.size(); i++) {
            if (normalizedId.equals(presets.get(i).id)) {
                presets.set(i, new PromptPreset(normalizedId, normalizedTitle, normalizedPrompt));
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            presets.add(new PromptPreset(normalizedId, normalizedTitle, normalizedPrompt));
        }
    }

    private static ArrayList<PromptPreset> parsePromptPresets(String raw) {
        ArrayList<PromptPreset> result = new ArrayList<>();
        if (TextUtils.isEmpty(raw)) {
            return result;
        }
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                String title = sanitize(object.optString("title"));
                String prompt = sanitize(object.optString("prompt"));
                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(prompt)) {
                    continue;
                }
                String id = sanitize(object.optString("id"));
                if (TextUtils.isEmpty(id)) {
                    id = UUID.randomUUID().toString();
                }
                result.add(new PromptPreset(id, title, prompt));
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    private static ArrayList<PromptPreset> migrateLegacyPromptPresets(SharedPreferences preferences) {
        ArrayList<PromptPreset> result = new ArrayList<>();
        addLegacyPrompt(result, "Translate", preferences.getString(LEGACY_KEY_TRANSLATE_PROMPT, getLegacyDefaultTranslatePrompt(preferences)));
        addLegacyPrompt(result, "Explain", preferences.getString(LEGACY_KEY_EXPLAIN_PROMPT, getLegacyDefaultExplainPrompt(preferences)));
        addLegacyPrompt(result, "What is this?", preferences.getString(LEGACY_KEY_IDENTIFY_PROMPT, getLegacyDefaultIdentifyPrompt(preferences)));
        addLegacyPrompt(result, "Custom prompt", preferences.getString(LEGACY_KEY_CUSTOM_PROMPT, ""));
        return result;
    }

    private static ArrayList<PromptPreset> createDefaultComposePromptPresets() {
        ArrayList<PromptPreset> result = new ArrayList<>();
        boolean ru = isRussianLocale();
        if (ru) {
            result.add(new PromptPreset(UUID.randomUUID().toString(), "Нейтрально",
                    "Ты пишешь готовый текст сообщения для пользователя. На основе идеи ниже напиши одно законченное сообщение в нейтральном, естественном и понятном стиле. Сохрани смысл, не добавляй пояснений от себя. Верни только итоговый текст сообщения."));
            result.add(new PromptPreset(UUID.randomUUID().toString(), "Дружелюбно",
                    "Ты пишешь готовый текст сообщения для пользователя. На основе идеи ниже напиши одно дружелюбное, тёплое и естественное сообщение. Тон должен быть живым, но без лишней фамильярности. Верни только итоговый текст сообщения."));
            result.add(new PromptPreset(UUID.randomUUID().toString(), "Официально",
                    "Ты пишешь готовый текст сообщения для пользователя. На основе идеи ниже напиши одно вежливое, собранное и официальное сообщение. Формулировки должны быть аккуратными и деловыми. Верни только итоговый текст сообщения."));
        } else {
            result.add(new PromptPreset(UUID.randomUUID().toString(), "Neutral",
                    "You are writing a ready-to-send message for the user. Based on the idea below, write one complete message in a neutral, natural and clear style. Preserve the meaning, do not add explanations. Return only the final message text."));
            result.add(new PromptPreset(UUID.randomUUID().toString(), "Friendly",
                    "You are writing a ready-to-send message for the user. Based on the idea below, write one friendly, warm and natural message. Keep the tone lively but not overly casual. Return only the final message text."));
            result.add(new PromptPreset(UUID.randomUUID().toString(), "Formal",
                    "You are writing a ready-to-send message for the user. Based on the idea below, write one polite, composed and formal message. Use careful, businesslike phrasing. Return only the final message text."));
        }
        return result;
    }

    private static boolean isRussianLocale() {
        String language = ApplicationLoader.applicationContext.getResources().getConfiguration().locale != null
                ? ApplicationLoader.applicationContext.getResources().getConfiguration().locale.getLanguage()
                : "";
        return language != null && language.toLowerCase().startsWith("ru");
    }

    private static void addLegacyPrompt(ArrayList<PromptPreset> result, String title, String prompt) {
        String normalizedPrompt = sanitize(prompt);
        if (TextUtils.isEmpty(normalizedPrompt)) {
            return;
        }
        result.add(new PromptPreset(UUID.randomUUID().toString(), title, normalizedPrompt));
    }

    private static String getLegacyDefaultTranslatePrompt(SharedPreferences preferences) {
        if (preferences.getInt(LEGACY_KEY_PROMPT_LIBRARY_LANGUAGE, 0) == 1) {
            return "Переведи это сообщение на язык текущего интерфейса пользователя. Сохрани исходный смысл и тон. Верни только перевод.";
        }
        return "Translate this message to the language of the current user interface. Preserve the original meaning and tone. Return only the translation.";
    }

    private static String getLegacyDefaultExplainPrompt(SharedPreferences preferences) {
        if (preferences.getInt(LEGACY_KEY_PROMPT_LIBRARY_LANGUAGE, 0) == 1) {
            return "Объясни это сообщение простыми словами и при необходимости добавь короткий контекст.";
        }
        return "Explain this message in simple words and add brief context if needed.";
    }

    private static String getLegacyDefaultIdentifyPrompt(SharedPreferences preferences) {
        if (preferences.getInt(LEGACY_KEY_PROMPT_LIBRARY_LANGUAGE, 0) == 1) {
            return "Объясни, что имеется в виду в этом сообщении, включая вероятный контекст и интерпретацию.";
        }
        return "Explain what is meant by this message, including the likely context and interpretation.";
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
}
