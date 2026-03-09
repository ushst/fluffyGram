package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;

public final class AppearanceSettingsHook {

    public interface Listener {
        void onAppearanceSettingsChanged();
    }

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

    public static int getDialogsTitleMode() {
        return AppearanceSettingsPatch.getDialogsTitleMode();
    }

    public static void setDialogsTitleMode(int mode) {
        AppearanceSettingsPatch.setDialogsTitleMode(mode);
    }

    public static int getDialogsAppTitleMode() {
        return AppearanceSettingsPatch.getDialogsAppTitleMode();
    }

    public static void setDialogsAppTitleMode(int mode) {
        AppearanceSettingsPatch.setDialogsAppTitleMode(mode);
    }

    public static String getDialogsAppTitleCustom() {
        return AppearanceSettingsPatch.getDialogsAppTitleCustom();
    }

    public static void setDialogsAppTitleCustom(String value) {
        AppearanceSettingsPatch.setDialogsAppTitleCustom(value);
    }

    public static void addListener(Listener listener) {
        if (listener == null) {
            return;
        }
        AppearanceSettingsPatch.addListener(listener::onAppearanceSettingsChanged);
    }
}
