package org.ushastoe.fluffy.ui.elements;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class HeaderSettingsCell extends FrameLayout {

    public final TextView titleTextView;
    private final ImageView logoImageView;

    public HeaderSettingsCell(Context context) {
        super(context);

        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        Drawable iconDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);
        int logoBackgroundColor = ContextCompat.getColor(context, R.color.ic_background_monet);

        logoImageView = new ImageView(context);
        logoImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logoImageView.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(108), logoBackgroundColor));
        logoImageView.setImageDrawable(createRoundedIcon(iconDrawable));
        addView(logoImageView, LayoutHelper.createFrame(108, 108, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 20, 0, 0));

        titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleTextView.setText(LocaleController.getString(ApplicationLoader.isBetaBuild() ? R.string.AppNameBeta : R.string.AppName));
        titleTextView.setLines(1);
        titleTextView.setMaxLines(1);
        titleTextView.setSingleLine(true);
        titleTextView.setGravity(Gravity.CENTER);
        addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 50, 145, 50, 0));

        TextView subtitleTextView = new TextView(context);
        subtitleTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitleTextView.setLineSpacing(AndroidUtilities.dp(2), 1f);
        subtitleTextView.setText(LocaleController.getString(R.string.info_fork));
        subtitleTextView.setGravity(Gravity.CENTER);
        subtitleTextView.setLines(0);
        subtitleTextView.setMaxLines(0);
        subtitleTextView.setSingleLine(false);
        addView(subtitleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 60, 180, 60, 27));
    }

    public void setOnLogoClickListener(OnClickListener onClickListener) {
        logoImageView.setOnClickListener(onClickListener);
    }

    private Drawable createRoundedIcon(Drawable iconDrawable) {
        if (iconDrawable == null) {
            return null;
        }
        int fallbackSize = AndroidUtilities.dp(56);
        int width = iconDrawable.getIntrinsicWidth() > 0 ? iconDrawable.getIntrinsicWidth() : fallbackSize;
        int height = iconDrawable.getIntrinsicHeight() > 0 ? iconDrawable.getIntrinsicHeight() : fallbackSize;
        Bitmap iconBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(iconBitmap);
        iconDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        iconDrawable.draw(canvas);
        RoundedBitmapDrawable roundedIcon = RoundedBitmapDrawableFactory.create(getResources(), iconBitmap);
        roundedIcon.setCircular(true);
        return roundedIcon;
    }
}
