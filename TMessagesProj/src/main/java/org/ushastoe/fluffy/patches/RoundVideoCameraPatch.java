package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

public final class RoundVideoCameraPatch {

    private static final Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final Paint badgeBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    static {
        badgePaint.setColor(Color.WHITE);
        badgePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        badgePaint.setTextAlign(Paint.Align.CENTER);
        badgePaint.setTextSize(AndroidUtilities.dp(10));

        badgeBackgroundPaint.setColor(0x66000000);
    }

    private RoundVideoCameraPatch() {
    }

    public static boolean resolveInitialFrontCamera(boolean fromPaused, boolean currentFrontface) {
        if (!AppearanceSettingsPatch.isRoundVideoCameraFeatureEnabled()) {
            return fromPaused ? currentFrontface : true;
        }
        if (fromPaused) {
            return currentFrontface;
        }
        boolean frontface = AppearanceSettingsPatch.useFrontRoundVideoCameraByDefault();
        setCurrentFrontCamera(frontface);
        return frontface;
    }

    public static void setCurrentFrontCamera(boolean frontface) {
        AppearanceSettingsPatch.setRoundVideoCameraMode(frontface
                ? AppearanceSettingsPatch.ROUND_VIDEO_CAMERA_FRONT
                : AppearanceSettingsPatch.ROUND_VIDEO_CAMERA_BACK);
    }

    public static void drawVideoCameraBadge(Canvas canvas, boolean videoMode, int width, int height) {
        if (!AppearanceSettingsPatch.isRoundVideoCameraFeatureEnabled() || !videoMode) {
            return;
        }
        Paint.FontMetrics metrics = badgePaint.getFontMetrics();
        float badgeCenterX = width / 2f + AndroidUtilities.dp(5);
        float badgeCenterY = height / 2f + AndroidUtilities.dp(6);
        float textBaselineY = badgeCenterY - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawCircle(badgeCenterX, badgeCenterY, AndroidUtilities.dp(7), badgeBackgroundPaint);
        canvas.drawText(AppearanceSettingsPatch.useFrontRoundVideoCamera() ? "f" : "b", badgeCenterX, textBaselineY, badgePaint);
    }

    public static CharSequence getRecordButtonContentDescription(Context context, boolean videoMode) {
        return context.getString(videoMode ? R.string.AccDescrVideoMessage : R.string.AccDescrVoiceMessage);
    }
}
