package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

public final class ScheduledMessageIndicatorPatch {

    private static final String ICON_PLACEHOLDER = "s";

    private ScheduledMessageIndicatorPatch() {
    }

    public static CharSequence buildScheduledTimeLabel(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return "";
        }
        String time = LocaleController.getInstance().getFormatterDay().format((long) messageObject.messageOwner.date * 1000);
        int mode = AppearanceSettingsPatch.getScheduledMarkerMode();
        if (mode == AppearanceSettingsPatch.SCHEDULED_MARKER_MODE_ICON_CALENDAR
                || mode == AppearanceSettingsPatch.SCHEDULED_MARKER_MODE_ICON_SCHEDULE) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(ICON_PLACEHOLDER);
            builder.append(" ");
            builder.append(time);

            int iconRes = mode == AppearanceSettingsPatch.SCHEDULED_MARKER_MODE_ICON_SCHEDULE
                    ? R.drawable.input_schedule
                    : R.drawable.msg_calendar2;
            ColoredImageSpan span = new ColoredImageSpan(iconRes, ColoredImageSpan.ALIGN_DEFAULT);
            span.setRelativeSize(Theme.chat_timePaint.getFontMetricsInt());
            span.setTopOffset(1);
            builder.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return getScheduledPrefix() + " " + time;
    }

    public static String getScheduledPrefix() {
        if (AppearanceSettingsPatch.getScheduledMarkerMode() == AppearanceSettingsPatch.SCHEDULED_MARKER_MODE_SHORT_TEXT) {
            return LocaleController.getString(R.string.FluffyScheduledMarkerModeShort);
        }
        if (AppearanceSettingsPatch.getScheduledMarkerMode() == AppearanceSettingsPatch.SCHEDULED_MARKER_MODE_ICON_CALENDAR
                || AppearanceSettingsPatch.getScheduledMarkerMode() == AppearanceSettingsPatch.SCHEDULED_MARKER_MODE_ICON_SCHEDULE) {
            return ICON_PLACEHOLDER;
        }
        return LocaleController.getString(R.string.FluffyScheduledMarkerModeFull);
    }
}
