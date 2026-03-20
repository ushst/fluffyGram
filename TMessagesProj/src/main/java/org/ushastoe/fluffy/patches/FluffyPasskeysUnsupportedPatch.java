package org.ushastoe.fluffy.patches;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;

public final class FluffyPasskeysUnsupportedPatch {
    private FluffyPasskeysUnsupportedPatch() {
    }

    public static void showUnsupportedAlert(BaseFragment fragment) {
        if (fragment == null) {
            return;
        }
        AlertsCreator.showSimpleAlert(
                fragment,
                LocaleController.getString(R.string.FluffyPasskeysUnsupportedTitle),
                LocaleController.getString(R.string.FluffyPasskeysUnsupportedText)
        );
    }
}
