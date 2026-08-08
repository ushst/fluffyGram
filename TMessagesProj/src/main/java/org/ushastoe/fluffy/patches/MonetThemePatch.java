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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Material You themes generated inside the app.
 *
 * <p>Two extra {@link Theme.ThemeInfo} entries are registered next to Blue/Day/Night, both
 * pointing at a token template in {@code assets/fluffy/}. {@code Theme.getThemeFileValues}
 * is intercepted for those asset names, so selecting, previewing and night-switching a
 * Monet theme all go through Telegram's own code paths — nothing is exported or imported.
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

    /** Built-in themes occupy 1..5; Monet sits right after them. */
    private static final int SORT_INDEX_LIGHT = 6;
    private static final int SORT_INDEX_DARK = 7;

    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    public interface Listener {
        void onMonetSettingsChanged();
    }

    private MonetThemePatch() {
    }

    // region theme registration

    /**
     * Called from {@code Theme}'s static initialiser, right after the built-in themes.
     * The two {@code ThemeInfo} instances are supplied by the caller because their
     * constructor is package-private to {@code org.telegram.ui.ActionBar}.
     */
    public static void registerThemes(ArrayList<Theme.ThemeInfo> themes,
                                      HashMap<String, Theme.ThemeInfo> themesDict,
                                      Theme.ThemeInfo light, Theme.ThemeInfo dark) {
        if (themes == null || themesDict == null || light == null || dark == null
                || themesDict.containsKey(THEME_NAME_LIGHT)) {
            return;
        }
        light.name = THEME_NAME_LIGHT;
        light.assetName = MonetThemeGenerator.ASSET_LIGHT;
        light.sortIndex = SORT_INDEX_LIGHT;
        themes.add(light);
        themesDict.put(THEME_NAME_LIGHT, light);

        dark.name = THEME_NAME_DARK;
        dark.assetName = MonetThemeGenerator.ASSET_DARK;
        dark.sortIndex = SORT_INDEX_DARK;
        themes.add(dark);
        themesDict.put(THEME_NAME_DARK, dark);

        applyPreviewColors(light, dark);
    }

    public static boolean isMonetTheme(String themeName) {
        return THEME_NAME_LIGHT.equals(themeName) || THEME_NAME_DARK.equals(themeName);
    }

    /**
     * Tells {@code ThemeInfo.isDark()} which side a Monet theme is on. Without this it would
     * try to parse {@code pathToFile}, which our themes do not have.
     *
     * @return {@code null} for any theme that is not ours.
     */
    public static Boolean resolveThemeIsDark(String themeName) {
        if (THEME_NAME_DARK.equals(themeName)) {
            return Boolean.TRUE;
        }
        if (THEME_NAME_LIGHT.equals(themeName)) {
            return Boolean.FALSE;
        }
        return null;
    }

    /** Resolves the token template behind {@code assetName} into live colours. */
    public static SparseIntArray getColorsForAsset(String assetName) {
        Boolean dark = darkForAsset(assetName);
        if (dark == null) {
            return null;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return MonetThemeGenerator.generate(context, buildOptions(dark));
    }

    /**
     * Materialises a Monet theme as a real {@code .attheme} file, so that sharing or
     * exporting it hands over resolved colours instead of the raw token template.
     *
     * @return {@code null} for any asset that is not ours.
     */
    public static File getGeneratedThemeFile(String assetName) {
        Boolean dark = darkForAsset(assetName);
        if (dark == null) {
            return null;
        }
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        File file = new File(ApplicationLoader.getFilesDirFixed(),
                dark ? "fluffy_monet_dark.attheme" : "fluffy_monet_light.attheme");
        if (!MonetThemeGenerator.writeTo(context, buildOptions(dark), file)) {
            return null;
        }
        return file;
    }

    private static Boolean darkForAsset(String assetName) {
        if (MonetThemeGenerator.ASSET_DARK.equals(assetName)) {
            return Boolean.TRUE;
        }
        if (MonetThemeGenerator.ASSET_LIGHT.equals(assetName)) {
            return Boolean.FALSE;
        }
        return null;
    }

    // endregion

    // region settings

    public static MonetThemeGenerator.Options buildOptions(boolean dark) {
        MonetThemeGenerator.Options options = new MonetThemeGenerator.Options();
        options.dark = dark;
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

    private static void refreshPreviewColors() {
        applyPreviewColors(Theme.getTheme(THEME_NAME_LIGHT), Theme.getTheme(THEME_NAME_DARK));
    }

    /**
     * Fills the swatches shown in the theme list. Deliberately built from the palette alone
     * rather than a full template parse, so app start does not pay for it.
     */
    private static void applyPreviewColors(Theme.ThemeInfo light, Theme.ThemeInfo dark) {
        Context context = ApplicationLoader.applicationContext;
        if (context == null || (light == null && dark == null)) {
            return;
        }
        Map<String, Integer> tokens = MonetThemeGenerator.buildTokens(context, buildOptions(false));
        if (tokens == null) {
            return;
        }
        if (light != null) {
            light.setPreviewBackgroundColor(token(tokens, "n1_50"));
            light.setPreviewInColor(token(tokens, "a2_50"));
            light.setPreviewOutColor(token(tokens, "a1_600"));
        }
        if (dark != null) {
            dark.setPreviewBackgroundColor(token(tokens, isAmoled() ? "n1_1000" : "n1_900"));
            dark.setPreviewInColor(token(tokens, "n2_800"));
            dark.setPreviewOutColor(token(tokens, "a1_100"));
        }
    }

    private static int token(Map<String, Integer> tokens, String name) {
        Integer value = tokens.get(name);
        return value == null ? 0 : value;
    }

    // endregion
}
