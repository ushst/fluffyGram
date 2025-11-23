package org.ushastoe.fluffy.activities.elements;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class headerSettingsCell extends FrameLayout {

  public final TextView titleTextView;
  private final ImageView logo;

  public headerSettingsCell(Context context) {
    super(context);

    Drawable iconDrawable =
      ContextCompat.getDrawable(context, R.mipmap.ic_launcher).mutate();
    int color = ContextCompat.getColor(context, R.color.ic_background_monet);

    logo = new ImageView(context);
    logo.setScaleType(ImageView.ScaleType.CENTER_CROP);
    logo.setBackground(
      Theme.createCircleDrawable(AndroidUtilities.dp(108), color));

    // Convert the drawable into a rounded/circular bitmap drawable
    int intrinsicW = iconDrawable.getIntrinsicWidth();
    int intrinsicH = iconDrawable.getIntrinsicHeight();
    int fallback = AndroidUtilities.dp(56);
    int width = intrinsicW > 0 ? intrinsicW : fallback;
    int height = intrinsicH > 0 ? intrinsicH : fallback;
    Bitmap iconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(iconBitmap);
    iconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    iconDrawable.draw(canvas);
    RoundedBitmapDrawable roundedIcon = RoundedBitmapDrawableFactory.create(getResources(), iconBitmap);
    roundedIcon.setCircular(true);
    logo.setImageDrawable(roundedIcon);
    addView(logo, LayoutHelper.createFrame(
                      108, 108, Gravity.CENTER | Gravity.TOP, 0, 20, 0, 0));

    titleTextView = new TextView(context);
    titleTextView.setTextColor(
        Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
    titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
    titleTextView.setText(!BuildConfig.DEBUG ? R.string.AppName
                                             : R.string.AppNameBeta);
    titleTextView.setLines(1);
    titleTextView.setMaxLines(1);
    titleTextView.setSingleLine(true);
    titleTextView.setPadding(0, 0, 0, 0);
    titleTextView.setGravity(Gravity.CENTER);
    addView(titleTextView,
            LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER | Gravity.TOP, 50, 145, 50, 0));

    TextView subtitleTextView = new TextView(context);
    subtitleTextView.setTextColor(
        Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
    subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
    subtitleTextView.setLineSpacing(AndroidUtilities.dp(2), 1f);
    subtitleTextView.setText(getString(R.string.info_fork));
    subtitleTextView.setGravity(Gravity.CENTER);
    subtitleTextView.setLines(0);
    subtitleTextView.setMaxLines(0);
    subtitleTextView.setSingleLine(false);
    subtitleTextView.setPadding(0, 0, 0, 0);
    addView(subtitleTextView,
            LayoutHelper.createFrame(
                LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT,
                Gravity.CENTER | Gravity.TOP, 60, 180, 60, 27));
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(
        MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec),
                                    MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
  }

  public void setOnLogoClickListener(OnClickListener onClickListener) {
    logo.setOnClickListener(onClickListener);
  }
}
