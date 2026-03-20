package org.ushastoe.fluffy.hooks;

import android.content.Context;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.ushastoe.fluffy.patches.PrivateReactionTimestampPatch;

public final class PrivateReactionTimestampHook {

    private PrivateReactionTimestampHook() {
    }

    public static boolean addPrivateReactionRow(ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout,
            Context context, MessageObject messageObject, Theme.ResourcesProvider resourcesProvider) {
        return PrivateReactionTimestampPatch.addPrivateReactionRow(popupLayout, context, messageObject, resourcesProvider);
    }
}
