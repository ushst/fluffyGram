package org.ushastoe.fluffy.hooks;

import android.content.Intent;

import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.patches.LegacyLaunchTaskRootPatch;

public final class LegacyLaunchTaskRootHook {

    private LegacyLaunchTaskRootHook() {
    }

    public static boolean relaunchIfNeeded(LaunchActivity activity, Intent intent) {
        return LegacyLaunchTaskRootPatch.relaunchIfNeeded(activity, intent);
    }
}
