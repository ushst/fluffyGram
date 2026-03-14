package org.ushastoe.fluffy.patches;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.text.TextPaint;
import android.widget.TextView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.AnimatedTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

public final class AppFontPatch {

    private static final String FONTS_DIR = "fluffy_fonts";
    private static final Object TYPEFACE_LOCK = new Object();
    private static final String[] SUPPORTED_EXTENSIONS = new String[] {".ttf", ".otf"};

    private static String cachedFontKey;
    private static Typeface cachedBaseTypeface;
    private static Typeface cachedItalicTypeface;
    private static Typeface cachedBoldTypeface;
    private static Typeface cachedBoldItalicTypeface;
    private static boolean originalDefaultsCaptured;
    private static Typeface originalDefaultTypeface;
    private static Typeface originalDefaultBoldTypeface;
    private static Typeface originalSansSerifTypeface;
    private static Typeface originalSerifTypeface;
    private static boolean defaultsPatched;

    private AppFontPatch() {
    }

    public static Typeface getTypefaceOverride(String assetPath) {
        ensureGlobalTypefaceOverride();
        if (!shouldOverrideAsset(assetPath)) {
            return null;
        }
        return getSelectedTypeface(resolveStyle(assetPath));
    }

    public static Typeface getBoldTypefaceOverride() {
        ensureGlobalTypefaceOverride();
        return getSelectedTypeface(Typeface.BOLD);
    }

    public static void applyToTextSettingsCell(TextView textView, AnimatedTextView valueTextView) {
        Typeface regularTypeface = getSelectedTypeface(Typeface.NORMAL);
        if (regularTypeface == null) {
            return;
        }
        textView.setTypeface(regularTypeface);
        valueTextView.setTypeface(regularTypeface);
    }

    public static void applyToTextCheckCell(TextView textView, TextView valueTextView) {
        Typeface regularTypeface = getSelectedTypeface(Typeface.NORMAL);
        if (regularTypeface == null) {
            return;
        }
        textView.setTypeface(regularTypeface);
        valueTextView.setTypeface(regularTypeface);
    }

    public static void applyToSimpleTextView(SimpleTextView textView) {
        Typeface regularTypeface = getSelectedTypeface(Typeface.NORMAL);
        if (regularTypeface == null) {
            return;
        }
        textView.setTypeface(regularTypeface);
    }

    public static void applyToTextView(TextView textView) {
        Typeface regularTypeface = getSelectedTypeface(Typeface.NORMAL);
        if (regularTypeface == null || textView == null) {
            return;
        }
        textView.setTypeface(regularTypeface);
    }

    public static void applyBoldToTextView(TextView textView) {
        Typeface boldTypeface = getSelectedTypeface(Typeface.BOLD);
        if (boldTypeface == null || textView == null) {
            return;
        }
        textView.setTypeface(boldTypeface);
    }

    public static void applyToAnimatedTextView(AnimatedTextView textView) {
        Typeface regularTypeface = getSelectedTypeface(Typeface.NORMAL);
        if (regularTypeface == null) {
            return;
        }
        textView.setTypeface(regularTypeface);
    }

    public static void applyToCommonMessagePaints(TextPaint... textPaints) {
        Typeface regularTypeface = getSelectedTypeface(Typeface.NORMAL);
        for (TextPaint textPaint : textPaints) {
            if (textPaint == null) {
                continue;
            }
            textPaint.setTypeface(regularTypeface);
        }
    }

    public static void applyToDialogMessagePaints(TextPaint[]... textPaintGroups) {
        Typeface regularTypeface = getSelectedTypeface(Typeface.NORMAL);
        for (TextPaint[] textPaintGroup : textPaintGroups) {
            if (textPaintGroup == null) {
                continue;
            }
            for (TextPaint textPaint : textPaintGroup) {
                if (textPaint == null) {
                    continue;
                }
                textPaint.setTypeface(regularTypeface);
            }
        }
    }

    public static void applyToRegularPaints(TextPaint... textPaints) {
        applyTypefaceToPaints(getSelectedTypeface(Typeface.NORMAL), textPaints);
    }

    public static void applyToBoldPaints(TextPaint... textPaints) {
        applyTypefaceToPaints(getSelectedTypeface(Typeface.BOLD), textPaints);
    }

    public static ArrayList<String> getAvailableFonts() {
        File dir = ApplicationLoader.getFilesDirFixed(FONTS_DIR);
        ArrayList<String> result = new ArrayList<>();
        if (dir == null) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        Arrays.sort(files, Comparator.comparing(file -> file.getName().toLowerCase(Locale.US)));
        for (File file : files) {
            if (file != null && file.isFile() && isSupportedFontFile(file.getName())) {
                result.add(file.getName());
            }
        }
        return result;
    }

