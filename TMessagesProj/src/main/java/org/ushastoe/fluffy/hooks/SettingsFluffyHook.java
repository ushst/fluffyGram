package org.ushastoe.fluffy.hooks;

import org.telegram.ui.SettingsActivity;
import org.telegram.ui.Components.UItem;
import org.ushastoe.fluffy.patches.SettingsFluffyPatch;

import java.util.ArrayList;

public final class SettingsFluffyHook {
    private SettingsFluffyHook() {
    }

    public static void addFluffySettingsSection(ArrayList<UItem> items) {
        SettingsFluffyPatch.addFluffySettingsSection(items);
    }

    public static boolean onSettingsItemClicked(SettingsActivity target, UItem item) {
        return SettingsFluffyPatch.onSettingsItemClicked(target, item);
    }
}
