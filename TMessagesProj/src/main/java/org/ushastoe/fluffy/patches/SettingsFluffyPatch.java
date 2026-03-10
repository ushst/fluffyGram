package org.ushastoe.fluffy.patches;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.SettingsActivity;
import org.ushastoe.fluffy.ui.FluffySettingsActivity;

import java.util.ArrayList;

public final class SettingsFluffyPatch {
    public static final int FLUFFY_SETTINGS_ITEM_ID = 1001;

    private SettingsFluffyPatch() {
    }

    public static void addFluffySettingsSection(ArrayList<UItem> items) {
        if (items == null) {
            return;
        }
        if (!items.isEmpty() && items.get(items.size() - 1).viewType != UniversalAdapter.VIEW_TYPE_SHADOW) {
            items.add(UItem.asShadow(null));
        }
        items.add(UItem.asHeader(LocaleController.getString(R.string.FluffySettingsSection)));
        items.add(SettingsActivity.SettingCell.Factory.of(
                FLUFFY_SETTINGS_ITEM_ID,
                0xFF2BB5A8,
                0xFF168F84,
                R.drawable.fluffy_settings_icon,
                LocaleController.getString(R.string.FluffySettings)
        ));
        items.add(UItem.asShadow(null));
    }

    public static boolean onSettingsItemClicked(SettingsActivity target, UItem item) {
        if (target == null || item == null) {
            return false;
        }
        if (item.id == FLUFFY_SETTINGS_ITEM_ID) {
            target.presentFragment(new FluffySettingsActivity());
            return true;
        }
        return false;
    }
}
