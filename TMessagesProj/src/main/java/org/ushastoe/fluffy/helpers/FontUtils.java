package org.ushastoe.fluffy.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.BuildVars;
import org.ushastoe.fluffy.fluffyConfig;

public class FontUtils {
  private static final List<String> CJK_LANGUAGES = List.of("zh", "ja", "ko");
  private static final List<String> CYRILLIC_LANGUAGES =
      List.of("ru", "uk", "be", "bg", "kk", "ky", "mk", "mn", "sr", "tt", "tg", "uz");
  private static final String TEST_TEXT = resolveTestText();

  private static String resolveTestText() {
    String language = "";
    try {
      Locale locale = Locale.getDefault();
      if (locale != null) {
        language = locale.getLanguage();
      }
    } catch (Exception e) {
      log(e);
    }

    if (language != null) {
      language = language.toLowerCase(Locale.ROOT);
    } else {
      language = "";
    }

    if (CJK_LANGUAGES.contains(language)) {
      return "日";
    } else if (CYRILLIC_LANGUAGES.contains(language)) {
      return "Я";
    } else {
      return "R";
    }
  }
  private static final int CANVAS_SIZE = AndroidUtilities.dp(12);
  private static final Paint PAINT = new Paint() {
    {
      setTextSize(CANVAS_SIZE);
      setAntiAlias(false);
      setSubpixelText(false);
      setFakeBoldText(false);
    }
  };

  private static Boolean mediumWeightSupported = null;

  private static final Object typefaceOverrideLock = new Object();
  private static boolean originalSnapshotCaptured = false;
  private static Typeface originalDefault;
  private static Typeface originalDefaultBold;
  private static Typeface originalSansSerif;
  private static Typeface originalSerif;
  private static Typeface originalMonospace;
  private static Typeface originalDefaultField;
  private static Typeface originalSystemDefaultField;
  private static Typeface[] originalDefaultsArray;
  private static Map<String, Typeface> originalSystemFontMap;
  private static boolean customTypefaceApplied = false;

  public static boolean isMediumWeightSupported() {
    if (mediumWeightSupported == null) {
      mediumWeightSupported =
          testTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }
    return mediumWeightSupported;
  }

  private static boolean testTypeface(Typeface typeface) {
    return haveDifferentGlyphs(null, typeface);
  }

  private static boolean haveDifferentGlyphs(Typeface first, Typeface second) {
    Canvas canvas = new Canvas();

    Bitmap bitmap1 =
        Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ALPHA_8);
    canvas.setBitmap(bitmap1);
    PAINT.setTypeface(first);
    canvas.drawText(TEST_TEXT, 0, CANVAS_SIZE, PAINT);

