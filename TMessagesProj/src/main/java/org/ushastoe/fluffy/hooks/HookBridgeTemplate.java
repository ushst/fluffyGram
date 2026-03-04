package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.FluffyFeaturePatch;

public final class HookBridgeTemplate {
    private HookBridgeTemplate() {
    }

    public static void applyFeaturePatch(Object target) {
        FluffyFeaturePatch.apply(target);
    }
}
