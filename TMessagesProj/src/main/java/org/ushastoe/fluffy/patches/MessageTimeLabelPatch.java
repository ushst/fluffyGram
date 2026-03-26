package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.ColoredImageSpan;
import org.ushastoe.fluffy.utils.MessageTimeIconSpanFactory;

public final class MessageTimeLabelPatch {
    private static final String SILENT_ICON_PLACEHOLDER = "m";
    private static final String EDITED_ICON_PLACEHOLDER = "e";

    private MessageTimeLabelPatch() {
    }

    public static CharSequence buildSilentTimeLabel(MessageObject messageObject, boolean edited) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return "";
        }
        if (!messageObject.messageOwner.silent) {
            return "";
        }
        if (!edited) {
            return SilentMessageIndicatorPatch.buildSilentTimeLabel(messageObject);
        }

        String time = LocaleController.getInstance().getFormatterDay().format((long) messageObject.messageOwner.date * 1000);
        SpannableStringBuilder builder = new SpannableStringBuilder();

        appendSilentMarker(builder);
        appendEditedMarker(builder);

        if (builder.length() > 0) {
            builder.append(" ");
        }
        builder.append(time);
        return builder;
    }

    private static void appendSilentMarker(SpannableStringBuilder builder) {
        int mode = AppearanceSettingsPatch.getSilentMarkerMode();
        if (builder.length() > 0) {
            builder.append(" ");
        }
        if (mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_NOTIFY_OFF
                || mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_MUTE) {
            int start = builder.length();
            builder.append(SILENT_ICON_PLACEHOLDER);
            int iconRes = mode == AppearanceSettingsPatch.SILENT_MARKER_MODE_ICON_MUTE
                    ? R.drawable.msg_mute
                    : R.drawable.input_notify_off;
            ColoredImageSpan span = MessageTimeIconSpanFactory.create(iconRes);
            builder.setSpan(span, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return;
        }
        builder.append(SilentMessageIndicatorPatch.getSilentPrefix());
    }

    private static void appendEditedMarker(SpannableStringBuilder builder) {
        int mode = AppearanceSettingsPatch.getEditedMarkerMode();
        if (builder.length() > 0) {
            builder.append(" ");
        }
        if (mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_STAMP
                || mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_EDIT) {
            int start = builder.length();
            builder.append(EDITED_ICON_PLACEHOLDER);
            int iconRes = mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_EDIT
                    ? R.drawable.msg_edit
                    : R.drawable.menu_edited_stamp;
            ColoredImageSpan span = MessageTimeIconSpanFactory.create(iconRes);
            builder.setSpan(span, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return;
        }
        builder.append(EditedMessageIndicatorPatch.getEditedPrefix());
    }
}
