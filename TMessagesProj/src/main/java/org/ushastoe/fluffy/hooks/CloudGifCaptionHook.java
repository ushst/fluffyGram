package org.ushastoe.fluffy.hooks;

import android.widget.TextView;

import org.ushastoe.fluffy.patches.CloudGifCaptionPatch;

public final class CloudGifCaptionHook {
    private CloudGifCaptionHook() {
    }

    public static String resolveInputCaption(boolean isStoryReply, CharSequence fieldText) {
        return CloudGifCaptionPatch.resolveInputCaption(isStoryReply, fieldText);
    }

    public static void clearInputAfterSend(TextView messageEditText, String caption) {
        if (messageEditText != null && caption != null) {
            messageEditText.setText("");
        }
    }
}
