package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.GoogleAiSettingsPatch;

import java.util.ArrayList;

public final class GoogleAiSettingsHook {

    private GoogleAiSettingsHook() {
    }

    public static String getApiKey() {
        return GoogleAiSettingsPatch.getApiKey();
    }

    public static boolean isEnabled() {
        return GoogleAiSettingsPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        GoogleAiSettingsPatch.setEnabled(enabled);
    }

    public static void setApiKey(String value) {
        GoogleAiSettingsPatch.setApiKey(value);
    }

    public static boolean hasApiKey() {
        return GoogleAiSettingsPatch.hasApiKey();
    }

    public static boolean isApiKeyValidated() {
        return GoogleAiSettingsPatch.isApiKeyValidated();
    }

    public static void setApiKeyValidated(boolean validated) {
        GoogleAiSettingsPatch.setApiKeyValidated(validated);
    }

    public static void clearApiKey() {
        GoogleAiSettingsPatch.clearApiKey();
    }

    public static String getApiBaseUrl() {
        return GoogleAiSettingsPatch.getApiBaseUrl();
    }

    public static void setApiBaseUrl(String value) {
        GoogleAiSettingsPatch.setApiBaseUrl(value);
    }

    public static String getModel() {
        return GoogleAiSettingsPatch.getModel();
    }

    public static void setModel(String value) {
        GoogleAiSettingsPatch.setModel(value);
    }

    public static ArrayList<GoogleAiSettingsPatch.PromptPreset> getPromptPresets() {
        return GoogleAiSettingsPatch.getPromptPresets();
    }

    public static void upsertPromptPreset(String id, String title, String prompt) {
        GoogleAiSettingsPatch.upsertPromptPreset(id, title, prompt);
    }

    public static void deletePromptPreset(String id) {
        GoogleAiSettingsPatch.deletePromptPreset(id);
    }

    public static ArrayList<GoogleAiSettingsPatch.PromptPreset> getComposePromptPresets() {
        return GoogleAiSettingsPatch.getComposePromptPresets();
    }

    public static void upsertComposePromptPreset(String id, String title, String prompt) {
        GoogleAiSettingsPatch.upsertComposePromptPreset(id, title, prompt);
    }

    public static void deleteComposePromptPreset(String id) {
        GoogleAiSettingsPatch.deleteComposePromptPreset(id);
    }
}
