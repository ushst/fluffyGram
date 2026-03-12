package org.ushastoe.fluffy.patches;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.time.FastDateFormat;

public final class LocaleFormattingPatch {

    private LocaleFormattingPatch() {
    }

    public static FastDateFormat getDayFormatterOverride(LocaleController localeController) {
        if (localeController == null || !AppearanceSettingsPatch.isTimeWithSecondsEnabled()) {
            return null;
        }
        return localeController.getFormatterDayWithSeconds();
    }

    public static String formatShortNumberOverride(int number, int[] rounded) {
        if (!AppearanceSettingsPatch.isRoundedNumbersDisabled()) {
            return null;
        }
        if (rounded != null && rounded.length > 0) {
            rounded[0] = number;
        }
        if (AppearanceSettingsPatch.isThousandsSeparatorEnabled()) {
            return LocaleController.formatNumber(number, ' ');
        }
        return Integer.toString(number);
    }
}
