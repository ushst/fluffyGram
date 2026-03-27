package org.ushastoe.fluffy.patches;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ChatActivity;

public final class GoogleAiTextSelectionMenuPatch {

    public static final int MENU_ITEM_GOOGLE_AI = 10_041;

    private GoogleAiTextSelectionMenuPatch() {
    }

    public static void appendAction(Menu menu, ChatActivity fragment, MessageObject messageObject, CharSequence selectedText) {
        if (menu == null || fragment == null || !canShow(messageObject, selectedText)) {
            return;
        }
        if (menu.findItem(MENU_ITEM_GOOGLE_AI) != null) {
            return;
        }
        menu.add(Menu.NONE, MENU_ITEM_GOOGLE_AI, 4, LocaleController.getString(R.string.FluffyGoogleAi));
    }

    public static void prepareAction(Menu menu, ChatActivity fragment, MessageObject messageObject, CharSequence selectedText) {
        if (menu == null) {
            return;
        }
        MenuItem item = menu.findItem(MENU_ITEM_GOOGLE_AI);
        if (item != null) {
            item.setVisible(canShow(messageObject, selectedText));
        }
    }

    public static boolean handleAction(ChatActivity fragment, MessageObject messageObject, CharSequence selectedText, int itemId) {
        if (itemId != MENU_ITEM_GOOGLE_AI || fragment == null) {
            return false;
        }
        if (!canShow(messageObject, selectedText)) {
            return true;
        }
        GoogleAiMessageMenuPatch.showPromptChooserForText(fragment, selectedText, LocaleController.getString(R.string.FluffyGoogleAi));
        return true;
    }

    private static boolean canShow(MessageObject messageObject, CharSequence selectedText) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        if (messageObject.type == MessageObject.TYPE_STORY || messageObject.isVoiceTranscriptionOpen() || messageObject.isInvoice()) {
            return false;
        }
        return GoogleAiMessageMenuPatch.canUseForText(normalize(selectedText));
    }

    private static String normalize(CharSequence selectedText) {
        if (selectedText == null) {
            return null;
        }
        String text = selectedText.toString().trim();
        return TextUtils.isEmpty(text) ? null : text;
    }
}
