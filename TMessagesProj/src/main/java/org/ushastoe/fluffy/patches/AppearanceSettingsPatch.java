package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

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
    private static final String KEY_ROUND_VIDEO_CAMERA_FEATURE_ENABLED = "round_video_camera_feature_enabled";
    private static final String KEY_ROUND_VIDEO_CAMERA_MODE = "round_video_camera_mode";
    private static final String KEY_ROUND_VIDEO_CAMERA_DEFAULT_MODE = "round_video_camera_default_mode";

    public static final int DIALOGS_TITLE_MODE_DEFAULT = 0;
    public static final int DIALOGS_TITLE_MODE_CENTERED = 1;
    public static final int DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS = 2;
    public static final int DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM = 0;
    public static final int DIALOGS_APP_TITLE_MODE_FLUFFY = 1;
    public static final int DIALOGS_APP_TITLE_MODE_TELEGRAM = 2;
    public static final int DIALOGS_APP_TITLE_MODE_USERNAME = 3;
    public static final int DIALOGS_APP_TITLE_MODE_FIRST_NAME = 4;
    public static final int DIALOGS_APP_TITLE_MODE_CUSTOM = 5;
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
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

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

    private static int clampDoubleTapAction(int action) {
        if (action < DOUBLE_TAP_ACTION_NONE || action > DOUBLE_TAP_ACTION_DELETE) {
            return DOUBLE_TAP_ACTION_REACTION;
        }
        return action;
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

    private static int clampRoundVideoCameraMode(int mode) {
        if (mode < ROUND_VIDEO_CAMERA_FRONT || mode > ROUND_VIDEO_CAMERA_BACK) {
            return ROUND_VIDEO_CAMERA_FRONT;
        }
        return mode;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
