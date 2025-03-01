package org.ushastoe.fluffy;

import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.helpers.BaseIconSet;
import org.ushastoe.fluffy.helpers.EmptyIconSet;
import org.ushastoe.fluffy.helpers.SolarIconSet;

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
    public static boolean downloadSpeedBoost;
    public static boolean disableRoundingNumber;
    public static boolean formatTimeWithSeconds;
    public static boolean useSystemFonts;
    public static boolean hideTopBar;
    public static boolean centerTitle;
    public static boolean useSolarIcons;
    public static boolean showStories;
    public static boolean showCallIcon;
    public static boolean moreInfoOnline;
    public static boolean unmuteVideoWithVolume;
    public static boolean saveDel;
    public static boolean saveEdit;

    public static int typeTitle;

    public static final int DOUBLE_TAP_ACTION_NONE = 0;
    public static final int DOUBLE_TAP_ACTION_REACTION = 1;
    public static final int DOUBLE_TAP_ACTION_REPLY = 2;
    public static final int DOUBLE_TAP_ACTION_SAVE = 3;
    public static final int DOUBLE_TAP_ACTION_REPEAT = 4;
    public static final int DOUBLE_TAP_ACTION_EDIT = 5;
    public static final int MESSAGES_DELETED_NOTIFICATION = 6969;

    public static int doubleTapInAction;
    public static int doubleTapOutAction;

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
        showStories = preferences.getBoolean("showStories", true);
        useSolarIcons = preferences.getBoolean("useSolarIcons", true);
        downloadSpeedBoost = preferences.getBoolean("downloadSpeedBoost", false);
        showCallIcon = preferences.getBoolean("showCallIcon", true);
        moreInfoOnline = preferences.getBoolean("moreInfoOnline", false);
        unmuteVideoWithVolume = preferences.getBoolean("unmuteVideoWithVolume", true);
        typeTitle = preferences.getInt("typeTitle", 0);
        disableRoundingNumber = preferences.getBoolean("roundingNumber", false);
        formatTimeWithSeconds = preferences.getBoolean("formatTimeWithSeconds", false);
        centerTitle = preferences.getBoolean("centerTitle", false);
        useSystemFonts = preferences.getBoolean("useSystemFonts", false);
        hideTopBar = preferences.getBoolean("hideTopBar", false);
        doubleTapInAction = preferences.getInt("doubleTapAction", DOUBLE_TAP_ACTION_REACTION);
        doubleTapOutAction = preferences.getInt("doubleTapOutAction", DOUBLE_TAP_ACTION_REACTION);
        saveDel = preferences.getBoolean("saveDel", false);
        saveEdit = preferences.getBoolean("saveEdit", false);

    }

    public static void cameraSwitch() {
        frontCamera = !frontCamera;
        editor.putBoolean("frontCamera", frontCamera).apply();
    }
    public static void saveDelSwitch() {
        saveDel = !saveDel;
        editor.putBoolean("saveDel", saveDel).apply();
    }
    public static void saveEditSwitch() {
        saveEdit = !saveEdit;
        editor.putBoolean("saveEdit", saveEdit).apply();
    }
    public static void writeCamera() {
        editor.putBoolean("frontCamera", frontCamera).apply();
    }
    public static void toggleShouldNotTrustMe() {
        shouldNOTTrustMe = !shouldNOTTrustMe;
        editor.putBoolean("shouldNOTTrustMe", shouldNOTTrustMe).apply();
    }
    public static void togglePremiumMode() {
        premiumMode = !premiumMode;
        editor.putBoolean("premiumMode", premiumMode).apply();
    }
    public static void toogleZodiacShow() {
        zodiacShow = !zodiacShow;
        editor.putBoolean("zodiacShow", zodiacShow).apply();
    }
    public static void toggleVoiceUseCloudflare() {
        voiceUseCloudflare = !voiceUseCloudflare;
        editor.putBoolean("voiceUseCloudflare", voiceUseCloudflare).apply();
    }
    public static void setCfAccountID(String accountID) {
        cfAccountID = accountID;
        editor.putString("cfAccountID", cfAccountID).apply();
    }
    public static void setCfApiToken(String apiToken) {
        cfApiToken = apiToken;
        editor.putString("cfApiToken", cfApiToken).apply();
    }
    public static void toggleShowStories() {
        showStories = !showStories;
        editor.putBoolean("showStories", showStories).apply();
    }
    public static void toggleUseSolarIcons() {
        useSolarIcons = !useSolarIcons;
        editor.putBoolean("useSolarIcons", useSolarIcons).apply();
    }
    public static void toggleShowCallIcon() {
        showCallIcon = !showCallIcon;
        editor.putBoolean("showCallIcon", showCallIcon).apply();
    }
    public static void toggleMoreInfoOnline() {
        moreInfoOnline = !moreInfoOnline;
        editor.putBoolean("moreInfoOnline", moreInfoOnline).apply();
    }
    public static void toggleUnmuteVideoWithVolume() {
        unmuteVideoWithVolume = !unmuteVideoWithVolume;
        editor.putBoolean("unmuteVideoWithVolume", unmuteVideoWithVolume).apply();
    }
    public static void toogleDownloadSpeedBoost() {
        downloadSpeedBoost = !downloadSpeedBoost;
        editor.putBoolean("downloadSpeedBoost", downloadSpeedBoost).apply();
    }
    public static void toggleCenterTitle() {
        centerTitle = !centerTitle;
        editor.putBoolean("centerTitle", centerTitle).apply();
    }
    public static void toogleRoundingNumber() {
        disableRoundingNumber = !disableRoundingNumber;
        editor.putBoolean("roundingNumber", disableRoundingNumber).apply();
    }
    public static void toogleFormatTimeWithSeconds() {
        formatTimeWithSeconds = !formatTimeWithSeconds;
        editor.putBoolean("formatTimeWithSeconds", formatTimeWithSeconds).apply();
    }
    public static void toogleUseSystemFonts() {
        useSystemFonts = !useSystemFonts;
        editor.putBoolean("useSystemFonts", useSystemFonts).apply();
    }
    public static void toogleHideTopBar() {
        hideTopBar = !hideTopBar;
        editor.putBoolean("hideTopBar", hideTopBar).apply();
    }

    public static void setDoubleTapInAction(int action) {
        doubleTapInAction = action;
        editor.putInt("doubleTapInAction", doubleTapInAction).apply();
    }

    public static void setDoubleTapOutAction(int action) {
        doubleTapOutAction = action;
        editor.putInt("doubleTapOutAction", doubleTapOutAction).apply();
    }

    public static BaseIconSet getIconPack() {
        return useSolarIcons ? new SolarIconSet() : new EmptyIconSet();
    }

    public static String getUsername() {
        String title;
        TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        if (!TextUtils.isEmpty(UserObject.getPublicUsername(user))) {
            title = UserObject.getPublicUsername(user);
        } else {
            title = UserObject.getFirstName(user);
        }
        return title;
    }

    public static String getTitleHeader() {
        switch (typeTitle) {
            case 0:
                return fluffyConfig.getUsername();
            case 1:
                return "fluffy";
            case 2:
                return "telegram";
            case 3:
                return "Disable";
            default:
                return LocaleController.getString(R.string.AppName);
        }
    }

    public static void setTypeTitle(int type) {
        typeTitle = type;
        editor.putInt("typeTitle", typeTitle).apply();
    }

}
