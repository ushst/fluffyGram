package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.MediaOnlyProxyPatch;

public final class MediaOnlyProxyHook {
    private MediaOnlyProxyHook() {
    }

    public static void onMediaOperationStart(Object operation) {
        MediaOnlyProxyPatch.onMediaOperationStart(operation);
    }

    public static void onMediaOperationStop(Object operation) {
        MediaOnlyProxyPatch.onMediaOperationStop(operation);
    }
}
