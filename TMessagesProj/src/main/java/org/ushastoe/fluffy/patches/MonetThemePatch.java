package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.SparseIntArray;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;
import org.ushastoe.fluffy.monet.MonetPalette;
import org.ushastoe.fluffy.monet.MonetThemeGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Material You themes generated inside the app.
 *
 * <p>Six extra {@link Theme.ThemeInfo} entries are registered next to Blue/Day/Night — one
 * per accent palette (primary, secondary, tertiary) per side — all resolved from the two
 * token templates in {@code assets/fluffy/}. {@code Theme.getThemeFileValues} is intercepted
 * for their asset keys, so selecting, previewing and night-switching a Monet theme all go
 * through Telegram's own code paths — nothing is exported or imported.
 */
public final class MonetThemePatch {

    public static final String THEME_NAME_LIGHT = "Monet";
    public static final String THEME_NAME_DARK = "Monet Dark";

    private static final String PREFS_NAME = "fluffy_monet_settings";
    private static final String KEY_PALETTE_SOURCE = "monet_palette_source";
    private static final String KEY_SEED_COLOR = "monet_seed_color";
    private static final String KEY_SCHEME = "monet_scheme";
    private static final String KEY_AMOLED = "monet_amoled";
    private static final String KEY_GRADIENT_BUBBLES = "monet_gradient_bubbles";
    private static final String KEY_GRADIENT_AVATARS = "monet_gradient_avatars";
    private static final String KEY_MONOCHROME_NAMES = "monet_monochrome_names";
    private static final String KEY_VISIBLE_DIVIDERS = "monet_visible_dividers";
    private static final String KEY_INVERT_OUTGOING = "monet_invert_outgoing";

    public static final int PALETTE_SOURCE_SYSTEM = 0;
    public static final int PALETTE_SOURCE_CUSTOM = 1;

    /**
     * One theme per accent palette per side. They are separate entries in the theme list
     * rather than a colour picker, so the standard day/night selectors can point at them.
     * Built-in themes occupy sortIndex 1..5, so Monet starts at 6.
     */
    private static final class Variant {
        final String themeName;
        final String assetKey;
        final boolean dark;
        final int accentPalette;
        final int sortIndex;

        Variant(String themeName, String assetKey,
                boolean dark, int accentPalette, int sortIndex) {
            this.themeName = themeName;
            this.assetKey = assetKey;
            this.dark = dark;
            this.accentPalette = accentPalette;
            this.sortIndex = sortIndex;
        }
    }

    private static final Variant[] VARIANTS = {
            new Variant("Monet", "fluffy/monet_light.attheme",
                    false, MonetPalette.ACCENT_PRIMARY, 6),
            new Variant("Monet Dark", "fluffy/monet_dark.attheme",
                    true, MonetPalette.ACCENT_PRIMARY, 7),
            new Variant("Monet Secondary", "fluffy/monet_secondary_light.attheme",
                    false, MonetPalette.ACCENT_SECONDARY, 8),
            new Variant("Monet Secondary Dark", "fluffy/monet_secondary_dark.attheme",
                    true, MonetPalette.ACCENT_SECONDARY, 9),
            new Variant("Monet Tertiary", "fluffy/monet_tertiary_light.attheme",
                    false, MonetPalette.ACCENT_TERTIARY, 10),
            new Variant("Monet Tertiary Dark", "fluffy/monet_tertiary_dark.attheme",
                    true, MonetPalette.ACCENT_TERTIARY, 11),
    };

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public interface Listener {
        void onMonetSettingsChanged();
    }

    private MonetThemePatch() {
    }

    // region theme registration

    /** Creates a {@code ThemeInfo}; its constructor is package-private to {@code Theme}. */
    public interface ThemeInfoFactory {
        Theme.ThemeInfo create();
    }

    /** Called from {@code Theme}'s static initialiser, right after the built-in themes. */
    public static void registerThemes(ArrayList<Theme.ThemeInfo> themes,
                                      HashMap<String, Theme.ThemeInfo> themesDict,
                                      ThemeInfoFactory factory) {
        if (themes == null || themesDict == null || factory == null
                || themesDict.containsKey(VARIANTS[0].themeName)) {
            return;
        }
        for (Variant variant : VARIANTS) {
            Theme.ThemeInfo themeInfo = factory.create();
            if (themeInfo == null) {
                return;
            }
            themeInfo.name = variant.themeName;
            themeInfo.assetName = variant.assetKey;
            themeInfo.sortIndex = variant.sortIndex;
            themes.add(themeInfo);
            themesDict.put(variant.themeName, themeInfo);
        }
        refreshPreviewColors();
    }

