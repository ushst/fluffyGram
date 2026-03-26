package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.MessageObject;
import org.ushastoe.fluffy.patches.DeletedMessageIndicatorPatch;
import org.ushastoe.fluffy.patches.LocalMessageArchivePatch;

public final class DeletedMessageIndicatorHook {

    private DeletedMessageIndicatorHook() {
    }

    public static boolean isLocallyDeleted(MessageObject messageObject) {
        return LocalMessageArchivePatch.isLocallyDeleted(messageObject);
    }

    public static CharSequence buildDeletedTimeLabel(MessageObject messageObject) {
        return DeletedMessageIndicatorPatch.buildDeletedTimeLabel(messageObject);
    }
}
