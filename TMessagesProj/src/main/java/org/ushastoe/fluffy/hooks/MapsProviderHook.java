package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.IMapsProvider;
import org.ushastoe.fluffy.patches.MapsProviderPatch;

public final class MapsProviderHook {

    private MapsProviderHook() {
    }

    public static IMapsProvider createMapsProvider() {
        return MapsProviderPatch.createMapsProvider();
    }
}
