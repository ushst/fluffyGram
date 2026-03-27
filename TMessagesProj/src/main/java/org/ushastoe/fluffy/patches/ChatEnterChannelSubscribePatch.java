package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.URLSpanReplacement;

public final class ChatEnterChannelSubscribePatch {

    private ChatEnterChannelSubscribePatch() {
    }

    public static void maybeAddMenuItem(
            Context context,
            Theme.ResourcesProvider resourcesProvider,
            ActionBarPopupWindow.ActionBarPopupWindowLayout layout,
            int itemHeightDp,
            int itemMinWidthDp,
            Runnable dismissAction,
            EditTextCaption editText,
            ChatActivity parentFragment) {
        if (context == null || layout == null || editText == null || parentFragment == null) {
            return;
        }
        TLRPC.Chat chat = parentFragment.getCurrentChat();
        if (chat == null || !ChatObject.isChannel(chat) || chat.megagroup || !ChatObject.canPost(chat)) {
            return;
        }
        String username = ChatObject.getPublicUsername(chat);
        if (TextUtils.isEmpty(username)) {
            return;
        }

        ActionBarMenuSubItem item = new ActionBarMenuSubItem(context, false, true, resourcesProvider);
        item.setTextAndIcon(LocaleController.getString(R.string.FluffyInsertSubscribeLinkAction), R.drawable.msg_channel);
        item.setMinimumWidth(org.telegram.messenger.AndroidUtilities.dp(itemMinWidthDp));
        item.setOnClickListener(v -> {
            if (dismissAction != null) {
                dismissAction.run();
            }
            insertSubscribeLink(editText, parentFragment, username);
        });
        layout.addView(item, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, itemHeightDp));
    }

    private static void insertSubscribeLink(EditTextCaption editText, ChatActivity parentFragment, String username) {
        Editable editable = editText.getText();
        if (editable == null) {
            return;
        }
        String label = LocaleController.getString(R.string.FluffySubscribeLinkText);
        String link = "https://" + parentFragment.getMessagesController().linkPrefix + "/" + username;
        int start = Math.max(0, editText.getSelectionStart());
        int end = Math.max(0, editText.getSelectionEnd());
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        String prefix = start > 0 && !Character.isWhitespace(editable.charAt(start - 1)) ? " " : "";
        String suffix = end < editable.length() && !Character.isWhitespace(editable.charAt(end)) ? " " : "";
        String insertion = prefix + label + suffix;
        editable.replace(start, end, insertion);

        int spanStart = start + prefix.length();
        int spanEnd = spanStart + label.length();
        editable.setSpan(new URLSpanReplacement(link), spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        editText.setSelection(spanEnd + suffix.length());
        editText.invalidateEffects();
    }
}
