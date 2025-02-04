package org.ushastoe.fluffy;

import android.app.Activity;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

public class fluffyConfig {
    public static SharedPreferences preferences;
    public static SharedPreferences.Editor editor;

    public static boolean frontCamera;
    public static boolean shouldNOTTrustMe;

    public static boolean premiumMode;
    public static boolean voiceUseCloudflare;
    public static String cfAccountID;
    public static String cfApiToken;
    public static boolean zodiacShow;

    public static void init() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences("fluffyConfig", Activity.MODE_PRIVATE);
        editor = preferences.edit();
        load();
    }

    public static void load() {
        frontCamera = preferences.getBoolean("frontCamera", true);
        premiumMode = preferences.getBoolean("premiumMode", false);
        voiceUseCloudflare = preferences.getBoolean("voiceUseCloudflare", false);
        cfApiToken = preferences.getString("cfApiToken", "");
        cfAccountID = preferences.getString("cfAccountID", "");
        zodiacShow = preferences.getBoolean("zodiacShow", false);
    }

    public static void cameraSwitch() {
        frontCamera = !frontCamera;
        editor.putBoolean("frontCamera", frontCamera);
        editor.commit();
    }
    public static void writeCamera() {
        editor.putBoolean("frontCamera", frontCamera);
        editor.commit();
    }

    public static void toggleShouldNotTrustMe() {
        shouldNOTTrustMe = !shouldNOTTrustMe;
        editor.putBoolean("shouldNOTTrustMe", shouldNOTTrustMe);
        editor.commit();
    }
    public static void togglePremiumMode() {
        premiumMode = !premiumMode;
        editor.putBoolean("premiumMode", premiumMode);
        editor.commit();
    }

    public static void toogleZodiacShow() {
        zodiacShow = !zodiacShow;
        editor.putBoolean("zodiacShow", zodiacShow);
        editor.commit();
    }

    public static void toggleVoiceUseCloudflare() {
        voiceUseCloudflare = !voiceUseCloudflare;
        editor.putBoolean("voiceUseCloudflare", voiceUseCloudflare);
        editor.apply();
    }

    public static void setCfAccountID(String accountID) {
        cfAccountID = accountID;
        editor.putString("cfAccountID", cfAccountID);
        editor.apply();
    }

    public static void setCfApiToken(String apiToken) {
        cfApiToken = apiToken;
        editor.putString("cfApiToken", cfApiToken);
        editor.apply();
    }



    public static String getTitleHeader() {
        return "fluffy";
    }

}
