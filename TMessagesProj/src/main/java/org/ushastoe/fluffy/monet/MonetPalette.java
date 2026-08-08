package org.ushastoe.fluffy.monet;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;

import org.ushastoe.fluffy.monet.mcu.DynamicScheme;
import org.ushastoe.fluffy.monet.mcu.Hct;
import org.ushastoe.fluffy.monet.mcu.SchemeContent;
import org.ushastoe.fluffy.monet.mcu.SchemeExpressive;
import org.ushastoe.fluffy.monet.mcu.SchemeFidelity;
import org.ushastoe.fluffy.monet.mcu.SchemeFruitSalad;
import org.ushastoe.fluffy.monet.mcu.SchemeMonochrome;
import org.ushastoe.fluffy.monet.mcu.SchemeNeutral;
import org.ushastoe.fluffy.monet.mcu.SchemeRainbow;
import org.ushastoe.fluffy.monet.mcu.SchemeTonalSpot;
import org.ushastoe.fluffy.monet.mcu.SchemeVibrant;
import org.ushastoe.fluffy.monet.mcu.TonalPalette;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the Material You token table the theme templates are written against:
 * {@code a1_*}/{@code a2_*}/{@code a3_*} accents, {@code n1_*}/{@code n2_*} neutrals
 * and a few fixed semantic colours.
 *
 * <p>Token suffixes follow Android's system palette resources, where 0 is the lightest
 * and 1000 the darkest tone — the inverse of the HCT tone the token maps to.
 */
public final class MonetPalette {

    public static final int SCHEME_TONAL_SPOT = 0;
    public static final int SCHEME_VIBRANT = 1;
    public static final int SCHEME_EXPRESSIVE = 2;
    public static final int SCHEME_NEUTRAL = 3;
    public static final int SCHEME_MONOCHROME = 4;
    public static final int SCHEME_RAINBOW = 5;
    public static final int SCHEME_FRUIT_SALAD = 6;
    public static final int SCHEME_CONTENT = 7;
    public static final int SCHEME_FIDELITY = 8;
    public static final int SCHEME_COUNT = 9;

    /** Token prefixes, paired index-wise with {@link #SYSTEM_PALETTE_NAMES}. */
    private static final String[] TOKEN_PREFIXES = {"a1", "a2", "a3", "n1", "n2"};
    private static final String[] SYSTEM_PALETTE_NAMES = {
            "accent1", "accent2", "accent3", "neutral1", "neutral2"
    };

    /** Token suffix -> HCT tone. */
    private static final int[] TOKEN_TONES = {0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000};
    private static final int[] HCT_TONES = {100, 99, 95, 90, 80, 70, 60, 50, 40, 30, 20, 10, 0};

    /** Material 3 error roles, kept fixed so warnings stay red whatever the seed is. */
    private static final int MONET_RED_LIGHT = 0xffb3261e;
    private static final int MONET_RED_DARK = 0xfff2b8b5;
    private static final int MONET_RED_CALL = 0xffef5350;
    private static final int MONET_GREEN_CALL = 0xff4caf50;

    /** Fallback seed when the system palette is unavailable and the user picked no colour. */
    public static final int DEFAULT_SEED = 0xff1a73e8;

    private MonetPalette() {
    }

    public static boolean isSystemPaletteAvailable() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    /**
     * Reads the wallpaper-derived palette Android exposes as {@code android.R.color.system_*}.
     *
     * @return {@code null} below API 31, or if the platform did not expose the resources.
     */
    public static Map<String, Integer> fromSystem(Context context) {
        if (!isSystemPaletteAvailable() || context == null) {
            return null;
        }
        Resources resources = context.getResources();
        Map<String, Integer> tokens = new HashMap<>();
        for (int p = 0; p < TOKEN_PREFIXES.length; p++) {
            for (int t = 0; t < TOKEN_TONES.length; t++) {
                String resourceName = "system_" + SYSTEM_PALETTE_NAMES[p] + "_" + TOKEN_TONES[t];
                int id = resources.getIdentifier(resourceName, "color", "android");
                if (id == 0) {
                    return null;
                }
                int color = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        ? resources.getColor(id, null)
                        : resources.getColor(id);
                tokens.put(TOKEN_PREFIXES[p] + "_" + TOKEN_TONES[t], color);
            }
        }
        addFixedTokens(tokens);
        return tokens;
    }

    /** Derives the same token table from a single seed colour through material-color-utilities. */
    public static Map<String, Integer> fromSeed(int seedArgb, int scheme) {
        DynamicScheme dynamicScheme = schemeFor(seedArgb, scheme);
        TonalPalette[] palettes = {
                dynamicScheme.primaryPalette,
                dynamicScheme.secondaryPalette,
                dynamicScheme.tertiaryPalette,
                dynamicScheme.neutralPalette,
                dynamicScheme.neutralVariantPalette,
        };
        Map<String, Integer> tokens = new HashMap<>();
        for (int p = 0; p < TOKEN_PREFIXES.length; p++) {
            for (int t = 0; t < TOKEN_TONES.length; t++) {
                tokens.put(TOKEN_PREFIXES[p] + "_" + TOKEN_TONES[t], palettes[p].tone(HCT_TONES[t]));
            }
        }
        addFixedTokens(tokens);
        return tokens;
    }

    private static DynamicScheme schemeFor(int seedArgb, int scheme) {
        Hct hct = Hct.fromInt(seedArgb);
        // Palettes are the same for light and dark; only DynamicColors, which we do not
        // vendor, differ. Pass false so the choice is at least deterministic.
        switch (scheme) {
            case SCHEME_VIBRANT:
                return new SchemeVibrant(hct, false, 0.0);
            case SCHEME_EXPRESSIVE:
                return new SchemeExpressive(hct, false, 0.0);
            case SCHEME_NEUTRAL:
                return new SchemeNeutral(hct, false, 0.0);
            case SCHEME_MONOCHROME:
                return new SchemeMonochrome(hct, false, 0.0);
            case SCHEME_RAINBOW:
                return new SchemeRainbow(hct, false, 0.0);
            case SCHEME_FRUIT_SALAD:
                return new SchemeFruitSalad(hct, false, 0.0);
            case SCHEME_CONTENT:
                return new SchemeContent(hct, false, 0.0);
            case SCHEME_FIDELITY:
                return new SchemeFidelity(hct, false, 0.0);
            case SCHEME_TONAL_SPOT:
            default:
                return new SchemeTonalSpot(hct, false, 0.0);
        }
    }

    private static void addFixedTokens(Map<String, Integer> tokens) {
        tokens.put("monetRedLight", MONET_RED_LIGHT);
        tokens.put("monetRedDark", MONET_RED_DARK);
        tokens.put("monetRedCall", MONET_RED_CALL);
        tokens.put("monetGreenCall", MONET_GREEN_CALL);
    }
}
