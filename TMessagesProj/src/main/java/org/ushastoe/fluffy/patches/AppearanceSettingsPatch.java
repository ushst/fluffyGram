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

    public static final int DIALOGS_TITLE_MODE_DEFAULT = 0;
    public static final int DIALOGS_TITLE_MODE_CENTERED = 1;
    public static final int DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS = 2;
    public static final int DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM = 0;
    public static final int DIALOGS_APP_TITLE_MODE_FLUFFY = 1;
    public static final int DIALOGS_APP_TITLE_MODE_TELEGRAM = 2;
    public static final int DIALOGS_APP_TITLE_MODE_USERNAME = 3;
    public static final int DIALOGS_APP_TITLE_MODE_FIRST_NAME = 4;
    public static final int DIALOGS_APP_TITLE_MODE_CUSTOM = 5;
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

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
