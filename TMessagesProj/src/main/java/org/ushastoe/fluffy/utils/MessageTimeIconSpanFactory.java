package org.ushastoe.fluffy.utils;

import android.graphics.Paint;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

public final class MessageTimeIconSpanFactory {

    private MessageTimeIconSpanFactory() {
    }

    public static ColoredImageSpan create(int iconRes) {
        ColoredImageSpan span = new ColoredImageSpan(iconRes, ColoredImageSpan.ALIGN_DEFAULT);
        configure(span);
        return span;
    }

    public static ColoredImageSpan createInvisible(int iconRes) {
        ColoredImageSpan span = new ColoredImageSpan(iconRes, ColoredImageSpan.ALIGN_DEFAULT);
        configure(span);
        span.draw = false;
        return span;
    }

    private static void configure(ColoredImageSpan span) {
        Paint.FontMetricsInt fontMetrics = Theme.chat_timePaint != null ? Theme.chat_timePaint.getFontMetricsInt() : null;
        if (fontMetrics != null) {
            span.setRelativeSize(fontMetrics);
            float textSize = Theme.chat_timePaint.getTextSize();
            if (textSize > 0) {
                span.setSize(Math.max(1, Math.round(textSize)));
            }
        }
        span.setTopOffset(0);
    }
}
