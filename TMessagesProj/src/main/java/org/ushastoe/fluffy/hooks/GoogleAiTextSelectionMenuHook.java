package org.ushastoe.fluffy.hooks;

import android.view.Menu;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.GoogleAiTextSelectionMenuPatch;

public final class GoogleAiTextSelectionMenuHook {

    private GoogleAiTextSelectionMenuHook() {
    }

    public static void appendAction(Menu menu, ChatActivity fragment, MessageObject messageObject, CharSequence selectedText) {
        GoogleAiTextSelectionMenuPatch.appendAction(menu, fragment, messageObject, selectedText);
    }

    public static void prepareAction(Menu menu, ChatActivity fragment, MessageObject messageObject, CharSequence selectedText) {
        GoogleAiTextSelectionMenuPatch.prepareAction(menu, fragment, messageObject, selectedText);
    }

    public static boolean handleAction(ChatActivity fragment, MessageObject messageObject, CharSequence selectedText, int itemId) {
        return GoogleAiTextSelectionMenuPatch.handleAction(fragment, messageObject, selectedText, itemId);
    }
}
