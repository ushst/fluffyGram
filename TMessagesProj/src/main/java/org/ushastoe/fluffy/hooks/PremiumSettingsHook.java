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

    public static boolean isLocalMessageFakeEditEnabled() {
        return PremiumSettingsPatch.isLocalMessageFakeEditEnabled();
    }

    public static void setLocalMessageFakeEditEnabled(boolean enabled) {
        PremiumSettingsPatch.setLocalMessageFakeEditEnabled(enabled);
    }

    public static boolean isLocalMessageHistoryEnabled() {
        return PremiumSettingsPatch.isLocalMessageHistoryEnabled();
    }

    public static void setLocalMessageHistoryEnabled(boolean enabled) {
        PremiumSettingsPatch.setLocalMessageHistoryEnabled(enabled);
    }

    public static boolean isSaveDeletedMessagesEnabled() {
        return PremiumSettingsPatch.isSaveDeletedMessagesEnabled();
    }

    public static void setSaveDeletedMessagesEnabled(boolean enabled) {
        PremiumSettingsPatch.setSaveDeletedMessagesEnabled(enabled);
    }

    public static int getDeletedMessageMarkerMode() {
        return PremiumSettingsPatch.getDeletedMessageMarkerMode();
    }

    public static void setDeletedMessageMarkerMode(int mode) {
        PremiumSettingsPatch.setDeletedMessageMarkerMode(mode);
    }
}
