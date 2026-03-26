package org.ushastoe.fluffy.utils;

import android.graphics.Paint;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

public final class MessageTimeIconSpanFactory {

    private MessageTimeIconSpanFactory() {
    }

    public static ColoredImageSpan create(int iconRes) {
        ColoredImageSpan span = new ColoredImageSpan(iconRes, ColoredImageSpan.ALIGN_DEFAULT);
        Paint.FontMetricsInt fontMetrics = Theme.chat_timePaint != null ? Theme.chat_timePaint.getFontMetricsInt() : null;
        if (fontMetrics != null) {
            int lineHeight = Math.abs(fontMetrics.descent) + Math.abs(fontMetrics.ascent);
            float textSize = Theme.chat_timePaint.getTextSize();
            span.setRelativeSize(fontMetrics);
            if (lineHeight > 0 && textSize > 0) {
                float scale = textSize / lineHeight;
                span.setScale(scale, scale);
            }
        }
        span.setTopOffset(0);
        return span;
    }
}
