package org.ushastoe.fluffy.hooks;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.patches.InlineCallbackDataPatch;

public final class InlineCallbackDataHook {

    private InlineCallbackDataHook() {
    }

    public static boolean showMenu(ChatActivity fragment, ChatMessageCell cell, TLRPC.KeyboardButton button) {
        return InlineCallbackDataPatch.showMenu(fragment, cell, button);
    }
}
