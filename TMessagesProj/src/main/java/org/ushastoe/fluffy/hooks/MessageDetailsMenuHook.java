package org.ushastoe.fluffy.hooks;

import java.util.ArrayList;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.ui.FluffyMessageDetailsActivity;

public final class MessageDetailsMenuHook {

    public static final int OPTION_DETAILS = 9992;

    private MessageDetailsMenuHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (items == null || options == null || icons == null || selectedMessage == null || selectedMessage.messageOwner == null) {
            return;
        }
        items.add(LocaleController.getString(R.string.MessageDetails));
        options.add(OPTION_DETAILS);
        icons.add(R.drawable.msg_info);
    }

    public static void openDetails(BaseFragment fragment, MessageObject selectedMessage) {
        if (fragment == null || selectedMessage == null || selectedMessage.messageOwner == null) {
            return;
        }
        fragment.presentFragment(new FluffyMessageDetailsActivity(selectedMessage));
    }
}
