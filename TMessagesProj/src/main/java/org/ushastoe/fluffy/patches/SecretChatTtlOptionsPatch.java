package org.ushastoe.fluffy.patches;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

public final class SecretChatTtlOptionsPatch {

    private static final int[] TTL_OPTIONS = new int[] {
            0,
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            30, 60,
            120, 180, 240, 300,
            600, 900, 1200, 1800, 2400,
            3600,
            7200, 10800, 18000, 28800, 43200, 57600,
            86400, 172800, 259200, 604800, 2764800
    };

    private SecretChatTtlOptionsPatch() {
    }

    public static int getPickerMaxValue() {
        return TTL_OPTIONS.length - 1;
    }

    public static int getPickerValueForTtl(int ttl) {
        for (int i = 0; i < TTL_OPTIONS.length; i++) {
            if (TTL_OPTIONS[i] == ttl) {
                return i;
            }
        }
        return 0;
    }

    public static String formatPickerValue(int value) {
        int ttl = getTtlForPickerValue(value);
        if (ttl == 0) {
            return LocaleController.getString(R.string.ShortMessageLifetimeForever);
        }
        return LocaleController.formatTTLString(ttl);
    }

    public static int getTtlForPickerValue(int value) {
        if (value < 0 || value >= TTL_OPTIONS.length) {
            return 0;
        }
        return TTL_OPTIONS[value];
    }
}
