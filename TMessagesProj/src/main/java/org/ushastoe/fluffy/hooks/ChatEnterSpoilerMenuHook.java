package org.ushastoe.fluffy.hooks;

import android.content.Context;
import android.view.View;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextCaption;
import org.ushastoe.fluffy.patches.ChatEnterSpoilerMenuPatch;

public final class ChatEnterSpoilerMenuHook {

    private ChatEnterSpoilerMenuHook() {
    }

    public static boolean onEmojiButtonLongClick(Object owner, View anchor, Context context, Theme.ResourcesProvider resourcesProvider, EditTextCaption editText) {
        return ChatEnterSpoilerMenuPatch.onEmojiButtonLongClick(owner, anchor, context, resourcesProvider, editText);
    }
}
