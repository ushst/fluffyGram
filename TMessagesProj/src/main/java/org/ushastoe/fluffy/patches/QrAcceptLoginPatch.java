package org.ushastoe.fluffy.patches;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

public final class QrAcceptLoginPatch {
    private QrAcceptLoginPatch() {
    }

    public static boolean handleError(BaseFragment fragment, TLRPC.TL_error error) {
        if (fragment == null || error == null || error.text == null) {
            return false;
        }
        String text = error.text.trim();
        if (!text.contains("AUTH_TOKEN_ALREADY_ACCEPTED")) {
            return false;
        }
        if (BulletinFactory.canShowBulletin(fragment)) {
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.raw.contact_check, LocaleController.getString(R.string.Done))
                    .show();
        }
        return true;
    }
}
