package org.ushastoe.fluffy;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Класс для управления настройками мода fluffy.
 * Обеспечивает загрузку, сохранение и доступ к различным параметрам конфигурации.
 */
public final class fluffyConfig {

    private static SharedPreferences preferences;

    // --- Ключи для SharedPreferences ---
    private static final String PREFS_NAME = "fluffyConfig";
    private static final String KEY_MENU_REPLY_ENABLED = "menu_reply_enabled";
    private static final String KEY_MENU_FORWARD_WO_AUTHOR_ENABLED = "menu_forward_wo_author_enabled";
    private static final String KEY_MENU_JSON_VIEWER_ENABLED = "menu_json_viewer_enabled";
    private static final String KEY_MENU_CLEAR_CACHE_ENABLED = "menu_clear_cache_enabled";
    private static final String KEY_FRONT_CAMERA = "frontCamera";
    private static final String KEY_SHOULD_NOT_TRUST_ME = "shouldNOTTrustMe";
    private static final String KEY_PREMIUM_MODE = "premiumMode";
    private static final String KEY_VOICE_USE_CLOUDFLARE = "voiceUseCloudflare";
    private static final String KEY_CF_ACCOUNT_ID = "cfAccountID";
    private static final String KEY_CF_API_TOKEN = "cfApiToken";
    private static final String KEY_ZODIAC_SHOW = "zodiacShow";
    private static final String KEY_DOWNLOAD_SPEED_BOOST = "downloadSpeedBoost";
    private static final String KEY_DISABLE_ROUNDING_NUMBER = "roundingNumber";
    private static final String KEY_FORMAT_TIME_WITH_SECONDS = "formatTimeWithSeconds";
    private static final String KEY_USE_SYSTEM_FONTS = "useSystemFonts";
    private static final String KEY_HIDE_TOP_BAR = "hideTopBar";
    private static final String KEY_CENTER_TITLE = "centerTitle";
    private static final String KEY_CENTER_TITLE_IN_CHAT = "centerTitleInChat";
    private static final String KEY_USE_SOLAR_ICONS = "useSolarIcons";
    private static final String KEY_HIDE_BUTTON_WRITE = "hideButtonWrite";
    private static final String KEY_SHOW_STORIES = "showStories";
    private static final String KEY_SHOW_DIVIDER = "showDivider";
    private static final String KEY_SHOW_CALL_ICON = "showCallIcon";
    private static final String KEY_NEW_SWITCH_STYLE = "newSwitchStyle";
    private static final String KEY_MORE_INFO_ONLINE = "moreInfoOnline";
    private static final String KEY_UNMUTE_VIDEO_WITH_VOLUME = "unmuteVideoWithVolume";
    private static final String KEY_SAVE_DELETED_MESSAGES = "saveDel";
    private static final String KEY_SAVE_EDITED_MESSAGES = "saveEdit";
    private static final String KEY_HIDE_GIFT = "hideGift";
    private static final String KEY_SHOW_COPY_PHOTO = "showCopyPhoto";
    private static final String KEY_SHOW_FORWARD_WO_AUTHORSHIP = "showForwardWoAuthorship";
    private static final String KEY_SHOW_VIEW_MESSAGE_FROM_USER = "showViewMessageFromUser";
    private static final String KEY_SHOW_JSON = "showJSON";
    private static final String KEY_READ_STICKER_MODE = "readSticker";
    private static final String KEY_TITLE_TYPE = "typeTitle";
    private static final String KEY_TRANSPARENCY = "transparency";
    private static final String KEY_STICKER_SIZE = "stickerSize";
    private static final String KEY_STICKER_RADIUS = "stickerRadius";
    private static final String KEY_DOUBLE_TAP_IN_ACTION = "doubleTapInAction";
    private static final String KEY_DOUBLE_TAP_OUT_ACTION = "doubleTapOutAction";
    private static final String KEY_ID_HIDE_WALLPAPER = "idHideWallpaper";



