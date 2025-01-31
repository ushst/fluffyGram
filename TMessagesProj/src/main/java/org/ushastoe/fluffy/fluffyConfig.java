package org.ushastoe.fluffy;

import android.app.Activity;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

public class fluffyConfig {
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    public static boolean frontCamera;

    public static void init() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("fluffyConfig", Activity.MODE_PRIVATE);
        editor = preferences.edit();
        load();
    }

    public static void load() {
        frontCamera = preferences.getBoolean("frontCamera", true);
    }

    public static void cameraSwitch() {
        frontCamera = !frontCamera;
        editor.putBoolean("frontCamera", frontCamera);
        editor.commit();
    }
}
