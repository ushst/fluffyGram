package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;

import java.util.Date;

public final class ForwardedOriginalDatePatch {

    private ForwardedOriginalDatePatch() {
    }

    public static boolean shouldShowThirdLine(MessageObject messageObject) {
        return buildOriginalDateLine(messageObject) != null;
    }

    public static CharSequence appendOriginalDateThirdLine(MessageObject messageObject, CharSequence forwardNameLine, TextPaint paint, int width) {
        String originalDateLine = buildOriginalDateLine(messageObject);
        if (TextUtils.isEmpty(originalDateLine)) {
            return forwardNameLine;
        }

        CharSequence topLine = TextUtils.ellipsize(originalDateLine, paint, width, TextUtils.TruncateAt.END);
        CharSequence bottomLine = TextUtils.ellipsize(forwardNameLine, paint, width, TextUtils.TruncateAt.END);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(topLine);
        builder.append('\n');
        builder.append(bottomLine);
        return builder;
    }

    private static String buildOriginalDateLine(MessageObject messageObject) {
        if (!AppearanceSettingsPatch.isForwardedOriginalDateShown()
                || messageObject == null
                || messageObject.messageOwner == null
                || messageObject.messageOwner.fwd_from == null) {
            return null;
        }

        int forwardDate = messageObject.messageOwner.fwd_from.date;
        if (forwardDate == 0) {
            forwardDate = messageObject.messageOwner.fwd_from.saved_date;
        }
        if (forwardDate <= 0) {
            return null;
        }

        Date date = new Date((long) forwardDate * 1000L);
        String dateTime = LocaleController.formatString(
                R.string.formatDateAtTime,
                LocaleController.getInstance().getFormatterYear().format(date),
                LocaleController.getInstance().getFormatterDayWithSeconds().format(date));
        return dateTime;
    }
}
