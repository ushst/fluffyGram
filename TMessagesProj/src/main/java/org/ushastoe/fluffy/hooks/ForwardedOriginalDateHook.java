package org.ushastoe.fluffy.hooks;

import android.text.TextPaint;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.ForwardedOriginalDatePatch;

public final class ForwardedOriginalDateHook {

    private ForwardedOriginalDateHook() {
    }

    public static CharSequence appendOriginalDateThirdLine(MessageObject messageObject, CharSequence forwardNameLine, TextPaint paint, int width) {
        return ForwardedOriginalDatePatch.appendOriginalDateThirdLine(messageObject, forwardNameLine, paint, width);
    }

    public static boolean shouldShowThirdLine(MessageObject messageObject) {
        return ForwardedOriginalDatePatch.shouldShowThirdLine(messageObject);
    }
}