    public static boolean isMonetTheme(String themeName) {
        return variantByName(themeName) != null;
    }

    /**
     * Tells {@code ThemeInfo.isDark()} which side a Monet theme is on. Without this it would
     * try to parse {@code pathToFile}, which our themes do not have.
     *
     * @return {@code null} for any theme that is not ours.
     */
    public static Boolean resolveThemeIsDark(String themeName) {
        Variant variant = variantByName(themeName);
        return variant == null ? null : variant.dark;
    }

    /** Resolves the token template behind {@code assetName} into live colours. */
    public static SparseIntArray getColorsForAsset(String assetName) {
        Variant variant = variantByAsset(assetName);
        if (variant == null) {
            return null;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return MonetThemeGenerator.generate(context, buildOptions(variant));
    }

    private static Variant variantByName(String themeName) {
        for (Variant variant : VARIANTS) {
            if (variant.themeName.equals(themeName)) {
                return variant;
            }
        }
        return null;
    }

    private static Variant variantByAsset(String assetName) {
        for (Variant variant : VARIANTS) {
            if (variant.assetKey.equals(assetName)) {
                return variant;
            }
        }
        return null;
    }

    /**
     * Materialises a Monet theme as a real {@code .attheme} file, so that sharing or
     * exporting it hands over resolved colours instead of the raw token template.
     *
     * @return {@code null} for any asset that is not ours.
     */
    public static File getGeneratedThemeFile(String assetName) {
        Variant variant = variantByAsset(assetName);
        if (variant == null) {
            return null;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        File file = new File(ApplicationLoader.getFilesDirFixed(),
                variant.themeName.toLowerCase(Locale.US).replace(' ', '_') + ".attheme");
        if (!MonetThemeGenerator.writeTo(context, buildOptions(variant), file)) {
            return null;
        }
        return file;
    }

    // endregion

    // region settings

    private static MonetThemeGenerator.Options buildOptions(Variant variant) {
        MonetThemeGenerator.Options options = new MonetThemeGenerator.Options();
        options.dark = variant.dark;
        options.accentPalette = variant.accentPalette;
        options.amoled = isAmoled();
        options.gradientBubbles = useGradientBubbles();
        options.gradientAvatars = useGradientAvatars();
        options.monochromeNames = useMonochromeNames();
        options.visibleDividers = useVisibleDividers();
        options.invertOutgoing = invertOutgoing();
        options.useSystemPalette = getPaletteSource() == PALETTE_SOURCE_SYSTEM;
        options.seedColor = getSeedColor();
        options.scheme = getScheme();
        return options;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getPaletteSource() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return PALETTE_SOURCE_SYSTEM;
        }
        int source = preferences.getInt(KEY_PALETTE_SOURCE, PALETTE_SOURCE_SYSTEM);
        if (source != PALETTE_SOURCE_CUSTOM) {
            return PALETTE_SOURCE_SYSTEM;
        }
        return source;
    }

    public static void setPaletteSource(int source) {
        putInt(KEY_PALETTE_SOURCE, source == PALETTE_SOURCE_CUSTOM
                ? PALETTE_SOURCE_CUSTOM : PALETTE_SOURCE_SYSTEM);
    }

    public static int getSeedColor() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return MonetPalette.DEFAULT_SEED;
        }
        int seed = preferences.getInt(KEY_SEED_COLOR, 0);
        return seed == 0 ? MonetPalette.DEFAULT_SEED : seed;
    }

    public static void setSeedColor(int color) {
        putInt(KEY_SEED_COLOR, color);
    }

    public static int getScheme() {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return MonetPalette.SCHEME_TONAL_SPOT;
        }
        int scheme = preferences.getInt(KEY_SCHEME, MonetPalette.SCHEME_TONAL_SPOT);
        if (scheme < 0 || scheme >= MonetPalette.SCHEME_COUNT) {
            return MonetPalette.SCHEME_TONAL_SPOT;
        }
        return scheme;
    }

    public static void setScheme(int scheme) {
        putInt(KEY_SCHEME, scheme);
    }

    public static boolean isAmoled() {
        return getBoolean(KEY_AMOLED, false);
    }

    public static void setAmoled(boolean enabled) {
        putBoolean(KEY_AMOLED, enabled);
    }

    public static boolean useGradientBubbles() {
        return getBoolean(KEY_GRADIENT_BUBBLES, false);
    }

    public static void setUseGradientBubbles(boolean enabled) {
        putBoolean(KEY_GRADIENT_BUBBLES, enabled);
    }

    public static boolean useGradientAvatars() {
        return getBoolean(KEY_GRADIENT_AVATARS, false);
    }

    public static void setUseGradientAvatars(boolean enabled) {
        putBoolean(KEY_GRADIENT_AVATARS, enabled);
    }

    public static boolean useMonochromeNames() {
        return getBoolean(KEY_MONOCHROME_NAMES, false);
    }

    public static void setUseMonochromeNames(boolean enabled) {
        putBoolean(KEY_MONOCHROME_NAMES, enabled);
    }

    public static boolean useVisibleDividers() {
        return getBoolean(KEY_VISIBLE_DIVIDERS, false);
    }

    public static void setUseVisibleDividers(boolean enabled) {
        putBoolean(KEY_VISIBLE_DIVIDERS, enabled);
    }

    public static boolean invertOutgoing() {
        return getBoolean(KEY_INVERT_OUTGOING, false);
    }

    public static void setInvertOutgoing(boolean enabled) {
        putBoolean(KEY_INVERT_OUTGOING, enabled);
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences preferences = getPreferences();
        return preferences == null ? defaultValue : preferences.getBoolean(key, defaultValue);
    }

    private static void putBoolean(String key, boolean value) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(key, value).apply();
        onSettingsChanged();
    }

    private static void putInt(String key, int value) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(key, value).apply();
        onSettingsChanged();
    }

    // endregion

    // region invalidation

    public static void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** Drops the generated colours and re-applies the theme if a Monet one is active. */
    public static void onSettingsChanged() {
        MonetThemeGenerator.clearCache();
        refreshPreviewColors();
        for (Listener listener : listeners) {
            listener.onMonetSettingsChanged();
        }
        reapplyIfActive();
    }

    /** The wallpaper-derived system palette can change under us; drop the cache when it does. */
    public static void onConfigurationChanged() {
        // The seed swatches are read from the system palette even in custom mode, so the
        // palette cache is dropped regardless of which source the theme itself uses.
        MonetPalette.clearCache();
        if (getPaletteSource() != PALETTE_SOURCE_SYSTEM) {
            return;
        }
        MonetThemeGenerator.clearCache();
        refreshPreviewColors();
        reapplyIfActive();
    }

    private static void reapplyIfActive() {
        Theme.ThemeInfo active = Theme.getActiveTheme();
        if (active == null || !isMonetTheme(active.name)) {
            return;
        }
        final boolean nightTheme = Theme.isCurrentThemeNight();
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance()
                .postNotificationName(NotificationCenter.needSetDayNightTheme,
                        active, nightTheme, null, -1));
    }

    /**
     * Fills the swatches shown in the theme list. Built from the palettes alone rather than a
     * full template parse, and one palette per accent rather than one per theme, so app start
     * pays for three token tables instead of six.
     */
    private static void refreshPreviewColors() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return;
        }
        boolean useSystem = getPaletteSource() == PALETTE_SOURCE_SYSTEM;
        int seed = getSeedColor();
        int scheme = getScheme();
        for (int accent = MonetPalette.ACCENT_PRIMARY; accent <= MonetPalette.ACCENT_TERTIARY; accent++) {
            Map<String, Integer> tokens = MonetPalette.tokensFor(context, useSystem, seed, scheme, accent);
            if (tokens == null) {
                continue;
            }
            for (Variant variant : VARIANTS) {
                if (variant.accentPalette != accent) {
                    continue;
                }
                Theme.ThemeInfo themeInfo = Theme.getTheme(variant.themeName);
                if (themeInfo == null) {
                    continue;
                }
                if (variant.dark) {
                    themeInfo.setPreviewBackgroundColor(token(tokens, isAmoled() ? "n1_1000" : "n1_900"));
                    themeInfo.setPreviewInColor(token(tokens, "n2_800"));
                    themeInfo.setPreviewOutColor(token(tokens, "a1_100"));
                } else {
                    themeInfo.setPreviewBackgroundColor(token(tokens, "n1_50"));
                    themeInfo.setPreviewInColor(token(tokens, "a2_50"));
                    themeInfo.setPreviewOutColor(token(tokens, "a1_600"));
                }
            }
        }
    }

    private static int token(Map<String, Integer> tokens, String name) {
        Integer value = tokens.get(name);
        return value == null ? 0 : value;
    }

    // endregion
}
