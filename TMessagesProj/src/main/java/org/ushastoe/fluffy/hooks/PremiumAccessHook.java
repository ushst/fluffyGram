package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.PremiumAccessPatch;

public final class PremiumAccessHook {

    private PremiumAccessHook() {
    }

    public static boolean hasPremiumAccess() {
        return PremiumAccessPatch.hasPremiumAccess();
    }

    public static boolean importPremiumAccessToken(String token) {
        return PremiumAccessPatch.importPremiumAccessToken(token);
    }

    public static void clearPremiumAccessToken() {
        PremiumAccessPatch.clearPremiumAccessToken();
    }

    public static boolean hasConfiguredPublicKey() {
        return PremiumAccessPatch.hasConfiguredPublicKey();
    }
}
