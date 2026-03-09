package org.ushastoe.fluffy.patches;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.LauncherIconController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.AppIconsSelectorCell;

public final class AppIconPatch {
    private static final LauncherIconController.LauncherIcon[] LAUNCHER_ICONS = new LauncherIconController.LauncherIcon[] {
            LauncherIconController.LauncherIcon.DEFAULT,
            LauncherIconController.LauncherIcon.CLASSIC,
            LauncherIconController.LauncherIcon.VINTAGE,
            LauncherIconController.LauncherIcon.AQUA,
            LauncherIconController.LauncherIcon.PREMIUM,
            LauncherIconController.LauncherIcon.TURBO,
            LauncherIconController.LauncherIcon.NOX
    };

    private AppIconPatch() {
    }

    public static LauncherIconController.LauncherIcon[] getLauncherIcons() {
        return LAUNCHER_ICONS.clone();
    }

    public static int getForegroundInsetDp(LauncherIconController.LauncherIcon icon) {
        if (icon == LauncherIconController.LauncherIcon.DEFAULT) {
            return 8;
        }
        return 0;
    }

    public static int getPreviewBackgroundTintColor(LauncherIconController.LauncherIcon icon) {
        if (icon == LauncherIconController.LauncherIcon.DEFAULT) {
            return Theme.getColor(Theme.key_windowBackgroundWhiteValueText);
        }
        return 0;
    }

    public static int getPreviewForegroundRes(LauncherIconController.LauncherIcon icon, int defaultRes) {
        if (icon == LauncherIconController.LauncherIcon.DEFAULT) {
            return R.drawable.fluffy_launcher_group43_preview_foreground_scaled;
        }
        return defaultRes;
    }

    public static void bindPreviewIcon(AppIconsSelectorCell.AdaptiveIconImageView imageView, LauncherIconController.LauncherIcon icon, int legacyBackgroundOuterPaddingDp) {
        Drawable background = createPreviewBackground(imageView, icon);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && background != null) {
            Drawable foreground = ContextCompat.getDrawable(imageView.getContext(), getPreviewForegroundRes(icon, icon.foreground));
            if (foreground != null) {
                imageView.setImageDrawable(new AdaptiveIconDrawable(background, foreground));
                imageView.setForeground((Drawable) null);
                imageView.setForegroundInset(0);
                imageView.setBackgroundOuterPadding(0);
                return;
            }
        }

        imageView.setImageDrawable(background);
        imageView.setForeground(getPreviewForegroundRes(icon, icon.foreground));
        imageView.setForegroundInset(AndroidUtilities.dp(getForegroundInsetDp(icon)));
        imageView.setBackgroundOuterPadding(AndroidUtilities.dp(legacyBackgroundOuterPaddingDp));
    }

    private static Drawable createPreviewBackground(AppIconsSelectorCell.AdaptiveIconImageView imageView, LauncherIconController.LauncherIcon icon) {
        Drawable background = ContextCompat.getDrawable(imageView.getContext(), icon.background);
        int tintColor = getPreviewBackgroundTintColor(icon);
        if (background != null && tintColor != 0) {
            background = background.mutate();
            background.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
        }
        return background;
    }
}
