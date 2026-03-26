package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.Components.ColoredImageSpan;
import org.ushastoe.fluffy.utils.MessageTimeIconSpanFactory;

public final class DeletedMessageIndicatorPatch {

    private static final String ICON_PLACEHOLDER = "d";

    private DeletedMessageIndicatorPatch() {
    }

    public static CharSequence buildDeletedTimeLabel(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return "";
        }
        String time = LocaleController.getInstance().getFormatterDay().format((long) messageObject.messageOwner.date * 1000);
        int mode = PremiumSettingsPatch.getDeletedMessageMarkerMode();
        if (mode == PremiumSettingsPatch.DELETED_MESSAGE_MARKER_MODE_TEXT) {
            return LocaleController.getString(R.string.FluffyDeletedMessageMarkerText) + " " + time;
        }
        if (mode == PremiumSettingsPatch.DELETED_MESSAGE_MARKER_MODE_SHORT_TEXT) {
            return LocaleController.getString(R.string.FluffyDeletedMessageMarkerShortText) + " " + time;
        }
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(ICON_PLACEHOLDER);
        builder.append(" ");
        builder.append(time);

        ColoredImageSpan span = MessageTimeIconSpanFactory.create(R.drawable.msg_delete);
        builder.setSpan(span, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }
}
