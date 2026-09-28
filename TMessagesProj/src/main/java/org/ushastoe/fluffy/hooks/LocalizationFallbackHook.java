package org.ushastoe.fluffy.hooks;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.ushastoe.fluffy.patches.LocalizationFallbackPatch;

public final class LocalizationFallbackHook {

    private LocalizationFallbackHook() {
    }

    @Nullable
    public static String orAndroidResource(@StringRes int stringRes, @Nullable String localized) {
        return LocalizationFallbackPatch.orAndroidResource(stringRes, localized);
    }
}