    Bitmap bitmap2 =
        Bitmap.createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ALPHA_8);
    canvas.setBitmap(bitmap2);
    PAINT.setTypeface(second);
    canvas.drawText(TEST_TEXT, 0, CANVAS_SIZE, PAINT);

    boolean supported = !bitmap1.sameAs(bitmap2);
    AndroidUtilities.recycleBitmaps(List.of(bitmap1, bitmap2));
    return supported;
  }

  public static Typeface applyCustomStyle(Typeface baseTypeface, int style) {
    if (baseTypeface == null || style == Typeface.NORMAL) {
      return baseTypeface;
    }

    Typeface candidate = Typeface.create(baseTypeface, style);
    if (candidate == null) {
      return baseTypeface;
    }

    if (!haveDifferentGlyphs(baseTypeface, candidate)) {
      return baseTypeface;
    }

    return candidate;
  }

  public static void updateTypefaceOverride() {
    if (fluffyConfig.useSystemFonts || !fluffyConfig.hasCustomFont()) {
      applyGlobalTypeface(null);
      return;
    }

    File customFontFile = new File(fluffyConfig.customFontPath);
    if (!customFontFile.exists()) {
      fluffyConfig.clearCustomFont();
      applyGlobalTypeface(null);
      return;
    }

    try {
      Typeface baseTypeface = Typeface.createFromFile(customFontFile);
      applyGlobalTypeface(baseTypeface);
    } catch (Exception e) {
      log(e);
      applyGlobalTypeface(null);
    }
  }

  private static void applyGlobalTypeface(Typeface baseTypeface) {
    synchronized (typefaceOverrideLock) {
      captureOriginalSnapshot();

      if (baseTypeface == null) {
        if (customTypefaceApplied) {
          restoreOriginalTypefaces();
          customTypefaceApplied = false;
        }
        return;
      }

      Typeface boldTypeface = applyCustomStyle(baseTypeface, Typeface.BOLD);
      Typeface italicTypeface = applyCustomStyle(baseTypeface, Typeface.ITALIC);
      Typeface boldItalicTypeface = applyCustomStyle(baseTypeface, Typeface.BOLD_ITALIC);

      replaceStaticTypeface("DEFAULT", baseTypeface);
      replaceStaticTypeface("DEFAULT_BOLD", boldTypeface);
      replaceStaticTypeface("SANS_SERIF", baseTypeface);
      replaceStaticTypeface("SERIF", baseTypeface);
      replaceHiddenTypefaceField("sDefaultTypeface", baseTypeface);
      replaceHiddenTypefaceField("sSystemDefaultTypeface", baseTypeface);

      updateDefaultsArray(baseTypeface, boldTypeface, italicTypeface, boldItalicTypeface);
      updateSystemFontMap(baseTypeface, boldTypeface, italicTypeface, boldItalicTypeface);
      invalidateTypefaceCaches();

      customTypefaceApplied = true;
    }
  }

  private static void captureOriginalSnapshot() {
    if (originalSnapshotCaptured) {
      return;
    }

    try {
      originalDefault = getStaticTypeface("DEFAULT");
      originalDefaultBold = getStaticTypeface("DEFAULT_BOLD");
      originalSansSerif = getStaticTypeface("SANS_SERIF");
      originalSerif = getStaticTypeface("SERIF");
      originalMonospace = getStaticTypeface("MONOSPACE");
      originalDefaultField = getHiddenTypeface("sDefaultTypeface");
      originalSystemDefaultField = getHiddenTypeface("sSystemDefaultTypeface");
    } catch (Exception e) {
      log(e);
    }

    try {
      Field defaultsField = Typeface.class.getDeclaredField("sDefaults");
      defaultsField.setAccessible(true);
      Typeface[] defaults = (Typeface[]) defaultsField.get(null);
      if (defaults != null) {
        originalDefaultsArray = defaults.clone();
      }
    } catch (Throwable t) {
      log(t);
    }

    try {
      Field systemFontMapField = Typeface.class.getDeclaredField("sSystemFontMap");
      systemFontMapField.setAccessible(true);
      Map<String, Typeface> map = (Map<String, Typeface>) systemFontMapField.get(null);
      if (map != null) {
        originalSystemFontMap = new HashMap<>(map);
      }
    } catch (Throwable t) {
      log(t);
    }

    originalSnapshotCaptured = true;
  }

  private static void restoreOriginalTypefaces() {
    try {
      replaceStaticTypeface("DEFAULT", originalDefault);
      replaceStaticTypeface("DEFAULT_BOLD", originalDefaultBold);
      replaceStaticTypeface("SANS_SERIF", originalSansSerif);
      replaceStaticTypeface("SERIF", originalSerif);
      replaceHiddenTypefaceField("sDefaultTypeface", originalDefaultField);
      replaceHiddenTypefaceField("sSystemDefaultTypeface", originalSystemDefaultField);
    } catch (Exception e) {
      log(e);
    }

    if (originalDefaultsArray != null) {
      try {
        Field defaultsField = Typeface.class.getDeclaredField("sDefaults");
        defaultsField.setAccessible(true);
        defaultsField.set(null, originalDefaultsArray.clone());
      } catch (Throwable t) {
        log(t);
      }
    }

    if (originalSystemFontMap != null) {
      try {
        Field systemFontMapField = Typeface.class.getDeclaredField("sSystemFontMap");
        systemFontMapField.setAccessible(true);
        systemFontMapField.set(null, new HashMap<>(originalSystemFontMap));
      } catch (Throwable t) {
        log(t);
      }
    }

    invalidateTypefaceCaches();
  }

  private static Typeface getStaticTypeface(String fieldName)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = Typeface.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (Typeface) field.get(null);
  }

  private static Typeface getHiddenTypeface(String fieldName) {
    try {
      Field field = Typeface.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return (Typeface) field.get(null);
    } catch (NoSuchFieldException ignored) {
      return null;
    } catch (Throwable t) {
      log(t);
      return null;
    }
  }

  private static void replaceStaticTypeface(String fieldName, Typeface typeface) {
    try {
      Field field = Typeface.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(null, typeface);
    } catch (Throwable t) {
      log(t);
    }
  }

  private static void replaceHiddenTypefaceField(String fieldName, Typeface typeface) {
    try {
      Field field = Typeface.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(null, typeface);
    } catch (Throwable ignored) {
      // some Android versions do not expose this field
    }
  }

  private static void updateDefaultsArray(
      Typeface base, Typeface bold, Typeface italic, Typeface boldItalic) {
    try {
      Field defaultsField = Typeface.class.getDeclaredField("sDefaults");
      defaultsField.setAccessible(true);
      Typeface[] newDefaults = new Typeface[] {base, bold, italic, boldItalic};
      defaultsField.set(null, newDefaults);
    } catch (Throwable t) {
      log(t);
    }
  }

  private static void updateSystemFontMap(
      Typeface base, Typeface bold, Typeface italic, Typeface boldItalic) {
    try {
      Field systemFontMapField = Typeface.class.getDeclaredField("sSystemFontMap");
      systemFontMapField.setAccessible(true);
      Map<String, Typeface> map = (Map<String, Typeface>) systemFontMapField.get(null);
      if (map == null) {
        return;
      }
      Map<String, Typeface> newMap = new HashMap<>(map.size());
      for (Map.Entry<String, Typeface> entry : map.entrySet()) {
        String key = entry.getKey();
        if (key == null) {
          continue;
        }
        String lowerKey = key.toLowerCase(Locale.ROOT);
        Typeface replacement = base;
        boolean containsItalic = lowerKey.contains("italic") || lowerKey.contains("oblique");
        boolean containsBold =
            lowerKey.contains("medium")
                || lowerKey.contains("bold")
                || lowerKey.contains("black")
                || lowerKey.contains("heavy")
                || lowerKey.contains("demi");
        boolean isMono = lowerKey.contains("mono");
        if (isMono) {
          replacement = originalMonospace != null ? originalMonospace : entry.getValue();
        } else if (containsItalic && containsBold) {
          replacement = boldItalic;
        } else if (containsItalic) {
          replacement = italic;
        } else if (containsBold) {
          replacement = bold;
        }
        newMap.put(key, replacement);
      }
      systemFontMapField.set(null, newMap);
    } catch (Throwable t) {
      log(t);
    }
  }

  private static void invalidateTypefaceCaches() {
    try {
      Field cacheLockField = Typeface.class.getDeclaredField("sLock");
      cacheLockField.setAccessible(true);
      Object lock = cacheLockField.get(null);
      if (lock != null) {
        synchronized (lock) {
          clearTypefaceCachesInternal();
        }
        return;
      }
    } catch (Throwable ignored) {
      // ignored, fallback to clearing without lock
    }
    clearTypefaceCachesInternal();
  }

  private static void clearTypefaceCachesInternal() {
    clearTypefaceCacheField("sTypefaceCache");
    clearTypefaceCacheField("sTypefaceCacheCustom");
    clearTypefaceCacheField("sTypefaceCacheNative");
    clearTypefaceCacheField("sTypefaceCacheEmoji");
    clearTypefaceCacheField("sDynamicTypefaceCache");
  }

  private static void clearTypefaceCacheField(String fieldName) {
    try {
      Field field = Typeface.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      Object cache = field.get(null);
      clearCacheObject(cache);
    } catch (Throwable t) {
      // Some fields may be absent on certain Android versions.
      if (BuildVars.LOGS_ENABLED) {
        FileLog.e("Failed to clear cache field " + fieldName + ": " + t.getMessage());
      }
    }
  }

  private static void clearCacheObject(Object cache) {
    if (cache == null) {
      return;
    }

    if (cache instanceof Map<?, ?> map) {
      map.clear();
      return;
    }

    if (cache.getClass().isArray()) {
      int length = Array.getLength(cache);
      for (int i = 0; i < length; i++) {
        clearCacheObject(Array.get(cache, i));
      }
      return;
    }

    try {
      Method clearMethod = cache.getClass().getMethod("clear");
      clearMethod.setAccessible(true);
      clearMethod.invoke(cache);
    } catch (Throwable ignored) {
      // ignore silently if there's no clear method
    }
  }

  private static void log(Throwable throwable) {
    if (BuildVars.LOGS_ENABLED) {
      FileLog.e(throwable);
    }
  }
}
