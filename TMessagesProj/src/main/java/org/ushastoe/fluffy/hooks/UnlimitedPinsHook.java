package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.UnlimitedPinsPatch;

public final class UnlimitedPinsHook {

    private UnlimitedPinsHook() {
    }

    public static boolean isEnabled() {
        return UnlimitedPinsPatch.isEnabled();
    }

    public static void setEnabled(boolean enabled) {
        UnlimitedPinsPatch.setEnabled(enabled);
    }

    public static int overrideDialogsPinnedLimit(int currentLimit, int folderId, boolean hasFilter) {
        return UnlimitedPinsPatch.overrideDialogsPinnedLimit(currentLimit, folderId, hasFilter);
    }

    public static boolean shouldSkipPinnedDialogsSync(int folderId) {
        return UnlimitedPinsPatch.shouldSkipPinnedDialogsSync(folderId);
    }
}
