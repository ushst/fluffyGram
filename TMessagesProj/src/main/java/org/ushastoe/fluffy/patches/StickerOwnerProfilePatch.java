package org.ushastoe.fluffy.patches;

import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;

public final class StickerOwnerProfilePatch {

    public static final int MENU_ID = 105;

    private StickerOwnerProfilePatch() {
    }

    public static void addMenuItem(ActionBarMenuItem optionsButton) {
        if (optionsButton == null) {
            return;
        }
        if (optionsButton.getContext() == null) {
            return;
        }
        optionsButton.addSubItem(MENU_ID, R.drawable.msg_openprofile, LocaleController.getString(R.string.FluffyStickerPackOwner));
    }

    public static boolean handleMenuClick(
            int id,
            TLRPC.TL_messages_stickerSet stickerSet,
            BaseFragment fragment,
            View containerView,
            Theme.ResourcesProvider resourcesProvider,
            int currentAccount
    ) {
        if (id != MENU_ID || stickerSet == null || stickerSet.set == null) {
            return false;
        }

        long userId = extractOwnerUserId(stickerSet.set.id);
        if (fragment != null) {
            TLRPC.User user = fragment.getMessagesController().getUser(userId);
            if (user != null) {
                MessagesController.getInstance(currentAccount).openChatOrProfileWith(user, null, fragment, 0, false);
                return true;
            }
        }

        try {
            AndroidUtilities.addToClipboard(Long.toString(userId));
            if (containerView instanceof FrameLayout) {
                BulletinFactory.of((FrameLayout) containerView, resourcesProvider).createCopyLinkBulletin().show();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return true;
    }

    private static long extractOwnerUserId(long stickerSetId) {
        long userId = stickerSetId >> 32;
        if (((stickerSetId >> 24) & 0xff) != 0) {
            userId += 0x100000000L;
        }
        return userId;
    }
}
