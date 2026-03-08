package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;

public final class AppearanceSettingsHook {
    private AppearanceSettingsHook() {
    }

    public static boolean isChannelPostStarsOfferHidden() {
        return AppearanceSettingsPatch.isChannelPostStarsOfferHidden();
    }

    public static void setChannelPostStarsOfferHidden(boolean hidden) {
        AppearanceSettingsPatch.setChannelPostStarsOfferHidden(hidden);
    }

    public static boolean shouldShowChannelPostStarsOffer() {
        return AppearanceSettingsPatch.shouldShowChannelPostStarsOffer();
    }

    public static boolean shouldShowChannelPostStarsUi(MessageObject messageObject) {
        return AppearanceSettingsPatch.shouldShowChannelPostStarsUi(messageObject);
    }

    public static boolean shouldShowReaction(MessageObject messageObject, TLRPC.Reaction reaction) {
        return AppearanceSettingsPatch.shouldShowReaction(messageObject, reaction);
    }
}
