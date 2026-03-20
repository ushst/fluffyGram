package org.ushastoe.fluffy.patches;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;

import java.nio.charset.StandardCharsets;

public final class TelegramSettingsSyncPatch {

    private static final String KEY_RAISE_TO_SPEAK = "raise_to_speak";
    private static final String KEY_RAISE_TO_LISTEN = "raise_to_listen";
    private static final String KEY_CUSTOM_TABS = "custom_tabs";
    private static final String KEY_INAPP_BROWSER = "inapp_browser";
    private static final String KEY_ADAPTABLE_BROWSER = "adaptableBrowser";
    private static final String KEY_DIRECT_SHARE = "direct_share";
    private static final String KEY_INAPP_CAMERA = "inappCamera";
    private static final String KEY_STREAM_MEDIA = "streamMedia";
    private static final String KEY_STREAM_ALL_VIDEO = "streamAllVideo";
    private static final String KEY_STREAM_MKV = "streamMkv";
    private static final String KEY_SAVE_STREAM_MEDIA = "saveStreamMedia";
    private static final String KEY_PAUSE_MUSIC_ON_RECORD = "pauseMusicOnRecord";
    private static final String KEY_PAUSE_MUSIC_ON_MEDIA = "pauseMusicOnMedia";
    private static final String KEY_SORT_CONTACTS_BY_NAME = "sortContactsByName";
    private static final String KEY_SORT_FILES_BY_NAME = "sortFilesByName";
    private static final String KEY_REPEAT_MODE = "repeatMode";
    private static final String KEY_ALLOW_BIG_EMOJI = "allowBigEmoji";
    private static final String KEY_USE_SYSTEM_EMOJI = "useSystemEmoji";
    private static final String KEY_FONT_SIZE = "fons_size";
    private static final String KEY_BUBBLE_RADIUS = "bubbleRadius";
    private static final String KEY_USE_THREE_LINES_LAYOUT = "useThreeLinesLayout";
    private static final String KEY_ARCHIVE_HIDDEN = "archiveHidden";
    private static final String KEY_DISTANCE_SYSTEM_TYPE = "distanceSystemType";
    private static final String KEY_VIEW_ANIMATIONS = "view_animations";

    private static final int MAX_SYNC_JSON_BYTES = 16 * 1024;
    private static final int FONT_SIZE_MIN = 12;
    private static final int FONT_SIZE_MAX = 30;
    private static final int BUBBLE_RADIUS_MIN = 0;
    private static final int BUBBLE_RADIUS_MAX = 30;
    private static final int DISTANCE_SYSTEM_TYPE_MIN = 0;
    private static final int DISTANCE_SYSTEM_TYPE_MAX = 1;

    private TelegramSettingsSyncPatch() {
    }

