package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.patches.DocumentMetadataPatch;
import org.ushastoe.fluffy.ui.FluffyDocumentMetadataActivity;

import java.util.ArrayList;

public final class DocumentMetadataMenuHook {

    public static final int OPTION_DOCUMENT_METADATA = 9999;

    private DocumentMetadataMenuHook() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (items == null || options == null || icons == null || selectedMessage == null) {
            return;
        }
        if (!MessageActionsHook.isDocumentMetadataEnabled() || !DocumentMetadataPatch.canShowForMessage(selectedMessage)) {
            return;
        }
        items.add(LocaleController.getString(R.string.FluffyDocumentMetadataAction));
        options.add(OPTION_DOCUMENT_METADATA);
        icons.add(R.drawable.msg_info);
    }

    public static void openDocumentMetadata(BaseFragment fragment, MessageObject selectedMessage) {
        if (fragment == null || selectedMessage == null) {
            return;
        }
        fragment.presentFragment(new FluffyDocumentMetadataActivity(selectedMessage));
    }
}
