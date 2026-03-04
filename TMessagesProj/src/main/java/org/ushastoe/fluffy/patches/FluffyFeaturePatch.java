package org.ushastoe.fluffy.patches;

public final class FluffyFeaturePatch {
    private FluffyFeaturePatch() {
    }

    public static void apply(Object target) {
        if (target == null) {
            return;
        }
        // Place custom feature logic here.
    }
}
