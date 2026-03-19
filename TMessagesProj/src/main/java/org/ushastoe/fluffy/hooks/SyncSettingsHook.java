package org.ushastoe.fluffy.hooks;

import android.content.Context;

import org.ushastoe.fluffy.sync.FluffySyncManager;

public final class SyncSettingsHook {

    private SyncSettingsHook() {
    }

    public static void initialize(Context context) {
        FluffySyncManager.getInstance().init(context);
    }
}