    // --- Действия для двойного нажатия ---
    public static final int DOUBLE_TAP_ACTION_NONE = 0;
    public static final int DOUBLE_TAP_ACTION_REACTION = 1;
    public static final int DOUBLE_TAP_ACTION_REPLY = 2;
    public static final int DOUBLE_TAP_ACTION_COPY = 3;
    public static final int DOUBLE_TAP_ACTION_FORWARD = 4;
    public static final int DOUBLE_TAP_ACTION_EDIT = 5;
    public static final int DOUBLE_TAP_ACTION_SAVE = 6;
    public static final int DOUBLE_TAP_ACTION_DELETE = 7;

    public static final int MESSAGES_DELETED_NOTIFICATION = 6969;

    // --- Переменные настроек ---
    public static boolean menuReplyEnabled;
    public static boolean menuForwardWoAuthorEnabled;
    public static boolean menuJsonViewerEnabled;
    public static boolean menuClearFromCacheEnabled;
    public static boolean frontCamera;
    public static boolean shouldNOTTrustMe;
    public static boolean premiumMode;
    public static int voiceUseCloudflare;
    public static String cfAccountID;
    public static String cfApiToken;
    public static boolean zodiacShow;
    public static boolean downloadSpeedBoost;
    public static boolean disableRoundingNumber;
    public static boolean formatTimeWithSeconds;
    public static boolean useSystemFonts;
    public static boolean hideTopBar;
    public static boolean centerTitle;
    public static boolean centerTitleInChat;
    public static boolean useSolarIcons;
    public static boolean hideButtonWrite;
    public static boolean showStories;
    public static boolean showDivider;
    public static boolean showCallIcon;
    public static boolean newSwitchStyle;
    public static boolean moreInfoOnline;
    public static boolean unmuteVideoWithVolume;
    public static boolean saveDeletedMessages;
    public static boolean saveEditedMessages;
    public static boolean hideGift;
    public static boolean showCopyPhoto;
    public static boolean showForwardWoAuthorship;
    public static boolean showViewMessageFromUser;
    public static boolean showJSON;
    public static int readStickerMode;
    public static int titleType;
    public static int transparency;
    public static int stickerSize;
    public static int stickerRadius;
    public static int doubleTapInAction;
    public static int doubleTapOutAction;
    public static boolean sendPhotoAsSticker = false;


    public static final ArrayList<Long> blockSticker = new ArrayList<>();

    // Запрещаем создание экземпляров класса
    private fluffyConfig() {}

