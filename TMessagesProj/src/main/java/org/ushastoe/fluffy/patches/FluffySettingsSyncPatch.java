package org.ushastoe.fluffy.patches;

import android.text.TextUtils;

import org.json.JSONObject;

public final class FluffySettingsSyncPatch {

    private static final String KEY_APPEARANCE = "appearance";
    private static final String KEY_MAIN_TABS = "main_tabs";
    private static final String KEY_UPDATE_CHECK = "update_check";
    private static final String KEY_PREMIUM_SETTINGS = "premium_settings";
    private static final String KEY_DIALOG_FILTER = "dialog_filter";
    private static final String KEY_MEDIA_ONLY_PROXY = "media_only_proxy";
    private static final String KEY_CHAT_FIRST_MESSAGE = "chat_first_message";
    private static final String KEY_QUICK_SHARE_MEDIA = "quick_share_media";
    private static final String KEY_UNLIMITED_PINS = "unlimited_pins";
    private static final String KEY_TELEGRAM_SETTINGS = "telegram_settings";

    private FluffySettingsSyncPatch() {
    }

    public static String exportSettingsJson() {
        JSONObject root = new JSONObject();
        try {
            putSection(root, KEY_APPEARANCE, AppearanceSettingsPatch.exportSettingsJson());
            putSection(root, KEY_MAIN_TABS, MainTabsConfigPatch.exportSettingsJson());
            putSection(root, KEY_UPDATE_CHECK, UpdateCheckSettingsPatch.exportSettingsJson());
            putSection(root, KEY_PREMIUM_SETTINGS, PremiumSettingsPatch.exportSettingsJson());
            putSection(root, KEY_DIALOG_FILTER, DialogFilterSelectionPatch.exportSettingsJson());
            putSection(root, KEY_MEDIA_ONLY_PROXY, MediaOnlyProxyPatch.exportSettingsJson());
            putSection(root, KEY_CHAT_FIRST_MESSAGE, ChatFirstMessagePatch.exportSettingsJson());
            putSection(root, KEY_QUICK_SHARE_MEDIA, QuickShareMediaPatch.exportSettingsJson());
            putSection(root, KEY_UNLIMITED_PINS, UnlimitedPinsPatch.exportSettingsJson());
            putSection(root, KEY_TELEGRAM_SETTINGS, TelegramSettingsSyncPatch.exportSettingsJson());
        } catch (Exception ignore) {
        }
        return root.toString();
    }

    public static void importSettingsJson(String json) {
        try {
            JSONObject root = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
            importSection(root, KEY_APPEARANCE, AppearanceSettingsPatch::importSettingsJson);
            importSection(root, KEY_MAIN_TABS, MainTabsConfigPatch::importSettingsJson);
            importSection(root, KEY_UPDATE_CHECK, UpdateCheckSettingsPatch::importSettingsJson);
            importSection(root, KEY_PREMIUM_SETTINGS, PremiumSettingsPatch::importSettingsJson);
            importSection(root, KEY_DIALOG_FILTER, DialogFilterSelectionPatch::importSettingsJson);
            importSection(root, KEY_MEDIA_ONLY_PROXY, MediaOnlyProxyPatch::importSettingsJson);
            importSection(root, KEY_CHAT_FIRST_MESSAGE, ChatFirstMessagePatch::importSettingsJson);
            importSection(root, KEY_QUICK_SHARE_MEDIA, QuickShareMediaPatch::importSettingsJson);
            importSection(root, KEY_UNLIMITED_PINS, UnlimitedPinsPatch::importSettingsJson);
            importSection(root, KEY_TELEGRAM_SETTINGS, TelegramSettingsSyncPatch::importSettingsJson);
        } catch (Exception ignore) {
        }
    }

    private static void putSection(JSONObject root, String key, String json) throws Exception {
        if (root == null || TextUtils.isEmpty(key) || TextUtils.isEmpty(json)) {
            return;
        }
        root.put(key, new JSONObject(json));
    }

    private static void importSection(JSONObject root, String key, Importer importer) {
        if (root == null || importer == null || TextUtils.isEmpty(key) || !root.has(key)) {
            return;
        }
        JSONObject object = root.optJSONObject(key);
        if (object != null) {
            importer.importJson(object.toString());
        }
    }

    private interface Importer {
        void importJson(String json);
    }
}
