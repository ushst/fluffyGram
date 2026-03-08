package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;

public final class AppearanceSettingsPatch {
    private static final String PREFS_NAME = "fluffy_appearance_settings";
    private static final String KEY_HIDE_CHANNEL_POST_STARS_OFFER = "hide_channel_post_stars_offer";

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

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
