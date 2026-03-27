package org.ushastoe.fluffy.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class NotificationMutedSenderStore {

    public static final String PREFS_NAME = "fluffy_sender_notification_mutes";
    public static final String KEY_PREFIX = "muted_notification_senders_v1_";

    private NotificationMutedSenderStore() {
    }

    public static boolean isMuted(int accountId, long dialogId, long senderId) {
        if (dialogId == 0 || senderId == 0) {
            return false;
        }
        return getSenderSet(accountId, dialogId).contains(Long.toString(senderId));
    }

    public static boolean setMuted(int accountId, long dialogId, long senderId, boolean muted) {
        if (dialogId == 0 || senderId == 0) {
            return false;
        }
        HashSet<String> values = new HashSet<>(getSenderSet(accountId, dialogId));
        boolean changed;
        String value = Long.toString(senderId);
        if (muted) {
            changed = values.add(value);
        } else {
            changed = values.remove(value);
        }
        if (!changed) {
            return false;
        }
        SharedPreferences preferences = getPreferences();
        String key = buildKey(accountId, dialogId);
        SharedPreferences.Editor editor = preferences.edit();
        if (values.isEmpty()) {
            editor.remove(key);
        } else {
            editor.putStringSet(key, values);
        }
        editor.apply();
        return true;
    }

    public static Set<String> getSenderSet(int accountId, long dialogId) {
        SharedPreferences preferences = getPreferences();
        Set<String> values = preferences.getStringSet(buildKey(accountId, dialogId), Collections.emptySet());
        return values != null ? values : Collections.emptySet();
    }

    public static String buildKey(int accountId, long dialogId) {
        return KEY_PREFIX + accountId + "_" + dialogId;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