    public static String importFont(Context context, Uri uri) throws IOException {
        if (context == null || uri == null) {
            throw new IOException("Missing font source");
        }

        String sourceName = queryDisplayName(context.getContentResolver(), uri);
        String sanitizedName = sanitizeFontFileName(sourceName);
        if (!isSupportedFontFile(sanitizedName)) {
            throw new IOException("Unsupported font format");
        }

        File dir = ApplicationLoader.getFilesDirFixed(FONTS_DIR);
        if (dir == null) {
            throw new IOException("Font storage is unavailable");
        }

        File destination = createUniqueFile(dir, sanitizedName);
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                throw new IOException("Unable to read font file");
            }
            copyToFile(inputStream, destination);
        } catch (IOException e) {
            destination.delete();
            throw e;
        }

        try {
            Typeface.createFromFile(destination);
        } catch (RuntimeException e) {
            destination.delete();
            throw new IOException("Invalid font file", e);
        }

        return destination.getName();
    }

    public static CharSequence getSelectedFontDisplayName() {
        ensureGlobalTypefaceOverride();
        String selectedFont = AppearanceSettingsPatch.getAppFontKey();
        if (TextUtils.isEmpty(selectedFont)) {
            return LocaleController.getString(R.string.FluffyAppFontDefault);
        }
        File file = getSelectedFontFile(selectedFont);
        if (file == null || !file.exists()) {
            return LocaleController.getString(R.string.FluffyAppFontDefault);
        }
        return getFontDisplayName(selectedFont);
    }

    public static CharSequence getFontDisplayName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return LocaleController.getString(R.string.FluffyAppFontDefault);
        }
        int extensionIndex = fileName.lastIndexOf('.');
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    public static void onFontChanged() {
        synchronized (TYPEFACE_LOCK) {
            cachedFontKey = null;
            cachedBaseTypeface = null;
            cachedItalicTypeface = null;
            cachedBoldTypeface = null;
            cachedBoldItalicTypeface = null;
        }
        ensureGlobalTypefaceOverride();
        if (ApplicationLoader.applicationContext != null) {
            Theme.reloadAllResources(ApplicationLoader.applicationContext);
        }
    }

    private static Typeface getSelectedTypeface(int style) {
        String selectedFont = AppearanceSettingsPatch.getAppFontKey();
        if (TextUtils.isEmpty(selectedFont)) {
            return null;
        }
        File fontFile = getSelectedFontFile(selectedFont);
        if (fontFile == null || !fontFile.exists()) {
            return null;
        }

        synchronized (TYPEFACE_LOCK) {
            if (!TextUtils.equals(cachedFontKey, fontFile.getAbsolutePath()) || cachedBaseTypeface == null) {
                cachedFontKey = fontFile.getAbsolutePath();
                cachedBaseTypeface = Typeface.createFromFile(fontFile);
                cachedItalicTypeface = null;
                cachedBoldTypeface = null;
                cachedBoldItalicTypeface = null;
            }
            switch (style) {
                case Typeface.BOLD:
                    if (cachedBoldTypeface == null) {
                        cachedBoldTypeface = Typeface.create(cachedBaseTypeface, Typeface.BOLD);
                    }
                    return cachedBoldTypeface;
                case Typeface.ITALIC:
                    if (cachedItalicTypeface == null) {
                        cachedItalicTypeface = Typeface.create(cachedBaseTypeface, Typeface.ITALIC);
                    }
                    return cachedItalicTypeface;
                case Typeface.BOLD_ITALIC:
                    if (cachedBoldItalicTypeface == null) {
                        cachedBoldItalicTypeface = Typeface.create(cachedBaseTypeface, Typeface.BOLD_ITALIC);
                    }
                    return cachedBoldItalicTypeface;
                default:
                    return cachedBaseTypeface;
            }
        }
    }

    private static void applyTypefaceToPaints(Typeface typeface, TextPaint... textPaints) {
        if (typeface == null || textPaints == null) {
            return;
        }
        for (TextPaint textPaint : textPaints) {
            if (textPaint == null) {
                continue;
            }
            textPaint.setTypeface(typeface);
        }
    }

    private static void ensureGlobalTypefaceOverride() {
        synchronized (TYPEFACE_LOCK) {
            captureOriginalDefaults();
            Typeface regular = getSelectedTypeface(Typeface.NORMAL);
            Typeface bold = getSelectedTypeface(Typeface.BOLD);
            Typeface sans = regular != null ? regular : originalSansSerifTypeface;
            Typeface serif = regular != null ? regular : originalSerifTypeface;

            if (regular == null || bold == null) {
                if (!defaultsPatched) {
                    return;
                }
                restoreTypefaceDefaults();
                defaultsPatched = false;
                return;
            }

            setTypefaceField("DEFAULT", regular);
            setTypefaceField("DEFAULT_BOLD", bold);
            setTypefaceField("SANS_SERIF", sans);
            setTypefaceField("SERIF", serif);
            setTypefaceDefaultsArray(regular, bold);
            defaultsPatched = true;
        }
    }

    private static void captureOriginalDefaults() {
        if (originalDefaultsCaptured) {
            return;
        }
        originalDefaultTypeface = Typeface.DEFAULT;
        originalDefaultBoldTypeface = Typeface.DEFAULT_BOLD;
        originalSansSerifTypeface = Typeface.SANS_SERIF;
        originalSerifTypeface = Typeface.SERIF;
        originalDefaultsCaptured = true;
    }

    private static void restoreTypefaceDefaults() {
        setTypefaceField("DEFAULT", originalDefaultTypeface);
        setTypefaceField("DEFAULT_BOLD", originalDefaultBoldTypeface);
        setTypefaceField("SANS_SERIF", originalSansSerifTypeface);
        setTypefaceField("SERIF", originalSerifTypeface);
        setTypefaceDefaultsArray(originalDefaultTypeface, originalDefaultBoldTypeface);
    }

    private static void setTypefaceField(String fieldName, Typeface typeface) {
        try {
            Field field = Typeface.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, typeface);
        } catch (Throwable ignore) {
        }
    }

    private static void setTypefaceDefaultsArray(Typeface regular, Typeface bold) {
        try {
            Field defaultsField = Typeface.class.getDeclaredField("sDefaults");
            defaultsField.setAccessible(true);
            Typeface[] defaults = (Typeface[]) defaultsField.get(null);
            if (defaults == null || defaults.length < 4) {
                return;
            }
            defaults[0] = regular;
            defaults[1] = bold;
            defaults[2] = Typeface.create(regular, Typeface.ITALIC);
            defaults[3] = Typeface.create(regular, Typeface.BOLD_ITALIC);
            defaultsField.set(null, defaults);
        } catch (Throwable ignore) {
        }
    }

    private static boolean shouldOverrideAsset(String assetPath) {
        if (TextUtils.isEmpty(assetPath)) {
            return false;
        }
        return !assetPath.contains("rmono")
                && !assetPath.contains("num.")
                && !assetPath.contains("mw_bold")
                && !assetPath.contains("courier_new_bold");
    }

    private static int resolveStyle(String assetPath) {
        String lowerCasePath = assetPath.toLowerCase(Locale.US);
        boolean italic = lowerCasePath.contains("italic");
        boolean bold = lowerCasePath.contains("medium")
                || lowerCasePath.contains("bold")
                || lowerCasePath.contains("rextrabold");
        if (bold && italic) {
            return Typeface.BOLD_ITALIC;
        }
        if (bold) {
            return Typeface.BOLD;
        }
        if (italic) {
            return Typeface.ITALIC;
        }
        return Typeface.NORMAL;
    }

    private static File getSelectedFontFile(String fileName) {
        File dir = ApplicationLoader.getFilesDirFixed(FONTS_DIR);
        if (dir == null || TextUtils.isEmpty(fileName)) {
            return null;
        }
        return new File(dir, fileName);
    }

    private static boolean isSupportedFontFile(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return false;
        }
        String lowerCaseName = fileName.toLowerCase(Locale.US);
        for (String extension : SUPPORTED_EXTENSIONS) {
            if (lowerCaseName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static String queryDisplayName(ContentResolver resolver, Uri uri) {
        if (resolver == null || uri == null) {
            return null;
        }
        Cursor cursor = null;
        try {
            cursor = resolver.query(uri, new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignore) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    private static String sanitizeFontFileName(String fileName) {
        String safeName = TextUtils.isEmpty(fileName) ? "font.ttf" : fileName.trim();
        safeName = safeName.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!isSupportedFontFile(safeName)) {
            safeName = safeName + ".ttf";
        }
        return safeName;
    }

    private static File createUniqueFile(File dir, String fileName) {
        File file = new File(dir, fileName);
        if (!file.exists()) {
            return file;
        }

        String baseName = fileName;
        String extension = "";
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            baseName = fileName.substring(0, extensionIndex);
            extension = fileName.substring(extensionIndex);
        }
        int counter = 2;
        while (file.exists()) {
            file = new File(dir, baseName + "_" + counter + extension);
            counter++;
        }
        return file;
    }

    private static void copyToFile(InputStream inputStream, File destination) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    outputStream.write(buffer, 0, read);
                }
            }
            outputStream.flush();
        }
    }
}
