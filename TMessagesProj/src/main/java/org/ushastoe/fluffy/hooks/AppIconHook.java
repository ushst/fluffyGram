package org.ushastoe.fluffy.hooks;

import org.telegram.ui.Cells.AppIconsSelectorCell;
import org.telegram.ui.LauncherIconController;
import org.ushastoe.fluffy.patches.AppIconPatch;

public final class AppIconHook {
    private AppIconHook() {
    }

    public static LauncherIconController.LauncherIcon[] getLauncherIcons() {
        return AppIconPatch.getLauncherIcons();
    }

    public static int getForegroundInsetDp(LauncherIconController.LauncherIcon icon) {
        return AppIconPatch.getForegroundInsetDp(icon);
    }

    public static int getPreviewBackgroundTintColor(LauncherIconController.LauncherIcon icon) {
        return AppIconPatch.getPreviewBackgroundTintColor(icon);
    }

    public static int getPreviewForegroundRes(LauncherIconController.LauncherIcon icon, int defaultRes) {
        return AppIconPatch.getPreviewForegroundRes(icon, defaultRes);
    }

    public static void bindPreviewIcon(AppIconsSelectorCell.AdaptiveIconImageView imageView, LauncherIconController.LauncherIcon icon, int legacyBackgroundOuterPaddingDp) {
        AppIconPatch.bindPreviewIcon(imageView, icon, legacyBackgroundOuterPaddingDp);
    }
}
