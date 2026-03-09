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

    public static String getConfigSignature() {
        return MainTabsConfigPatch.getConfigSignature();
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
