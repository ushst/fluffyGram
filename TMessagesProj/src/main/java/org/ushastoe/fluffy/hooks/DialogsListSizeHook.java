package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.DialogsListSizePatch;

public final class DialogsListSizeHook {

    private DialogsListSizeHook() {
    }

    public static void applyThemePaintSizes() {
        DialogsListSizePatch.applyThemePaintSizes();
    }

    public static void applyDialogCellTextSizes(boolean threeLines) {
        DialogsListSizePatch.applyDialogCellTextSizes(threeLines);
    }

    public static int adjustDialogCellHeight(int defaultDp) {
        return DialogsListSizePatch.adjustDialogCellHeight(defaultDp);
    }

    public static int adjustDialogThumbSize(int defaultDp) {
        return DialogsListSizePatch.adjustDialogThumbSize(defaultDp);
    }

    public static int getDialogAvatarSize(int defaultDp) {
        return DialogsListSizePatch.getDialogAvatarSize(defaultDp);
    }

    public static int getDialogAvatarTop(int defaultTopDp, int defaultSizeDp, int scaledSizeDp) {
        return DialogsListSizePatch.getDialogAvatarTop(defaultTopDp, defaultSizeDp, scaledSizeDp);
    }

    public static int getDialogAvatarRoundRadius(int defaultDp) {
        return DialogsListSizePatch.getDialogAvatarRoundRadius(defaultDp);
    }
}
