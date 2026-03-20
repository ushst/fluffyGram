package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.SecretChatTtlOptionsPatch;

public final class SecretChatTtlOptionsHook {

    private SecretChatTtlOptionsHook() {
    }

    public static int getPickerMaxValue() {
        return SecretChatTtlOptionsPatch.getPickerMaxValue();
    }

    public static int getPickerValueForTtl(int ttl) {
        return SecretChatTtlOptionsPatch.getPickerValueForTtl(ttl);
    }

    public static String formatPickerValue(int value) {
        return SecretChatTtlOptionsPatch.formatPickerValue(value);
    }

    public static int getTtlForPickerValue(int value) {
        return SecretChatTtlOptionsPatch.getTtlForPickerValue(value);
    }
}
