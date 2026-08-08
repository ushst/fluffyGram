package org.ushastoe.fluffy.patches;

import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;
import org.ushastoe.fluffy.monet.MonetPalette;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds the Android system accent as one more colour in the accent row of the stock themes,
 * so "Classic" or "Dark" can simply be tinted with the wallpaper colour.
 *
 * <p>The accent is rebuilt from the live palette on every launch and deliberately never
 * persisted: {@code Theme.saveThemeAccents} only serialises accents with an id of 100 or
 * above, and it derives how many to write from {@code themeAccents.size() -
 * defaultAccentCount}. Using a low id keeps ours out of the saved blob, and bumping
 * {@code defaultAccentCount} alongside keeps that subtraction honest — without it the
 * header would claim one more accent than the body contains and corrupt the user's own.
 */
public final class MonetSystemAccentPatch {

    /**
     * Free across every stock theme: they use 0..18 plus 99 for the Blue default, and user
     * accents start at 100. Sorting puts non-default accents in descending id order, so 90
     * lands first in the row, right after the theme's own default swatch.
     */
    private static final int ACCENT_ID = 90;

    /** Mid-tone of the wallpaper's primary palette — the closest thing to "the" system colour. */
    private static final String ACCENT_TOKEN = "a1_500";

    private static final String[] TARGET_THEMES = {
            "Blue", "Day", "Night", "Dark Blue", "Arctic Blue"
    };

    /** Creates a {@code ThemeAccent}; its constructor is package-private to {@code Theme}. */
    public interface ThemeAccentFactory {
        Theme.ThemeAccent create();
    }

    private MonetSystemAccentPatch() {
    }

    /** Called from {@code Theme}'s static initialiser once the stock themes exist. */
    public static void addSystemAccents(HashMap<String, Theme.ThemeInfo> themesDict,
                                        ThemeAccentFactory factory) {
        if (themesDict == null || factory == null || !MonetPalette.isSystemPaletteAvailable()) {
            return;
        }
        Integer color = systemAccentColor();
        if (color == null) {
            return;
        }
        for (String themeName : TARGET_THEMES) {
            Theme.ThemeInfo themeInfo = themesDict.get(themeName);
            if (themeInfo == null || themeInfo.themeAccents == null
                    || themeInfo.themeAccentsMap == null
                    || themeInfo.themeAccentsMap.get(ACCENT_ID) != null) {
                continue;
            }
            Theme.ThemeAccent accent = factory.create();
            if (accent == null) {
                continue;
            }
            accent.id = ACCENT_ID;
            accent.parentTheme = themeInfo;
            accent.accentColor = color;
            themeInfo.themeAccentsMap.put(ACCENT_ID, accent);
            themeInfo.themeAccents.add(insertIndex(themeInfo), accent);
            // Keeps saveThemeAccents' "size minus defaults" arithmetic correct.
            themeInfo.defaultAccentCount++;
        }
    }

    /**
     * The list is already in {@code sortAccents} order here, and our id outranks every other
     * built-in one, so the slot right after the theme's home accent is where it belongs.
     */
    private static int insertIndex(Theme.ThemeInfo themeInfo) {
        for (int i = 0; i < themeInfo.themeAccents.size(); i++) {
            if (!Theme.isHome(themeInfo.themeAccents.get(i))) {
                return i;
            }
        }
        return themeInfo.themeAccents.size();
    }

    /** Refreshes the accent colour after the wallpaper palette changed. */
    public static void onSystemPaletteChanged() {
        Integer color = systemAccentColor();
        if (color == null) {
            return;
        }
        boolean activeChanged = false;
        Theme.ThemeInfo active = Theme.getActiveTheme();
        for (String themeName : TARGET_THEMES) {
            Theme.ThemeInfo themeInfo = Theme.getTheme(themeName);
            if (themeInfo == null || themeInfo.themeAccentsMap == null) {
                continue;
            }
            Theme.ThemeAccent accent = themeInfo.themeAccentsMap.get(ACCENT_ID);
            if (accent == null || accent.accentColor == color) {
                continue;
            }
            accent.accentColor = color;
            if (themeInfo == active && themeInfo.currentAccentId == ACCENT_ID) {
                activeChanged = true;
            }
        }
        if (!activeChanged) {
            return;
        }
        final Theme.ThemeInfo themeToReapply = active;
        final boolean nightTheme = Theme.isCurrentThemeNight();
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance()
                .postNotificationName(NotificationCenter.needSetDayNightTheme,
                        themeToReapply, nightTheme, null, ACCENT_ID));
    }

    private static Integer systemAccentColor() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        Map<String, Integer> tokens = MonetPalette.fromSystem(context);
        return tokens == null ? null : tokens.get(ACCENT_TOKEN);
    }
}
