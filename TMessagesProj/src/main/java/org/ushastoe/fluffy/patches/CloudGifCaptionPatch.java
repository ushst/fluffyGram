package org.ushastoe.fluffy.patches;

import org.telegram.messenger.SendMessagesHelper;

public final class CloudGifCaptionPatch {
    private CloudGifCaptionPatch() {
    }

    public static String resolveInputCaption(boolean isStoryReply, CharSequence fieldText) {
        if (isStoryReply || fieldText == null) {
            return null;
        }
        String caption = SendMessagesHelper.getTrimmedString(fieldText.toString());
        if (caption.isEmpty()) {
            return null;
        }
        if (caption.startsWith("@gif")) {
            return "";
        }
        return caption;
    }
}
