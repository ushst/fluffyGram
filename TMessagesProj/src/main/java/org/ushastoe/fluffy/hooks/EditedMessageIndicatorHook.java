package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.EditedMessageIndicatorPatch;

public final class EditedMessageIndicatorHook {

    private EditedMessageIndicatorHook() {
    }

    public static CharSequence buildEditedTimeLabel(MessageObject messageObject) {
        return EditedMessageIndicatorPatch.buildEditedTimeLabel(messageObject);
    }

    public static CharSequence buildEditedTimeLabel(MessageObject messageObject, int editDate) {
        return EditedMessageIndicatorPatch.buildEditedTimeLabel(messageObject, editDate);
    }

    public static String getEditedPrefix() {
        return EditedMessageIndicatorPatch.getEditedPrefix();
    }
}
