package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.patches.MainTabsConfigPatch;

public final class MainTabsConfigHook {

    private MainTabsConfigHook() {
    }

    public static int[] getVisibleTabTypes() {
        return MainTabsConfigPatch.getVisibleTabTypes();
    }

    public static int[] getOptionalVisibleTypes() {
        return MainTabsConfigPatch.getOptionalVisibleTypes();
    }

    public static int[] getHiddenOptionalTypes() {
        return MainTabsConfigPatch.getHiddenOptionalTypes();
    }

    public static void moveOptionalTabUp(int type) {
        MainTabsConfigPatch.moveOptionalTabUp(type);
    }

    public static void moveOptionalTabDown(int type) {
        MainTabsConfigPatch.moveOptionalTabDown(type);
    }

    public static void showOptionalTab(int type) {
        MainTabsConfigPatch.showOptionalTab(type);
    }

    public static void hideOptionalTab(int type) {
        MainTabsConfigPatch.hideOptionalTab(type);
    }

    public static boolean isOptionalTabVisible(int type) {
        return MainTabsConfigPatch.isOptionalTabVisible(type);
    }

    public static boolean canHideOptionalTab(int type) {
        return MainTabsConfigPatch.canHideOptionalTab(type);
    }

    public static long[] getQuickDialogIds() {
        return MainTabsConfigPatch.getQuickDialogIds();
    }

    public static void addQuickDialog(long dialogId) {
        MainTabsConfigPatch.addQuickDialog(dialogId);
    }

    public static void removeQuickDialog(long dialogId) {
        MainTabsConfigPatch.removeQuickDialog(dialogId);
    }

    public static void moveQuickDialogUp(long dialogId) {
        MainTabsConfigPatch.moveQuickDialogUp(dialogId);
    }

    public static void moveQuickDialogDown(long dialogId) {
        MainTabsConfigPatch.moveQuickDialogDown(dialogId);
    }

    public static String getQuickDialogCustomLabel(long dialogId) {
        return MainTabsConfigPatch.getQuickDialogCustomLabel(dialogId);
    }

    public static void setQuickDialogCustomLabel(long dialogId, String label) {
        MainTabsConfigPatch.setQuickDialogCustomLabel(dialogId, label);
    }

    public static int getQuickDialogLongPressAction(long dialogId) {
        return MainTabsConfigPatch.getQuickDialogLongPressAction(dialogId);
    }

    public static int getQuickDialogDoubleTapAction(long dialogId) {
        return MainTabsConfigPatch.getQuickDialogDoubleTapAction(dialogId);
    }

    public static void setQuickDialogDoubleTapAction(long dialogId, int action) {
        MainTabsConfigPatch.setQuickDialogDoubleTapAction(dialogId, action);
    }

    public static void setQuickDialogLongPressAction(long dialogId, int action) {
        MainTabsConfigPatch.setQuickDialogLongPressAction(dialogId, action);
    }

    public static long getQuickDialogLongPressTargetDialogId(long dialogId) {
        return MainTabsConfigPatch.getQuickDialogLongPressTargetDialogId(dialogId);
    }

    public static long getQuickDialogDoubleTapTargetDialogId(long dialogId) {
        return MainTabsConfigPatch.getQuickDialogDoubleTapTargetDialogId(dialogId);
    }

    public static void setQuickDialogLongPressTargetDialogId(long dialogId, long targetDialogId) {
        MainTabsConfigPatch.setQuickDialogLongPressTargetDialogId(dialogId, targetDialogId);
    }

    public static void setQuickDialogDoubleTapTargetDialogId(long dialogId, long targetDialogId) {
        MainTabsConfigPatch.setQuickDialogDoubleTapTargetDialogId(dialogId, targetDialogId);
    }

    public static String getConfigSignature() {
        return MainTabsConfigPatch.getConfigSignature();
    }

    public static boolean isOpenSavedMessagesOnDoubleTapEnabled() {
        return MainTabsConfigPatch.isOpenSavedMessagesOnDoubleTapEnabled();
    }

    public static void setOpenSavedMessagesOnDoubleTapEnabled(boolean enabled) {
        MainTabsConfigPatch.setOpenSavedMessagesOnDoubleTapEnabled(enabled);
    }

    public static int getDoubleTapAction(int tabType) {
        return MainTabsConfigPatch.getDoubleTapAction(tabType);
    }

    public static void setDoubleTapAction(int tabType, int action) {
        MainTabsConfigPatch.setDoubleTapAction(tabType, action);
    }

    public static int getLongPressAction(int tabType) {
        return MainTabsConfigPatch.getLongPressAction(tabType);
    }

    public static void setLongPressAction(int tabType, int action) {
        MainTabsConfigPatch.setLongPressAction(tabType, action);
    }

    public static long getDoubleTapTargetDialogId(int tabType) {
        return MainTabsConfigPatch.getDoubleTapTargetDialogId(tabType);
    }

    public static long getLongPressTargetDialogId(int tabType) {
        return MainTabsConfigPatch.getLongPressTargetDialogId(tabType);
    }

    public static void setDoubleTapTargetDialogId(int tabType, long dialogId) {
        MainTabsConfigPatch.setDoubleTapTargetDialogId(tabType, dialogId);
    }

    public static void setLongPressTargetDialogId(int tabType, long dialogId) {
        MainTabsConfigPatch.setLongPressTargetDialogId(tabType, dialogId);
    }

    public static long[] getQuickContactUserIds() {
        return MainTabsConfigPatch.getQuickContactUserIds();
    }

    public static void addQuickContact(long userId) {
        MainTabsConfigPatch.addQuickContact(userId);
    }

    public static void removeQuickContact(long userId) {
        MainTabsConfigPatch.removeQuickContact(userId);
    }

    public static void moveQuickContactUp(long userId) {
        MainTabsConfigPatch.moveQuickContactUp(userId);
    }

    public static void moveQuickContactDown(long userId) {
        MainTabsConfigPatch.moveQuickContactDown(userId);
    }

    public static int getTabTypeAtPosition(int[] visibleTypes, int position) {
        return MainTabsConfigPatch.getTabTypeAtPosition(visibleTypes, position);
    }

    public static int getPositionForType(int[] visibleTypes, int type) {
        return MainTabsConfigPatch.getPositionForType(visibleTypes, type);
    }

    public static int findTypeByFragment(BaseFragment fragment) {
        return fragment == null ? -1 : MainTabsConfigPatch.findTypeByFragmentClassName(fragment.getClass().getName());
    }
}