    /**
     * Инициализирует конфигурацию, загружая настройки из SharedPreferences.
     */
    public static void init() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Activity.MODE_PRIVATE);
        blockSticker.add(5314569068664091371L);
        load();
    }

    /**
     * Загружает все настройки из SharedPreferences в статические переменные.
     */
    public static void load() {
        menuReplyEnabled = preferences.getBoolean(KEY_MENU_REPLY_ENABLED, true);
        menuForwardWoAuthorEnabled = preferences.getBoolean(KEY_MENU_FORWARD_WO_AUTHOR_ENABLED, true);
        menuJsonViewerEnabled = preferences.getBoolean(KEY_MENU_JSON_VIEWER_ENABLED, false);
        menuClearFromCacheEnabled = preferences.getBoolean(KEY_MENU_CLEAR_CACHE_ENABLED, true);
        frontCamera = preferences.getBoolean(KEY_FRONT_CAMERA, true);
        premiumMode = preferences.getBoolean(KEY_PREMIUM_MODE, false);
        voiceUseCloudflare = preferences.getInt(KEY_VOICE_USE_CLOUDFLARE, 0);
        cfApiToken = preferences.getString(KEY_CF_API_TOKEN, "");
        cfAccountID = preferences.getString(KEY_CF_ACCOUNT_ID, "");
        zodiacShow = preferences.getBoolean(KEY_ZODIAC_SHOW, false);
        showStories = preferences.getBoolean(KEY_SHOW_STORIES, true);
        useSolarIcons = preferences.getBoolean(KEY_USE_SOLAR_ICONS, true);
        downloadSpeedBoost = preferences.getBoolean(KEY_DOWNLOAD_SPEED_BOOST, false);
        showCallIcon = preferences.getBoolean(KEY_SHOW_CALL_ICON, true);
        moreInfoOnline = preferences.getBoolean(KEY_MORE_INFO_ONLINE, false);
        unmuteVideoWithVolume = preferences.getBoolean(KEY_UNMUTE_VIDEO_WITH_VOLUME, true);
        titleType = preferences.getInt(KEY_TITLE_TYPE, 0);
        readStickerMode = preferences.getInt(KEY_READ_STICKER_MODE, 0);
        disableRoundingNumber = preferences.getBoolean(KEY_DISABLE_ROUNDING_NUMBER, false);
        formatTimeWithSeconds = preferences.getBoolean(KEY_FORMAT_TIME_WITH_SECONDS, false);
        centerTitle = preferences.getBoolean(KEY_CENTER_TITLE, false);
        centerTitleInChat = preferences.getBoolean(KEY_CENTER_TITLE_IN_CHAT, false);
        useSystemFonts = preferences.getBoolean(KEY_USE_SYSTEM_FONTS, false);
        hideTopBar = preferences.getBoolean(KEY_HIDE_TOP_BAR, false);
        doubleTapInAction = preferences.getInt(KEY_DOUBLE_TAP_IN_ACTION, DOUBLE_TAP_ACTION_REACTION);
        doubleTapOutAction = preferences.getInt(KEY_DOUBLE_TAP_OUT_ACTION, DOUBLE_TAP_ACTION_REACTION);
        saveDeletedMessages = preferences.getBoolean(KEY_SAVE_DELETED_MESSAGES, false);
        saveEditedMessages = preferences.getBoolean(KEY_SAVE_EDITED_MESSAGES, false);
        hideGift = preferences.getBoolean(KEY_HIDE_GIFT, false);
        newSwitchStyle = preferences.getBoolean(KEY_NEW_SWITCH_STYLE, false);
        transparency = preferences.getInt(KEY_TRANSPARENCY, 255);
        stickerSize = preferences.getInt(KEY_STICKER_SIZE, 20);
        stickerRadius = preferences.getInt(KEY_STICKER_RADIUS, 0);
        hideButtonWrite = preferences.getBoolean(KEY_HIDE_BUTTON_WRITE, false);
        showCopyPhoto = preferences.getBoolean(KEY_SHOW_COPY_PHOTO, true);
        showForwardWoAuthorship = preferences.getBoolean(KEY_SHOW_FORWARD_WO_AUTHORSHIP, true);
        showViewMessageFromUser = preferences.getBoolean(KEY_SHOW_VIEW_MESSAGE_FROM_USER, true);
        showJSON = preferences.getBoolean(KEY_SHOW_JSON, true);
        showDivider = preferences.getBoolean(KEY_SHOW_DIVIDER, true);
    }

    // --- Методы-переключатели (Toggles) ---

    public static void toggleFrontCamera() {
        frontCamera = toggleBooleanSetting(KEY_FRONT_CAMERA, frontCamera);
    }

    public static void toggleSaveDeletedMessages() {
        saveDeletedMessages = toggleBooleanSetting(KEY_SAVE_DELETED_MESSAGES, saveDeletedMessages);
    }

    public static void toggleSaveEditedMessages() {
        saveEditedMessages = toggleBooleanSetting(KEY_SAVE_EDITED_MESSAGES, saveEditedMessages);
    }

    public static void toggleGift() {
        hideGift = toggleBooleanSetting(KEY_HIDE_GIFT, hideGift);
    }

    public static void toggleHideButtonWrite() {
        hideButtonWrite = toggleBooleanSetting(KEY_HIDE_BUTTON_WRITE, hideButtonWrite);
    }

    public static void toggleShouldNotTrustMe() {
        shouldNOTTrustMe = toggleBooleanSetting(KEY_SHOULD_NOT_TRUST_ME, shouldNOTTrustMe);
    }

    public static void togglePremiumMode() {
        premiumMode = toggleBooleanSetting(KEY_PREMIUM_MODE, premiumMode);
    }

    public static void toggleZodiacShow() {
        zodiacShow = toggleBooleanSetting(KEY_ZODIAC_SHOW, zodiacShow);
    }

    public static void toggleShowStories() {
        showStories = toggleBooleanSetting(KEY_SHOW_STORIES, showStories);
    }
    public static void toggleShowDivider() {
        showDivider = toggleBooleanSetting(KEY_SHOW_DIVIDER, showDivider);
    }

    public static void toggleUseSolarIcons() {
        useSolarIcons = toggleBooleanSetting(KEY_USE_SOLAR_ICONS, useSolarIcons);
    }

    public static void toggleShowCallIcon() {
        showCallIcon = toggleBooleanSetting(KEY_SHOW_CALL_ICON, showCallIcon);
    }


    public static void toggleMoreInfoOnline() {
        moreInfoOnline = toggleBooleanSetting(KEY_MORE_INFO_ONLINE, moreInfoOnline);
    }

    public static void toggleUnmuteVideoWithVolume() {
        unmuteVideoWithVolume = toggleBooleanSetting(KEY_UNMUTE_VIDEO_WITH_VOLUME, unmuteVideoWithVolume);
    }

    public static void toggleDownloadSpeedBoost() {
        downloadSpeedBoost = toggleBooleanSetting(KEY_DOWNLOAD_SPEED_BOOST, downloadSpeedBoost);
    }

    public static void toggleCenterTitle() {
        centerTitle = toggleBooleanSetting(KEY_CENTER_TITLE, centerTitle);
    }

    public static void toggleCenterTitleInChat() {
        centerTitleInChat = toggleBooleanSetting(KEY_CENTER_TITLE_IN_CHAT, centerTitleInChat);
    }
    public static void toggleRoundingNumber() {
        disableRoundingNumber = toggleBooleanSetting(KEY_DISABLE_ROUNDING_NUMBER, disableRoundingNumber);
    }

    public static void toggleFormatTimeWithSeconds() {
        formatTimeWithSeconds = toggleBooleanSetting(KEY_FORMAT_TIME_WITH_SECONDS, formatTimeWithSeconds);
    }

    public static void toggleUseSystemFonts() {
        useSystemFonts = toggleBooleanSetting(KEY_USE_SYSTEM_FONTS, useSystemFonts);
    }

    public static void toggleHideTopBar() {
        hideTopBar = toggleBooleanSetting(KEY_HIDE_TOP_BAR, hideTopBar);
    }

    public static void toggleNewSwitchStyle() {
        newSwitchStyle = toggleBooleanSetting(KEY_NEW_SWITCH_STYLE, newSwitchStyle);
    }

    public static void toggleShowCopyPhoto() {
        showCopyPhoto = toggleBooleanSetting(KEY_SHOW_COPY_PHOTO, showCopyPhoto);
    }

    public static void toggleShowForwardWoAuthorship() {
        showForwardWoAuthorship = toggleBooleanSetting(KEY_SHOW_FORWARD_WO_AUTHORSHIP, showForwardWoAuthorship);
    }

    public static void toggleShowViewMessageFromUser() {
        showViewMessageFromUser = toggleBooleanSetting(KEY_SHOW_VIEW_MESSAGE_FROM_USER, showViewMessageFromUser);
    }

    public static void toggleShowJSON() {
        showJSON = toggleBooleanSetting(KEY_SHOW_JSON, showJSON);
    }

    // --- Сеттеры для разных типов данных ---

    public static void setCfAccountID(String accountID) {
        cfAccountID = accountID;
        setStringSetting(KEY_CF_ACCOUNT_ID, accountID);
    }

    public static void setCfApiToken(String apiToken) {
        cfApiToken = apiToken;
        setStringSetting(KEY_CF_API_TOKEN, apiToken);
    }

    public static void setTransparency(int value) {
        transparency = setIntSetting(KEY_TRANSPARENCY, value);
    }

    public static void setStickerSize(int value) {
        stickerSize = setIntSetting(KEY_STICKER_SIZE, value);
    }

    public static void setStickerRadius(int value) {
        stickerRadius = setIntSetting(KEY_STICKER_RADIUS, value);
    }

    public static void setDoubleTapInAction(int action) {
        doubleTapInAction = setIntSetting(KEY_DOUBLE_TAP_IN_ACTION, action);
    }

    public static void setDoubleTapOutAction(int action) {
        doubleTapOutAction = setIntSetting(KEY_DOUBLE_TAP_OUT_ACTION, action);
    }

    public static void setTitleType(int type) {
        titleType = setIntSetting(KEY_TITLE_TYPE, type);
    }

    public static void setReadStickerMode(int type) {
        readStickerMode = setIntSetting(KEY_READ_STICKER_MODE, type);
    }

    public static void setProviderVoice(int type) {
        voiceUseCloudflare = setIntSetting(KEY_VOICE_USE_CLOUDFLARE, type);
    }

    public static void setSendPhotoAsSticker(boolean shouldSendAsSticker) {
        fluffyConfig.sendPhotoAsSticker = shouldSendAsSticker;
    }

    // --- Утилитарные методы ---

    /**
     * Возвращает класс иконок в зависимости от настроек.
     * @return SolarIconSet или EmptyIconSet.
     */
    public static BaseIconSet getIconPack() {
        return useSolarIcons ? new SolarIconSet() : new EmptyIconSet();
    }

    /**
     * Возвращает имя пользователя или его публичное имя.
     * @return Имя пользователя.
     */
    public static String getUsername() {
        TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        if (user == null) {
            return "";
        }
        String publicUsername = UserObject.getPublicUsername(user);
        return !TextUtils.isEmpty(publicUsername) ? publicUsername : UserObject.getFirstName(user);
    }

    /**
     * Возвращает имя пользователя.
     * @return Имя пользователя.
     */
    public static String getFirstName() {
        TLRPC.User user = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser();
        return UserObject.getFirstName(user);
    }

    /**
     * Определяет заголовок для хедера на основе настроек.
     * @return Строка заголовка.
     */
    public static String getTitleHeader() {
        switch (titleType) {
            case 0:
                return getUsername();
            case 1:
                return "fluffy";
            case 2:
                return "telegram";
            case 3:
                return "Disable";
            default:
                return LocaleController.getString("AppName", R.string.AppName);
        }
    }

    /**
     * Проверяет, используется ли Cloudflare для голосовых сообщений.
     * @return true, если Cloudflare используется.
     */
    public static boolean useCloudFlare() {
        return voiceUseCloudflare == 1;
    }

    /**
     * Переключает видимость обоев для указанного чата.
     * @param id ID чата.
     */
    public static void toggleIdInWallpaperChat(long id) {
        String idHideWallpaper = preferences.getString(KEY_ID_HIDE_WALLPAPER, "");
        List<String> ids = new ArrayList<>(Arrays.asList(idHideWallpaper.split(";")));
        ids.removeAll(Arrays.asList("", null)); // Очистка от пустых элементов

        String idString = String.valueOf(id);

        if (ids.contains(idString)) {
            ids.remove(idString);
        } else {
            ids.add(idString);
        }

        setStringSetting(KEY_ID_HIDE_WALLPAPER, TextUtils.join(";", ids));
    }

    /**
     * Проверяет, нужно ли показывать обои для указанного чата.
     * @param id ID чата.
     * @return true, если обои должны быть видны.
     */
    public static boolean shouldShowWallpaperForChat(long id) {
        String idHideWallpaper = preferences.getString(KEY_ID_HIDE_WALLPAPER, "");
        if (idHideWallpaper.isEmpty()) {
            return true;
        }
        List<String> ids = Arrays.asList(idHideWallpaper.split(";"));
        return !ids.contains(String.valueOf(id));
    }


    // --- Приватные хелперы для работы с SharedPreferences ---

    /**
     * Обобщенный метод для переключения boolean-настроек.
     * @param key Ключ настройки.
     * @param currentValue Текущее значение.
     * @return Новое значение.
     */
    private static boolean toggleBooleanSetting(String key, boolean currentValue) {
        boolean newValue = !currentValue;
        preferences.edit().putBoolean(key, newValue).apply();
        return newValue;
    }

    /**
     * Обобщенный метод для сохранения int-настроек.
     * @param key Ключ настройки.
     * @param value Значение для сохранения.
     * @return Сохраненное значение.
     */
    private static int setIntSetting(String key, int value) {
        preferences.edit().putInt(key, value).apply();
        return value;
    }

    /**
     * Обобщенный метод для сохранения String-настроек.
     * @param key Ключ настройки.
     * @param value Значение для сохранения.
     */
    private static void setStringSetting(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }
}