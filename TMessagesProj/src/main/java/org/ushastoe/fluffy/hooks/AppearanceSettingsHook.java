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

    public static boolean useFluffyNotificationIcon() {
        return AppearanceSettingsPatch.useFluffyNotificationIcon();
    }

    public static void setUseFluffyNotificationIcon(boolean enabled) {
        AppearanceSettingsPatch.setUseFluffyNotificationIcon(enabled);
    }

    public static String getAppFontKey() {
        return AppearanceSettingsPatch.getAppFontKey();
    }

    public static void setAppFontKey(String value) {
        AppearanceSettingsPatch.setAppFontKey(value);
    }

    public static int getDoubleTapInAction() {
        return AppearanceSettingsPatch.getDoubleTapInAction();
    }

    public static void setDoubleTapInAction(int action) {
        AppearanceSettingsPatch.setDoubleTapInAction(action);
    }

    public static int getDoubleTapOutAction() {
        return AppearanceSettingsPatch.getDoubleTapOutAction();
    }

    public static void setDoubleTapOutAction(int action) {
        AppearanceSettingsPatch.setDoubleTapOutAction(action);
    }

    public static boolean isTimeWithSecondsEnabled() {
        return AppearanceSettingsPatch.isTimeWithSecondsEnabled();
    }

    public static void setTimeWithSecondsEnabled(boolean enabled) {
        AppearanceSettingsPatch.setTimeWithSecondsEnabled(enabled);
    }

    public static boolean isRoundedNumbersDisabled() {
        return AppearanceSettingsPatch.isRoundedNumbersDisabled();
    }

    public static void setRoundedNumbersDisabled(boolean disabled) {
        AppearanceSettingsPatch.setRoundedNumbersDisabled(disabled);
    }

    public static boolean isThousandsSeparatorEnabled() {
        return AppearanceSettingsPatch.isThousandsSeparatorEnabled();
    }

    public static void setThousandsSeparatorEnabled(boolean enabled) {
        AppearanceSettingsPatch.setThousandsSeparatorEnabled(enabled);
    }

    public static int getDialogsListScale() {
        return AppearanceSettingsPatch.getDialogsListScale();
    }

    public static void setDialogsListScale(int scale) {
        AppearanceSettingsPatch.setDialogsListScale(scale);
    }

    public static boolean isCenterChatHeaderEnabled() {
        return AppearanceSettingsPatch.isCenterChatHeaderEnabled();
    }

    public static void setCenterChatHeaderEnabled(boolean enabled) {
        AppearanceSettingsPatch.setCenterChatHeaderEnabled(enabled);
    }

    public static int getMapProvider() {
        return AppearanceSettingsPatch.getMapProvider();
    }

    public static void setMapProvider(int provider) {
        AppearanceSettingsPatch.setMapProvider(provider);
    }

    public static boolean isRoundVideoCameraFeatureEnabled() {
        return AppearanceSettingsPatch.isRoundVideoCameraFeatureEnabled();
    }

    public static void setRoundVideoCameraFeatureEnabled(boolean enabled) {
        AppearanceSettingsPatch.setRoundVideoCameraFeatureEnabled(enabled);
    }

    public static int getRoundVideoCameraMode() {
        return AppearanceSettingsPatch.getRoundVideoCameraMode();
    }

    public static void setRoundVideoCameraMode(int mode) {
        AppearanceSettingsPatch.setRoundVideoCameraMode(mode);
    }

    public static boolean useFrontRoundVideoCamera() {
        return AppearanceSettingsPatch.useFrontRoundVideoCamera();
    }

    public static int getDefaultRoundVideoCameraMode() {
        return AppearanceSettingsPatch.getDefaultRoundVideoCameraMode();
    }

    public static void setDefaultRoundVideoCameraMode(int mode) {
        AppearanceSettingsPatch.setDefaultRoundVideoCameraMode(mode);
    }

    public static boolean useFrontRoundVideoCameraByDefault() {
        return AppearanceSettingsPatch.useFrontRoundVideoCameraByDefault();
    }

    public static void addListener(Listener listener) {
        if (listener == null) {
            return;
        }
        AppearanceSettingsPatch.addListener(listener::onAppearanceSettingsChanged);
    }
}
