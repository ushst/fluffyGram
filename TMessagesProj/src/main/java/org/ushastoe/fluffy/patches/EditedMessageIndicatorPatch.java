package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.Components.ColoredImageSpan;
import org.ushastoe.fluffy.utils.MessageTimeIconSpanFactory;

public final class EditedMessageIndicatorPatch {

    private static final String ICON_PLACEHOLDER = "e";

    private EditedMessageIndicatorPatch() {
    }

    public static CharSequence buildEditedTimeLabel(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return "";
        }
        String time = LocaleController.getInstance().getFormatterDay().format((long) messageObject.messageOwner.date * 1000);
        int mode = AppearanceSettingsPatch.getEditedMarkerMode();
        if (mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_STAMP
                || mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_EDIT) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(ICON_PLACEHOLDER);
            builder.append(" ");
            builder.append(time);

            int iconRes = mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_EDIT
                    ? R.drawable.msg_edit
                    : R.drawable.menu_edited_stamp;
            ColoredImageSpan span = MessageTimeIconSpanFactory.create(iconRes);
            builder.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return getEditedPrefix() + " " + time;
    }

    public static String getEditedPrefix() {
        int mode = AppearanceSettingsPatch.getEditedMarkerMode();
        if (mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_SHORT_TEXT) {
            return LocaleController.getString(R.string.FluffyEditedMarkerModeShort);
        }
        if (mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_STAMP
                || mode == AppearanceSettingsPatch.EDITED_MARKER_MODE_ICON_EDIT) {
            return ICON_PLACEHOLDER;
        }
        return LocaleController.getString(R.string.EditedMessage);
    }
}