    public static String exportSettingsJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_RAISE_TO_SPEAK, SharedConfig.raiseToSpeak);
            object.put(KEY_RAISE_TO_LISTEN, SharedConfig.raiseToListen);
            object.put(KEY_CUSTOM_TABS, SharedConfig.customTabs);
            object.put(KEY_INAPP_BROWSER, SharedConfig.inappBrowser);
            object.put(KEY_ADAPTABLE_BROWSER, SharedConfig.adaptableColorInBrowser);
            object.put(KEY_DIRECT_SHARE, SharedConfig.directShare);
            object.put(KEY_INAPP_CAMERA, SharedConfig.inappCamera);
            object.put(KEY_STREAM_MEDIA, SharedConfig.streamMedia);
            object.put(KEY_STREAM_ALL_VIDEO, SharedConfig.streamAllVideo);
            object.put(KEY_STREAM_MKV, SharedConfig.streamMkv);
            object.put(KEY_SAVE_STREAM_MEDIA, SharedConfig.saveStreamMedia);
            object.put(KEY_PAUSE_MUSIC_ON_RECORD, SharedConfig.pauseMusicOnRecord);
            object.put(KEY_PAUSE_MUSIC_ON_MEDIA, SharedConfig.pauseMusicOnMedia);
            object.put(KEY_SORT_CONTACTS_BY_NAME, SharedConfig.sortContactsByName);
            object.put(KEY_SORT_FILES_BY_NAME, SharedConfig.sortFilesByName);
            object.put(KEY_REPEAT_MODE, clampRepeatMode(SharedConfig.repeatMode));
            object.put(KEY_ALLOW_BIG_EMOJI, SharedConfig.allowBigEmoji);
            object.put(KEY_USE_SYSTEM_EMOJI, SharedConfig.useSystemEmoji);
            object.put(KEY_FONT_SIZE, clampFontSize(SharedConfig.fontSize));
            object.put(KEY_BUBBLE_RADIUS, clampBubbleRadius(SharedConfig.bubbleRadius));
            object.put(KEY_USE_THREE_LINES_LAYOUT, SharedConfig.useThreeLinesLayout);
            object.put(KEY_ARCHIVE_HIDDEN, SharedConfig.archiveHidden);
            object.put(KEY_DISTANCE_SYSTEM_TYPE, clampDistanceSystemType(SharedConfig.distanceSystemType));
            object.put(KEY_VIEW_ANIMATIONS, SharedConfig.animationsEnabled());
        } catch (Exception ignore) {
        }
        String json = object.toString();
        if (!isSyncJsonValid(json)) {
            return "{}";
        }
        return json;
    }

    public static void importSettingsJson(String json) {
        if (!isSyncJsonValid(json)) {
            return;
        }
        SharedPreferences preferences = MessagesController.getGlobalMainSettings();
        if (preferences == null) {
            return;
        }
        try {
            JSONObject object = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);

            applyToggleBoolean(object, KEY_RAISE_TO_SPEAK, SharedConfig.raiseToSpeak, SharedConfig::toggleRaiseToSpeak);
            applyToggleBoolean(object, KEY_RAISE_TO_LISTEN, SharedConfig.raiseToListen, SharedConfig::toggleRaiseToListen);
            applyBooleanValue(object, KEY_CUSTOM_TABS, value -> SharedConfig.toggleCustomTabs(value));
            applyToggleBoolean(object, KEY_INAPP_BROWSER, SharedConfig.inappBrowser, SharedConfig::toggleInappBrowser);
            applyToggleBoolean(object, KEY_ADAPTABLE_BROWSER, SharedConfig.adaptableColorInBrowser, SharedConfig::toggleBrowserAdaptableColors);
            applyToggleBoolean(object, KEY_DIRECT_SHARE, SharedConfig.directShare, SharedConfig::toggleDirectShare);
            applyToggleBoolean(object, KEY_INAPP_CAMERA, SharedConfig.inappCamera, SharedConfig::toggleInappCamera);
            applyToggleBoolean(object, KEY_STREAM_MEDIA, SharedConfig.streamMedia, SharedConfig::toggleStreamMedia);
            applyToggleBoolean(object, KEY_STREAM_ALL_VIDEO, SharedConfig.streamAllVideo, SharedConfig::toggleStreamAllVideo);
            applyToggleBoolean(object, KEY_STREAM_MKV, SharedConfig.streamMkv, SharedConfig::toggleStreamMkv);
            applyToggleBoolean(object, KEY_SAVE_STREAM_MEDIA, SharedConfig.saveStreamMedia, SharedConfig::toggleSaveStreamMedia);
            applyToggleBoolean(object, KEY_PAUSE_MUSIC_ON_RECORD, SharedConfig.pauseMusicOnRecord, SharedConfig::togglePauseMusicOnRecord);
            applyToggleBoolean(object, KEY_PAUSE_MUSIC_ON_MEDIA, SharedConfig.pauseMusicOnMedia, SharedConfig::togglePauseMusicOnMedia);
            applyToggleBoolean(object, KEY_SORT_CONTACTS_BY_NAME, SharedConfig.sortContactsByName, SharedConfig::toggleSortContactsByName);
            applyToggleBoolean(object, KEY_SORT_FILES_BY_NAME, SharedConfig.sortFilesByName, SharedConfig::toggleSortFilesByName);

            if (object.has(KEY_REPEAT_MODE)) {
                SharedConfig.setRepeatMode(clampRepeatMode(object.optInt(KEY_REPEAT_MODE, SharedConfig.repeatMode)));
            }
            applyToggleBoolean(object, KEY_ALLOW_BIG_EMOJI, SharedConfig.allowBigEmoji, SharedConfig::toggleBigEmoji);
            if (object.has(KEY_USE_THREE_LINES_LAYOUT)) {
                SharedConfig.setUseThreeLinesLayout(object.optBoolean(KEY_USE_THREE_LINES_LAYOUT, SharedConfig.useThreeLinesLayout));
            }
            applyToggleBoolean(object, KEY_ARCHIVE_HIDDEN, SharedConfig.archiveHidden, SharedConfig::toggleArchiveHidden);
            if (object.has(KEY_DISTANCE_SYSTEM_TYPE)) {
                SharedConfig.setDistanceSystemType(clampDistanceSystemType(object.optInt(KEY_DISTANCE_SYSTEM_TYPE, SharedConfig.distanceSystemType)));
            }

            SharedPreferences.Editor editor = preferences.edit();
            boolean uiChanged = false;

            if (object.has(KEY_USE_SYSTEM_EMOJI)) {
                boolean value = object.optBoolean(KEY_USE_SYSTEM_EMOJI, SharedConfig.useSystemEmoji);
                SharedConfig.useSystemEmoji = value;
                editor.putBoolean(KEY_USE_SYSTEM_EMOJI, value);
                uiChanged = true;
            }
            if (object.has(KEY_FONT_SIZE)) {
                SharedConfig.fontSize = clampFontSize(object.optInt(KEY_FONT_SIZE, SharedConfig.fontSize));
                SharedConfig.fontSizeIsDefault = false;
                editor.putInt(KEY_FONT_SIZE, SharedConfig.fontSize);
                uiChanged = true;
            }
            if (object.has(KEY_BUBBLE_RADIUS)) {
                SharedConfig.bubbleRadius = clampBubbleRadius(object.optInt(KEY_BUBBLE_RADIUS, SharedConfig.bubbleRadius));
                editor.putInt(KEY_BUBBLE_RADIUS, SharedConfig.bubbleRadius);
                uiChanged = true;
            }
            if (object.has(KEY_VIEW_ANIMATIONS)) {
                boolean value = object.optBoolean(KEY_VIEW_ANIMATIONS, SharedConfig.animationsEnabled());
                SharedConfig.setAnimationsEnabled(value);
                editor.putBoolean(KEY_VIEW_ANIMATIONS, value);
            }

            editor.apply();

            if (uiChanged) {
                AndroidUtilities.runOnUIThread(() ->
                        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.dialogsNeedReload, true)
                );
            }
        } catch (Exception ignore) {
        }
    }

    public static boolean isSyncJsonValid(String json) {
        if (TextUtils.isEmpty(json)) {
            return true;
        }
        return json.getBytes(StandardCharsets.UTF_8).length <= MAX_SYNC_JSON_BYTES;
    }

    private static void applyToggleBoolean(JSONObject object, String key, boolean currentValue, Runnable toggler) {
        if (object == null || toggler == null || TextUtils.isEmpty(key) || !object.has(key)) {
            return;
        }
        boolean targetValue = object.optBoolean(key, currentValue);
        if (targetValue != currentValue) {
            toggler.run();
        }
    }

    private static void applyBooleanValue(JSONObject object, String key, BooleanConsumer consumer) {
        if (object == null || consumer == null || TextUtils.isEmpty(key) || !object.has(key)) {
            return;
        }
        consumer.accept(object.optBoolean(key, false));
    }

    private static int clampRepeatMode(int value) {
        if (value < 0 || value > 2) {
            return 0;
        }
        return value;
    }

    private static int clampFontSize(int value) {
        if (value < FONT_SIZE_MIN) {
            return FONT_SIZE_MIN;
        }
        if (value > FONT_SIZE_MAX) {
            return FONT_SIZE_MAX;
        }
        return value;
    }

    private static int clampBubbleRadius(int value) {
        if (value < BUBBLE_RADIUS_MIN) {
            return BUBBLE_RADIUS_MIN;
        }
        if (value > BUBBLE_RADIUS_MAX) {
            return BUBBLE_RADIUS_MAX;
        }
        return value;
    }

    private static int clampDistanceSystemType(int value) {
        if (value < DISTANCE_SYSTEM_TYPE_MIN) {
            return DISTANCE_SYSTEM_TYPE_MIN;
        }
        if (value > DISTANCE_SYSTEM_TYPE_MAX) {
            return DISTANCE_SYSTEM_TYPE_MAX;
        }
        return value;
    }

    private interface BooleanConsumer {
        void accept(boolean value);
    }
}
