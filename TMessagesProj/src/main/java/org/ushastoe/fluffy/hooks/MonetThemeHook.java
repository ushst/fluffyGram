package org.ushastoe.fluffy.hooks;

import android.util.SparseIntArray;

import org.telegram.ui.ActionBar.Theme;
import org.ushastoe.fluffy.patches.MonetThemePatch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public final class MonetThemeHook {
    private MonetThemeHook() {
    }

    /**
     * {@code ThemeInfo}'s constructor is package-private, so the two instances are created
     * on the {@code Theme} side and only filled in here.
     */
    public static void registerThemes(ArrayList<Theme.ThemeInfo> themes,
                                      HashMap<String, Theme.ThemeInfo> themesDict,
                                      Theme.ThemeInfo light, Theme.ThemeInfo dark) {
        MonetThemePatch.registerThemes(themes, themesDict, light, dark);
    }

    /** @return generated colours for a Monet asset name, or {@code null} for any other theme. */
    public static SparseIntArray getThemeFileValues(String assetName) {
        return assetName == null ? null : MonetThemePatch.getColorsForAsset(assetName);
    }

    /** @return {@code null} for any theme that is not a Monet one. */
    public static Boolean resolveThemeIsDark(String themeName) {
        return themeName == null ? null : MonetThemePatch.resolveThemeIsDark(themeName);
    }

    /** @return a resolved .attheme file for a Monet asset name, or {@code null}. */
    public static File getGeneratedThemeFile(String assetName) {
        return assetName == null ? null : MonetThemePatch.getGeneratedThemeFile(assetName);
    }

    public static void onConfigurationChanged() {
        MonetThemePatch.onConfigurationChanged();
    }
}
