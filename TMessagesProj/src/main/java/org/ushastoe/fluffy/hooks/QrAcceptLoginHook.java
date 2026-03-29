package org.ushastoe.fluffy.hooks;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.ushastoe.fluffy.patches.QrAcceptLoginPatch;

public final class QrAcceptLoginHook {
    private QrAcceptLoginHook() {
    }

    public static boolean handleError(BaseFragment fragment, TLRPC.TL_error error) {
        return QrAcceptLoginPatch.handleError(fragment, error);
    }
}
