package org.ushastoe.fluffy.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import java.util.List;
import java.util.Locale;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;

public class FontUtils {
  private static final String TEST_TEXT;

  private static final List<String> CJK_LANGUAGES = List.of("zh", "ja", "ko");
  private static final List<String> CYRILLIC_LANGUAGES =
      List.of("ru", "uk", "be", "bg", "kk", "ky", "mk", "mn", "sr", "tt", "tg", "uz");

  static {
    String language =
        LocaleController.getInstance()
            .getCurrentLocale()
            .getLanguage();
    if (language != null) {
      language = language.toLowerCase(Locale.ROOT);
    } else {
      language = "";
    }
    if (CJK_LANGUAGES.contains(language)) {
      TEST_TEXT = "日";
    } else if (CYRILLIC_LANGUAGES.contains(language)) {
      TEST_TEXT = "Я";
    } else {
      TEST_TEXT = "R";
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
}
