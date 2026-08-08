package org.ushastoe.fluffy.monet;

import android.content.Context;
import android.graphics.Color;
import android.util.SparseIntArray;

import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.ThemeColors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Resolves a Monet token template ({@code assets/fluffy/monet_*.attheme}) into the
 * {@link SparseIntArray} of colour-key to ARGB that {@code Theme} consumes.
 *
 * <p>Nothing is written to disk: the templates are parsed straight into memory, so a
 * palette change is a regenerate-and-reapply rather than an export/import round trip.
 */
public final class MonetThemeGenerator {

    public static final String ASSET_LIGHT = "fluffy/monet_light.attheme";
    public static final String ASSET_DARK = "fluffy/monet_dark.attheme";

    /** Outgoing-bubble keys swapped with the opposite template by {@link Options#invertOutgoing}. */
    private static final String OUTGOING_PREFIX = "chat_out";
    private static final String[] EXTRA_OUTGOING_KEYS = {"chat_messageTextOut", "chat_messageLinkOut"};

    private static final String[] GRADIENT_AVATAR_KEYS = {
            "avatar_backgroundBlue", "avatar_backgroundCyan", "avatar_backgroundGreen",
            "avatar_backgroundOrange", "avatar_backgroundPink", "avatar_backgroundRed",
            "avatar_backgroundSaved", "avatar_backgroundViolet",
    };

    private static final String[] NAME_IN_MESSAGE_KEYS = {
            "avatar_nameInMessageBlue", "avatar_nameInMessageCyan", "avatar_nameInMessageGreen",
            "avatar_nameInMessageOrange", "avatar_nameInMessagePink", "avatar_nameInMessageRed",
            "avatar_nameInMessageViolet",
    };

    /** Selection tint has to stay translucent or it hides the message underneath. */
    private static final String KEY_SELECTED_BACKGROUND = "chat_selectedBackground";
    private static final int SELECTED_BACKGROUND_ALPHA = 0x80;

    public static final class Options {
        public boolean dark;
        public boolean amoled;
        public boolean gradientBubbles;
        public boolean gradientAvatars;
        public boolean monochromeNames;
        public boolean visibleDividers;
        public boolean invertOutgoing;
        public boolean useSystemPalette = true;
        public int seedColor = MonetPalette.DEFAULT_SEED;
        public int scheme = MonetPalette.SCHEME_TONAL_SPOT;
        public int accentPalette = MonetPalette.ACCENT_PRIMARY;

        /** Identity of a generated theme, used as the cache key. */
        public String signature() {
            return (dark ? "d" : "l")
                    + (amoled ? "a" : "-")
                    + (gradientBubbles ? "g" : "-")
                    + (gradientAvatars ? "v" : "-")
                    + (monochromeNames ? "m" : "-")
                    + (visibleDividers ? "s" : "-")
                    + (invertOutgoing ? "i" : "-")
                    + (useSystemPalette ? "y" : "n")
                    + Integer.toHexString(seedColor)
                    + "/" + scheme
                    + "/" + accentPalette;
        }
    }

