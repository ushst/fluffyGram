package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ColoredImageSpan;

public final class DeletedMessageIndicatorPatch {

    private static final String ICON_PLACEHOLDER = "d";

    private DeletedMessageIndicatorPatch() {
    }

    public static CharSequence buildDeletedTimeLabel(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return "";
        }
        String time = LocaleController.getInstance().getFormatterDay().format((long) messageObject.messageOwner.date * 1000);
        if (PremiumSettingsPatch.getDeletedMessageMarkerMode() == PremiumSettingsPatch.DELETED_MESSAGE_MARKER_MODE_TEXT) {
            return LocaleController.getString(R.string.FluffyDeletedMessageMarkerText) + " " + time;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(ICON_PLACEHOLDER);
        builder.append(" ");
        builder.append(time);

        ColoredImageSpan span = new ColoredImageSpan(R.drawable.msg_delete, ColoredImageSpan.ALIGN_DEFAULT);
        span.setRelativeSize(Theme.chat_timePaint.getFontMetricsInt());
        span.setTopOffset(1);
        builder.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
