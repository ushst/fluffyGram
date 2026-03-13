package org.ushastoe.fluffy.patches;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.GoogleMapsProvider;
import org.telegram.messenger.IMapsProvider;
import org.ushastoe.fluffy.maps.OsmMapsProvider;

import java.lang.reflect.Field;

public final class MapsProviderPatch {

    private MapsProviderPatch() {
    }

    public static IMapsProvider createMapsProvider() {
        if (AppearanceSettingsPatch.getMapProvider() == AppearanceSettingsPatch.MAP_PROVIDER_OPENSTREETMAP) {
            return new OsmMapsProvider();
        }
        return new GoogleMapsProvider();
    }

    public static void onMapProviderChanged() {
        try {
            Field field = ApplicationLoader.class.getDeclaredField("mapsProvider");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Throwable ignore) {
        }
    }
}
