package org.ushastoe.fluffy.patches;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Components.ChatActivityEnterView;

public final class RoundVideoCameraMenuPatch {

    private RoundVideoCameraMenuPatch() {
    }

    public static void addChatMenuItems(ActionBarMenuItem headerItem, int switchFrontId, int switchBackId) {
        if (headerItem == null) {
            return;
        }
        if (!headerItem.hasSubItem(switchFrontId)) {
            headerItem.lazilyAddSubItem(switchFrontId, R.drawable.msg_camera, LocaleController.getString(R.string.FluffyRoundVideoCameraUseFront));
        }
        if (!headerItem.hasSubItem(switchBackId)) {
            headerItem.lazilyAddSubItem(switchBackId, R.drawable.msg_camera, LocaleController.getString(R.string.FluffyRoundVideoCameraUseBack));
        }
    }

    public static void updateChatMenuItems(ActionBarMenuItem headerItem, ChatActivityEnterView enterView, int switchFrontId, int switchBackId) {
        if (headerItem == null) {
            return;
        }
        boolean show = AppearanceSettingsPatch.isRoundVideoCameraFeatureEnabled() && enterView != null && enterView.hasRecordVideo();
        boolean useFrontByDefault = AppearanceSettingsPatch.useFrontRoundVideoCameraByDefault();
        headerItem.setSubItemShown(switchFrontId, show && !useFrontByDefault);
        headerItem.setSubItemShown(switchBackId, show && useFrontByDefault);
    }

    public static boolean onChatMenuItemClick(ChatActivityEnterView enterView, ActionBarMenuItem headerItem, int id, int switchFrontId, int switchBackId) {
        if (id != switchFrontId && id != switchBackId) {
            return false;
        }
        boolean useFront = id == switchFrontId;
        int mode = useFront ? AppearanceSettingsPatch.ROUND_VIDEO_CAMERA_FRONT : AppearanceSettingsPatch.ROUND_VIDEO_CAMERA_BACK;
        AppearanceSettingsPatch.setDefaultRoundVideoCameraMode(mode);
        AppearanceSettingsPatch.setRoundVideoCameraMode(mode);
        updateChatMenuItems(headerItem, enterView, switchFrontId, switchBackId);
        if (enterView != null) {
            enterView.invalidateRoundVideoCameraButton();
        }
        return true;
    }
}
