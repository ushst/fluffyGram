package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AppearanceSettingsPatch {
    private static final String PREFS_NAME = "fluffy_appearance_settings";
    private static final String KEY_HIDE_CHANNEL_POST_STARS_OFFER = "hide_channel_post_stars_offer";
    private static final String KEY_DIALOGS_TITLE_MODE = "dialogs_title_mode";
    private static final String KEY_DIALOGS_APP_TITLE_MODE = "dialogs_app_title_mode";
    private static final String KEY_DIALOGS_APP_TITLE_CUSTOM = "dialogs_app_title_custom";
    private static final String KEY_FLUFFY_NOTIFICATION_ICON = "fluffy_notification_icon";
    private static final String KEY_APP_FONT = "app_font";
    private static final String KEY_DOUBLE_TAP_IN_ACTION = "double_tap_in_action";
    private static final String KEY_DOUBLE_TAP_OUT_ACTION = "double_tap_out_action";
    private static final String KEY_TIME_WITH_SECONDS = "time_with_seconds";
    private static final String KEY_DISABLE_ROUNDED_NUMBERS = "disable_rounded_numbers";
    private static final String KEY_THOUSANDS_SEPARATOR = "thousands_separator";
    private static final String KEY_DIALOGS_LIST_SCALE = "dialogs_list_scale";
    private static final String KEY_CENTER_CHAT_HEADER = "center_chat_header";
    private static final String KEY_MAP_PROVIDER = "map_provider";
    private static final String KEY_EDITED_MARKER_ICON = "edited_marker_icon";
    private static final String KEY_EDITED_MARKER_MODE = "edited_marker_mode";
    private static final String KEY_SCHEDULED_MARKER_MODE = "scheduled_marker_mode";
    private static final String KEY_SILENT_MARKER_MODE = "silent_marker_mode";
    private static final String KEY_ROUND_VIDEO_CAMERA_FEATURE_ENABLED = "round_video_camera_feature_enabled";
    private static final String KEY_ROUND_VIDEO_CAMERA_MODE = "round_video_camera_mode";
    private static final String KEY_ROUND_VIDEO_CAMERA_DEFAULT_MODE = "round_video_camera_default_mode";
    private static final String KEY_HIDE_STORIES = "hide_stories";
    private static final String KEY_SHOW_FORWARDED_ORIGINAL_DATE = "show_forwarded_original_date";

    public static final int DIALOGS_TITLE_MODE_DEFAULT = 0;
    public static final int DIALOGS_TITLE_MODE_CENTERED = 1;
    public static final int DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS = 2;
    public static final int DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM = 0;
    public static final int DIALOGS_APP_TITLE_MODE_FLUFFY = 1;
    public static final int DIALOGS_APP_TITLE_MODE_TELEGRAM = 2;
    public static final int DIALOGS_APP_TITLE_MODE_USERNAME = 3;
    public static final int DIALOGS_APP_TITLE_MODE_FIRST_NAME = 4;
    public static final int DIALOGS_APP_TITLE_MODE_CUSTOM = 5;
    public static final int DIALOGS_LIST_SCALE_MIN = 90;
    public static final int DIALOGS_LIST_SCALE_DEFAULT = 100;
    public static final int DIALOGS_LIST_SCALE_MAX = 110;
    public static final int MAP_PROVIDER_GOOGLE = 0;
    public static final int MAP_PROVIDER_OPENSTREETMAP = 1;
    public static final int EDITED_MARKER_MODE_TEXT = 0;
    public static final int EDITED_MARKER_MODE_SHORT_TEXT = 1;
    public static final int EDITED_MARKER_MODE_ICON_STAMP = 2;
    public static final int EDITED_MARKER_MODE_ICON_EDIT = 3;
    public static final int SCHEDULED_MARKER_MODE_TEXT = 0;
    public static final int SCHEDULED_MARKER_MODE_SHORT_TEXT = 1;
    public static final int SCHEDULED_MARKER_MODE_ICON_CALENDAR = 2;
    public static final int SCHEDULED_MARKER_MODE_ICON_SCHEDULE = 3;
    public static final int SILENT_MARKER_MODE_TEXT = 0;
    public static final int SILENT_MARKER_MODE_SHORT_TEXT = 1;
    public static final int SILENT_MARKER_MODE_ICON_NOTIFY_OFF = 2;
    public static final int SILENT_MARKER_MODE_ICON_MUTE = 3;
    public static final int DOUBLE_TAP_ACTION_NONE = 0;
    public static final int DOUBLE_TAP_ACTION_REACTION = 1;
    public static final int DOUBLE_TAP_ACTION_REPLY = 2;
    public static final int DOUBLE_TAP_ACTION_COPY = 3;
    public static final int DOUBLE_TAP_ACTION_FORWARD = 4;
    public static final int DOUBLE_TAP_ACTION_EDIT = 5;
    public static final int DOUBLE_TAP_ACTION_SAVE = 6;
    public static final int DOUBLE_TAP_ACTION_DELETE = 7;
    public static final int ROUND_VIDEO_CAMERA_FRONT = 0;
    public static final int ROUND_VIDEO_CAMERA_BACK = 1;
    private static final int MAX_SYNC_JSON_BYTES = 16 * 1024;
    private static final int MAX_CUSTOM_TITLE_LENGTH = 64;
    private static final int MAX_FONT_KEY_LENGTH = 128;
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Set<String> SYNC_ALLOWED_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            KEY_HIDE_CHANNEL_POST_STARS_OFFER,
            KEY_DIALOGS_TITLE_MODE,
            KEY_DIALOGS_APP_TITLE_MODE,
            KEY_DIALOGS_APP_TITLE_CUSTOM,
            KEY_FLUFFY_NOTIFICATION_ICON,
            KEY_DOUBLE_TAP_IN_ACTION,
            KEY_DOUBLE_TAP_OUT_ACTION,
            KEY_TIME_WITH_SECONDS,
            KEY_DISABLE_ROUNDED_NUMBERS,
            KEY_THOUSANDS_SEPARATOR,
            KEY_DIALOGS_LIST_SCALE,
            KEY_CENTER_CHAT_HEADER,
            KEY_MAP_PROVIDER,
            KEY_EDITED_MARKER_MODE,
            KEY_SCHEDULED_MARKER_MODE,
            KEY_SILENT_MARKER_MODE,
            KEY_ROUND_VIDEO_CAMERA_FEATURE_ENABLED,
            KEY_ROUND_VIDEO_CAMERA_MODE,
            KEY_ROUND_VIDEO_CAMERA_DEFAULT_MODE,
            KEY_HIDE_STORIES,
            KEY_SHOW_FORWARDED_ORIGINAL_DATE
    )));

    public interface Listener {
        void onAppearanceSettingsChanged();
    }

    private AppearanceSettingsPatch() {
    }

    public static boolean isChannelPostStarsOfferHidden() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_HIDE_CHANNEL_POST_STARS_OFFER, false);
    }

    public static void setChannelPostStarsOfferHidden(boolean hidden) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_HIDE_CHANNEL_POST_STARS_OFFER, hidden).apply();
        notifyListeners();
    }

    public static boolean shouldShowChannelPostStarsOffer() {
        return !isChannelPostStarsOfferHidden();
    }

    public static boolean shouldShowChannelPostStarsUi(MessageObject messageObject) {
        return !isChannelPostStarsOfferHidden()
                || messageObject == null;
    }

    public static boolean shouldShowReaction(MessageObject messageObject, TLRPC.Reaction reaction) {
        return !(reaction instanceof TLRPC.TL_reactionPaid) || shouldShowChannelPostStarsUi(messageObject);
    }

    public static int getDialogsTitleMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return DIALOGS_TITLE_MODE_DEFAULT;
        }
        int mode = preferences.getInt(KEY_DIALOGS_TITLE_MODE, DIALOGS_TITLE_MODE_DEFAULT);
        if (mode < DIALOGS_TITLE_MODE_DEFAULT || mode > DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS) {
            return DIALOGS_TITLE_MODE_DEFAULT;
        }
        return mode;
    }

    public static void setDialogsTitleMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DIALOGS_TITLE_MODE, mode).apply();
        notifyListeners();
    }

    public static int getDialogsAppTitleMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM;
        }
        int mode = preferences.getInt(KEY_DIALOGS_APP_TITLE_MODE, DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM);
        if (mode < DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM || mode > DIALOGS_APP_TITLE_MODE_CUSTOM) {
            return DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM;
        }
        return mode;
    }

    public static void setDialogsAppTitleMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DIALOGS_APP_TITLE_MODE, clampDialogsAppTitleMode(mode)).apply();
        notifyListeners();
    }

    public static String getDialogsAppTitleCustom() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return "";
        }
        String value = preferences.getString(KEY_DIALOGS_APP_TITLE_CUSTOM, "");
        return value != null ? value : "";
    }

    public static void setDialogsAppTitleCustom(String value) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putString(KEY_DIALOGS_APP_TITLE_CUSTOM, value != null ? value.trim() : "").apply();
        notifyListeners();
    }

    public static boolean useFluffyNotificationIcon() {
        SharedPreferences preferences = getPreferences();
        return preferences == null || preferences.getBoolean(KEY_FLUFFY_NOTIFICATION_ICON, true);
    }

    public static void setUseFluffyNotificationIcon(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_FLUFFY_NOTIFICATION_ICON, enabled).apply();
        notifyListeners();
    }

    public static String getAppFontKey() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return "";
        }
        String value = preferences.getString(KEY_APP_FONT, "");
        return value != null ? value : "";
    }

    public static void setAppFontKey(String value) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putString(KEY_APP_FONT, value != null ? value.trim() : "").apply();
        AppFontPatch.onFontChanged();
        notifyListeners();
    }

    public static int getDoubleTapInAction() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return DOUBLE_TAP_ACTION_REACTION;
        }
        return clampDoubleTapAction(preferences.getInt(KEY_DOUBLE_TAP_IN_ACTION, DOUBLE_TAP_ACTION_REACTION));
    }

    public static void setDoubleTapInAction(int action) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DOUBLE_TAP_IN_ACTION, clampDoubleTapAction(action)).apply();
        notifyListeners();
    }

    public static int getDoubleTapOutAction() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return DOUBLE_TAP_ACTION_REACTION;
        }
        return clampDoubleTapAction(preferences.getInt(KEY_DOUBLE_TAP_OUT_ACTION, DOUBLE_TAP_ACTION_REACTION));
    }

    public static void setDoubleTapOutAction(int action) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DOUBLE_TAP_OUT_ACTION, clampDoubleTapAction(action)).apply();
        notifyListeners();
    }

    public static boolean isTimeWithSecondsEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_TIME_WITH_SECONDS, false);
    }

    public static void setTimeWithSecondsEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_TIME_WITH_SECONDS, enabled).apply();
        notifyListeners();
    }

    public static boolean isRoundedNumbersDisabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_DISABLE_ROUNDED_NUMBERS, false);
    }

    public static void setRoundedNumbersDisabled(boolean disabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_DISABLE_ROUNDED_NUMBERS, disabled).apply();
        notifyListeners();
    }

    public static boolean isThousandsSeparatorEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_THOUSANDS_SEPARATOR, false);
    }

    public static void setThousandsSeparatorEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_THOUSANDS_SEPARATOR, enabled).apply();
        notifyListeners();
    }

    public static int getDialogsListScale() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return DIALOGS_LIST_SCALE_DEFAULT;
        }
        return clampDialogsListScale(preferences.getInt(KEY_DIALOGS_LIST_SCALE, DIALOGS_LIST_SCALE_DEFAULT));
    }

    public static void setDialogsListScale(int scale) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_DIALOGS_LIST_SCALE, clampDialogsListScale(scale)).apply();
        notifyListeners();
    }

    public static boolean isCenterChatHeaderEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_CENTER_CHAT_HEADER, false);
    }

    public static void setCenterChatHeaderEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_CENTER_CHAT_HEADER, enabled).apply();
        notifyListeners();
    }

    public static int getMapProvider() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return MAP_PROVIDER_OPENSTREETMAP;
        }
        int provider = preferences.getInt(KEY_MAP_PROVIDER, MAP_PROVIDER_OPENSTREETMAP);
        if (provider < MAP_PROVIDER_GOOGLE || provider > MAP_PROVIDER_OPENSTREETMAP) {
            return MAP_PROVIDER_OPENSTREETMAP;
        }
        return provider;
    }

    public static void setMapProvider(int provider) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_MAP_PROVIDER, clampMapProvider(provider)).apply();
        MapsProviderPatch.onMapProviderChanged();
        notifyListeners();
    }

    public static int getEditedMarkerMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return EDITED_MARKER_MODE_TEXT;
        }
        if (preferences.contains(KEY_EDITED_MARKER_MODE)) {
            int mode = preferences.getInt(KEY_EDITED_MARKER_MODE, EDITED_MARKER_MODE_TEXT);
            return clampEditedMarkerMode(mode);
        }
        // Backward compatibility with old boolean toggle.
        boolean useIcon = preferences.getBoolean(KEY_EDITED_MARKER_ICON, false);
        return useIcon ? EDITED_MARKER_MODE_ICON_STAMP : EDITED_MARKER_MODE_TEXT;
    }

    public static void setEditedMarkerMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putInt(KEY_EDITED_MARKER_MODE, clampEditedMarkerMode(mode))
                .remove(KEY_EDITED_MARKER_ICON)
                .apply();
        notifyListeners();
    }

    public static boolean useEditedMarkerIcon() {
        int mode = getEditedMarkerMode();
        return mode == EDITED_MARKER_MODE_ICON_STAMP || mode == EDITED_MARKER_MODE_ICON_EDIT;
    }

    public static void setUseEditedMarkerIcon(boolean enabled) {
        setEditedMarkerMode(enabled ? EDITED_MARKER_MODE_ICON_STAMP : EDITED_MARKER_MODE_TEXT);
    }

    public static int getScheduledMarkerMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return SCHEDULED_MARKER_MODE_TEXT;
        }
        int mode = preferences.getInt(KEY_SCHEDULED_MARKER_MODE, SCHEDULED_MARKER_MODE_TEXT);
        return clampScheduledMarkerMode(mode);
    }

    public static void setScheduledMarkerMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_SCHEDULED_MARKER_MODE, clampScheduledMarkerMode(mode)).apply();
        notifyListeners();
    }

    public static int getSilentMarkerMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return SILENT_MARKER_MODE_TEXT;
        }
        int mode = preferences.getInt(KEY_SILENT_MARKER_MODE, SILENT_MARKER_MODE_TEXT);
        return clampSilentMarkerMode(mode);
    }

    public static void setSilentMarkerMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_SILENT_MARKER_MODE, clampSilentMarkerMode(mode)).apply();
        notifyListeners();
    }

    public static boolean isRoundVideoCameraFeatureEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_ROUND_VIDEO_CAMERA_FEATURE_ENABLED, false);
    }

    public static void setRoundVideoCameraFeatureEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ROUND_VIDEO_CAMERA_FEATURE_ENABLED, enabled).apply();
        notifyListeners();
    }

    public static int getRoundVideoCameraMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return ROUND_VIDEO_CAMERA_FRONT;
        }
        int mode = preferences.getInt(KEY_ROUND_VIDEO_CAMERA_MODE, ROUND_VIDEO_CAMERA_FRONT);
        if (mode < ROUND_VIDEO_CAMERA_FRONT || mode > ROUND_VIDEO_CAMERA_BACK) {
            return ROUND_VIDEO_CAMERA_FRONT;
        }
        return mode;
    }

    public static void setRoundVideoCameraMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_ROUND_VIDEO_CAMERA_MODE, clampRoundVideoCameraMode(mode)).apply();
        notifyListeners();
    }

    public static boolean useFrontRoundVideoCamera() {
        return getRoundVideoCameraMode() == ROUND_VIDEO_CAMERA_FRONT;
    }

    public static int getDefaultRoundVideoCameraMode() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return ROUND_VIDEO_CAMERA_FRONT;
        }
        int mode = preferences.getInt(KEY_ROUND_VIDEO_CAMERA_DEFAULT_MODE, ROUND_VIDEO_CAMERA_FRONT);
        if (mode < ROUND_VIDEO_CAMERA_FRONT || mode > ROUND_VIDEO_CAMERA_BACK) {
            return ROUND_VIDEO_CAMERA_FRONT;
        }
        return mode;
    }

    public static void setDefaultRoundVideoCameraMode(int mode) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(KEY_ROUND_VIDEO_CAMERA_DEFAULT_MODE, clampRoundVideoCameraMode(mode)).apply();
        notifyListeners();
    }

    public static boolean useFrontRoundVideoCameraByDefault() {
        return getDefaultRoundVideoCameraMode() == ROUND_VIDEO_CAMERA_FRONT;
    }

    public static boolean isStoriesHidden() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_HIDE_STORIES, false);
    }

    public static void setStoriesHidden(boolean hidden) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_HIDE_STORIES, hidden).apply();
        notifyListeners();
    }

    public static boolean isForwardedOriginalDateShown() {
        SharedPreferences preferences = getPreferences();
        return preferences == null || preferences.getBoolean(KEY_SHOW_FORWARDED_ORIGINAL_DATE, true);
    }

    public static void setForwardedOriginalDateShown(boolean shown) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_SHOW_FORWARDED_ORIGINAL_DATE, shown).apply();
        notifyListeners();
    }

    public static String exportSettingsJson() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return "{}";
        }
        JSONObject result = new JSONObject();
        try {
            for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
                if (!SYNC_ALLOWED_KEYS.contains(entry.getKey())) {
                    continue;
                }
                Object sanitizedValue = sanitizeValueForKey(entry.getKey(), entry.getValue());
                if (sanitizedValue != null) {
                    result.put(entry.getKey(), wrapJsonValue(sanitizedValue));
                }
            }
        } catch (Exception ignore) {
        }
        String json = result.toString();
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_SYNC_JSON_BYTES) {
            return "{}";
        }
        return json;
    }

    public static void importSettingsJson(String json) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        try {
            if (!isSyncJsonValid(json)) {
                return;
            }
            String preservedAppFont = preferences.getString(KEY_APP_FONT, "");
            JSONObject data = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.putString(KEY_APP_FONT, preservedAppFont != null ? preservedAppFont : "");
            java.util.Iterator<String> keys = data.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                if (!SYNC_ALLOWED_KEYS.contains(key)) {
                    continue;
                }
                putSanitizedJsonValue(editor, key, data.opt(key));
            }
            editor.apply();
            AppFontPatch.onFontChanged();
            MapsProviderPatch.onMapProviderChanged();
            notifyListeners();
        } catch (Exception ignore) {
        }
    }

    public static boolean isSyncJsonValid(String json) {
        if (TextUtils.isEmpty(json)) {
            return true;
        }
        return json.getBytes(StandardCharsets.UTF_8).length <= MAX_SYNC_JSON_BYTES;
    }

    private static int clampDoubleTapAction(int action) {
        if (action < DOUBLE_TAP_ACTION_NONE || action > DOUBLE_TAP_ACTION_DELETE) {
            return DOUBLE_TAP_ACTION_REACTION;
        }
        return action;
    }

    private static int clampEditedMarkerMode(int mode) {
        if (mode < EDITED_MARKER_MODE_TEXT || mode > EDITED_MARKER_MODE_ICON_EDIT) {
            return EDITED_MARKER_MODE_TEXT;
        }
        return mode;
    }

    private static int clampScheduledMarkerMode(int mode) {
        if (mode < SCHEDULED_MARKER_MODE_TEXT || mode > SCHEDULED_MARKER_MODE_ICON_SCHEDULE) {
            return SCHEDULED_MARKER_MODE_TEXT;
        }
        return mode;
    }

    private static int clampSilentMarkerMode(int mode) {
        if (mode < SILENT_MARKER_MODE_TEXT || mode > SILENT_MARKER_MODE_ICON_MUTE) {
            return SILENT_MARKER_MODE_TEXT;
        }
        return mode;
    }

    public static void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private static void notifyListeners() {
        for (Listener listener : listeners) {
            listener.onAppearanceSettingsChanged();
        }
    }

    private static int clampDialogsAppTitleMode(int mode) {
        if (mode < DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM || mode > DIALOGS_APP_TITLE_MODE_CUSTOM) {
            return DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM;
        }
        return mode;
    }

    private static int clampDialogsListScale(int scale) {
        if (scale < DIALOGS_LIST_SCALE_MIN) {
            return DIALOGS_LIST_SCALE_MIN;
        }
        if (scale > DIALOGS_LIST_SCALE_MAX) {
            return DIALOGS_LIST_SCALE_MAX;
        }
        return scale;
    }

    private static int clampRoundVideoCameraMode(int mode) {
        if (mode < ROUND_VIDEO_CAMERA_FRONT || mode > ROUND_VIDEO_CAMERA_BACK) {
            return ROUND_VIDEO_CAMERA_FRONT;
        }
        return mode;
    }

    private static int clampMapProvider(int provider) {
        if (provider < MAP_PROVIDER_GOOGLE || provider > MAP_PROVIDER_OPENSTREETMAP) {
            return MAP_PROVIDER_OPENSTREETMAP;
        }
        return provider;
    }

    private static Object wrapJsonValue(Object value) {
        if (value instanceof Collection) {
            return new JSONArray((Collection<?>) value);
        }
        return value;
    }

    private static void putSanitizedJsonValue(SharedPreferences.Editor editor, String key, Object value) {
        if (editor == null || TextUtils.isEmpty(key) || value == null || value == JSONObject.NULL) {
            return;
        }
        Object sanitized = sanitizeValueForKey(key, value);
        if (sanitized == null) {
            return;
        }
        if (sanitized instanceof Boolean) {
            editor.putBoolean(key, (Boolean) sanitized);
        } else if (sanitized instanceof Integer) {
            editor.putInt(key, (Integer) sanitized);
        } else if (sanitized instanceof Long) {
            editor.putLong(key, (Long) sanitized);
        } else if (sanitized instanceof Float) {
            editor.putFloat(key, (Float) sanitized);
        } else if (sanitized instanceof String) {
            editor.putString(key, (String) sanitized);
        }
    }

    private static Object sanitizeValueForKey(String key, Object value) {
        if (TextUtils.isEmpty(key) || value == null || value == JSONObject.NULL) {
            return null;
        }
        switch (key) {
            case KEY_HIDE_CHANNEL_POST_STARS_OFFER:
            case KEY_FLUFFY_NOTIFICATION_ICON:
            case KEY_TIME_WITH_SECONDS:
            case KEY_DISABLE_ROUNDED_NUMBERS:
            case KEY_THOUSANDS_SEPARATOR:
            case KEY_CENTER_CHAT_HEADER:
            case KEY_ROUND_VIDEO_CAMERA_FEATURE_ENABLED:
            case KEY_HIDE_STORIES:
            case KEY_SHOW_FORWARDED_ORIGINAL_DATE:
                return asBoolean(value);
            case KEY_DIALOGS_TITLE_MODE:
                return clampDialogsTitleMode(asInt(value, DIALOGS_TITLE_MODE_DEFAULT));
            case KEY_DIALOGS_APP_TITLE_MODE:
                return clampDialogsAppTitleMode(asInt(value, DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM));
            case KEY_DIALOGS_APP_TITLE_CUSTOM:
                return trimToLength(asString(value), MAX_CUSTOM_TITLE_LENGTH);
            case KEY_DOUBLE_TAP_IN_ACTION:
            case KEY_DOUBLE_TAP_OUT_ACTION:
                return clampDoubleTapAction(asInt(value, DOUBLE_TAP_ACTION_REACTION));
            case KEY_DIALOGS_LIST_SCALE:
                return clampDialogsListScale(asInt(value, DIALOGS_LIST_SCALE_DEFAULT));
            case KEY_MAP_PROVIDER:
                return clampMapProvider(asInt(value, MAP_PROVIDER_OPENSTREETMAP));
            case KEY_EDITED_MARKER_MODE:
                return clampEditedMarkerMode(asInt(value, EDITED_MARKER_MODE_TEXT));
            case KEY_SCHEDULED_MARKER_MODE:
                return clampScheduledMarkerMode(asInt(value, SCHEDULED_MARKER_MODE_TEXT));
            case KEY_SILENT_MARKER_MODE:
                return clampSilentMarkerMode(asInt(value, SILENT_MARKER_MODE_TEXT));
            case KEY_ROUND_VIDEO_CAMERA_MODE:
            case KEY_ROUND_VIDEO_CAMERA_DEFAULT_MODE:
                return clampRoundVideoCameraMode(asInt(value, ROUND_VIDEO_CAMERA_FRONT));
            default:
                return null;
        }
    }

    private static int clampDialogsTitleMode(int mode) {
        if (mode < DIALOGS_TITLE_MODE_DEFAULT || mode > DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS) {
            return DIALOGS_TITLE_MODE_DEFAULT;
        }
        return mode;
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private static int asInt(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignore) {
            return fallback;
        }
    }

    private static String asString(Object value) {
        return value == null || value == JSONObject.NULL ? "" : String.valueOf(value).trim();
    }

    private static String trimToLength(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
