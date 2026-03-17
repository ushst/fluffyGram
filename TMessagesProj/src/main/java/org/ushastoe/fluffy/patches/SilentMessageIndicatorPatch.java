package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

public final class SilentMessageIndicatorPatch {

    private static final String ICON_PLACEHOLDER = "m";

    private SilentMessageIndicatorPatch() {
    }

    public static CharSequence buildSilentTimeLabel(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return "";
        }
        String time = LocaleController.getInstance().getFormatterDay().format((long) messageObject.messageOwner.date * 1000);
        int mode = AppearanceSettingsPatch.getSilentMarkerMode();
        if (mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_NOTIFY_OFF
                || mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_MUTE) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(ICON_PLACEHOLDER);
            builder.append(" ");
            builder.append(time);

            int iconRes = mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_MUTE
                    ? R.drawable.msg_mute
                    : R.drawable.input_notify_off;
            ColoredImageSpan span = new ColoredImageSpan(iconRes, ColoredImageSpan.ALIGN_DEFAULT);
            span.setRelativeSize(Theme.chat_timePaint.getFontMetricsInt());
            span.setTopOffset(1);
            builder.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return getSilentPrefix() + " " + time;
    }

    public static String getSilentPrefix() {
        int mode = AppearanceSettingsPatch.getSilentMarkerMode();
        if (mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_SHORT_TEXT) {
            return LocaleController.getString(R.string.FluffySilentMarkerModeShort);
        }
        if (mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_NOTIFY_OFF
                || mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_MUTE) {
            return ICON_PLACEHOLDER;
        }
        return LocaleController.getString(R.string.FluffySilentMarkerModeFull);
    }
}
