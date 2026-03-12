package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.time.FastDateFormat;
import org.ushastoe.fluffy.patches.LocaleFormattingPatch;

public final class LocaleFormattingHook {

    private LocaleFormattingHook() {
    }

    public static FastDateFormat getDayFormatterOverride(LocaleController localeController) {
        return LocaleFormattingPatch.getDayFormatterOverride(localeController);
    }

    public static String formatShortNumberOverride(int number, int[] rounded) {
        return LocaleFormattingPatch.formatShortNumberOverride(number, rounded);
    }
}
