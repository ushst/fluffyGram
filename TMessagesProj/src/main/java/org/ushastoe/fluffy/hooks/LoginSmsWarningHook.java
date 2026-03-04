package org.ushastoe.fluffy.hooks;

import android.content.Context;
import android.widget.LinearLayout;
import org.ushastoe.fluffy.patches.LoginSmsWarningPatch;

public final class LoginSmsWarningHook {
    private LoginSmsWarningHook() {
    }

    public static void addSmsOnlyOfficialWarning(LinearLayout target, Context context) {
        LoginSmsWarningPatch.addSmsOnlyOfficialWarning(target, context);
    }
}
