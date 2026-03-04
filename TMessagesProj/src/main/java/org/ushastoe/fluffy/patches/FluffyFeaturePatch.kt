package org.ushastoe.fluffy.patches

object FluffyFeaturePatch {
    @JvmStatic
    fun apply(target: Any?) {
        if (target == null) {
            return
        }
        // Place custom feature logic here.
    }
}