    /**
     * Light and dark are both live whenever automatic night mode is on, so the cache holds
     * more than one entry — otherwise every day/night switch reparses a template.
     */
    private static final int CACHE_SIZE = 4;
    private static final Object cacheLock = new Object();
    private static final LinkedHashMap<String, SparseIntArray> cache =
            new LinkedHashMap<String, SparseIntArray>(CACHE_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SparseIntArray> eldest) {
                    return size() > CACHE_SIZE;
                }
            };

    private MonetThemeGenerator() {
    }

    public static void clearCache() {
        synchronized (cacheLock) {
            cache.clear();
        }
    }

    /**
     * @return the resolved colours, or {@code null} if the template could not be read.
     *         The caller gets its own copy and may mutate it.
     */
    public static SparseIntArray generate(Context context, Options options) {
        String signature = options.signature();
        synchronized (cacheLock) {
            SparseIntArray cached = cache.get(signature);
            if (cached != null) {
                return cached.clone();
            }
        }

        Map<String, Integer> tokens = buildTokens(context, options);
        Map<String, String> template = readTemplate(context, options.dark ? ASSET_DARK : ASSET_LIGHT);
        if (tokens == null || template == null) {
            return null;
        }
        applyModifiers(context, template, options);

        SparseIntArray colors = new SparseIntArray(template.size() + 1);
        for (Map.Entry<String, String> entry : template.entrySet()) {
            int colorKey = ThemeColors.stringKeyToInt(entry.getKey());
            if (colorKey < 0) {
                continue;
            }
            Integer value = resolveValue(entry.getValue(), tokens);
            if (value == null) {
                continue;
            }
            if (KEY_SELECTED_BACKGROUND.equals(entry.getKey())) {
                value = (value & 0x00ffffff) | (SELECTED_BACKGROUND_ALPHA << 24);
            }
            colors.put(colorKey, value);
        }
        // No wallpaper is embedded in a generated theme.
        int wallpaperOffsetKey = ThemeColors.stringKeyToInt("wallpaperFileOffset");
        if (wallpaperOffsetKey >= 0) {
            colors.put(wallpaperOffsetKey, -1);
        }

        synchronized (cacheLock) {
            cache.put(signature, colors);
        }
        return colors.clone();
    }

    /**
     * Writes the generated theme out as a real {@code .attheme} file with resolved colours,
     * for the export/share paths that expect a file on disk.
     */
    public static boolean writeTo(Context context, Options options, File file) {
        SparseIntArray colors = generate(context, options);
        if (colors == null) {
            return false;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < colors.size(); i++) {
            int colorKey = colors.keyAt(i);
            String name = ThemeColors.getStringName(colorKey);
            if (name == null || "wallpaperFileOffset".equals(name)) {
                continue;
            }
            builder.append(name).append('=').append(colors.valueAt(i)).append('\n');
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(builder.toString());
            return true;
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }

    public static Map<String, Integer> buildTokens(Context context, Options options) {
        // Falls back to the seed on its own when the platform has no system palette.
        return MonetPalette.tokensFor(context, options.useSystemPalette,
                options.seedColor, options.scheme, options.accentPalette);
    }

    private static Map<String, String> readTemplate(Context context, String assetName) {
        Map<String, String> template = new LinkedHashMap<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open(assetName), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }
                template.put(line.substring(0, separator), line.substring(separator + 1));
            }
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception ignore) {
                }
            }
        }
        return template.isEmpty() ? null : template;
    }

    private static void applyModifiers(Context context, Map<String, String> template, Options options) {
        if (options.invertOutgoing) {
            Map<String, String> opposite = readTemplate(context, options.dark ? ASSET_LIGHT : ASSET_DARK);
            if (opposite != null) {
                for (Map.Entry<String, String> entry : opposite.entrySet()) {
                    if (isOutgoingKey(entry.getKey()) && template.containsKey(entry.getKey())) {
                        template.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        // The templates spell the gradient stops "noGradient*" so that a flat bubble is the
        // default: renaming the keys is what turns the gradient on.
        for (String suffix : new String[]{"", "2", "3"}) {
            String value = template.remove("noGradient" + suffix);
            if (value != null && options.gradientBubbles) {
                template.put("chat_outBubbleGradient" + suffix, value);
            }
        }

        if (options.visibleDividers) {
            String divider = template.get("divider");
            if ("n1_50".equals(divider)) {
                template.put("divider", "n1_200");
            } else if ("n1_900".equals(divider)) {
                template.put("divider", "n1_700");
            }
        }

        if (options.gradientAvatars) {
            for (String key : GRADIENT_AVATAR_KEYS) {
                if ("n2_800".equals(template.get(key))) {
                    template.put(key, "n2_700");
                }
            }
        }

        if (options.monochromeNames) {
            for (String key : NAME_IN_MESSAGE_KEYS) {
                template.put(key, "a1_400");
            }
        }

        if (options.dark && options.amoled) {
            for (Map.Entry<String, String> entry : template.entrySet()) {
                if ("n1_900".equals(entry.getValue())) {
                    entry.setValue("n1_1000");
                }
            }
        }
    }

    private static boolean isOutgoingKey(String key) {
        if (key.startsWith(OUTGOING_PREFIX)) {
            return true;
        }
        for (String extra : EXTRA_OUTGOING_KEYS) {
            if (extra.equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** Values are either a palette token, a {@code #aarrggbb} literal or a plain int. */
    private static Integer resolveValue(String value, Map<String, Integer> tokens) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        Integer token = tokens.get(value);
        if (token != null) {
            return token;
        }
        try {
            if (value.charAt(0) == '#') {
                return Color.parseColor(value);
            }
            return (int) Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }
}
