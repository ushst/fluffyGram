package org.ushastoe.fluffy.patches;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

/**
 * 12.10.2+ resolves strings via localization_*.bin. Fluffy strings live in
 * fluffy_strings.xml and are not always packed into those assets, so fall back
 * to Android resources when the binary lookup misses.
 */
public final class LocalizationFallbackPatch {

    private LocalizationFallbackPatch() {
    }

    @Nullable
    public static String orAndroidResource(@StringRes int stringRes, @Nullable String localized) {
        if (localized != null || stringRes == 0) {
            return localized;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        try {
            return context.getString(stringRes);
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }
}
