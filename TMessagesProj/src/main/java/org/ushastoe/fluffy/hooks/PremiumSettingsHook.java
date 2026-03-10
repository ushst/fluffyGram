package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.PremiumSettingsPatch;

public final class PremiumSettingsHook {

    private PremiumSettingsHook() {
    }

    public static boolean useLocalAnonymousStoryView() {
        return PremiumSettingsPatch.useLocalAnonymousStoryView();
    }

    public static void setUseLocalAnonymousStoryView(boolean enabled) {
        PremiumSettingsPatch.setUseLocalAnonymousStoryView(enabled);
    }
}
