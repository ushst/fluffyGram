package org.ushastoe.fluffy.hooks

import org.ushastoe.fluffy.patches.FluffyFeaturePatch

object HookBridgeTemplate {
    @JvmStatic
    fun applyFeaturePatch(target: Any?) {
        FluffyFeaturePatch.apply(target)
    }
}
