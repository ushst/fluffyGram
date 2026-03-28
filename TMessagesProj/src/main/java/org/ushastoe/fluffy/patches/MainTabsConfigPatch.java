package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;

import java.util.ArrayList;
import java.util.List;

public final class MainTabsConfigPatch {
    public static final int TAB_CHATS = 0;
    public static final int TAB_CONTACTS = 1;
    public static final int TAB_SETTINGS = 2;
    public static final int TAB_PROFILE = 3;
    public static final int TAB_ACTION_NONE = 0;
    public static final int TAB_ACTION_OPEN_SAVED_MESSAGES = 1;
    public static final int TAB_ACTION_OPEN_ACCOUNT_SELECTOR = 2;
    public static final int TAB_ACTION_OPEN_TABS_MENU = 3;
    public static final int TAB_ACTION_OPEN_CUSTOM_CHAT = 4;
    public static final int TAB_ACTION_MARK_CUSTOM_CHAT_READ = 5;

    private static final String PREFS_NAME = "fluffy_main_tabs_settings";
    private static final String KEY_OPTIONAL_ORDER = "optional_order";
    private static final String KEY_QUICK_DIALOGS = "quick_dialogs";
    private static final String KEY_QUICK_CONTACTS = "quick_contacts";
    private static final String KEY_QUICK_DIALOG_LABEL_PREFIX = "quick_dialog_label_";
    private static final String KEY_QUICK_DIALOG_ACTION_DOUBLE_TAP_PREFIX = "quick_dialog_action_double_tap_";
    private static final String KEY_QUICK_DIALOG_ACTION_LONG_PRESS_PREFIX = "quick_dialog_action_long_press_";
    private static final String KEY_OPEN_SAVED_MESSAGES_ON_DOUBLE_TAP = "open_saved_messages_on_double_tap";
    private static final String KEY_TAB_ACTION_DOUBLE_TAP_PREFIX = "tab_action_double_tap_";
    private static final String KEY_TAB_ACTION_LONG_PRESS_PREFIX = "tab_action_long_press_";
    private static final String KEY_TAB_ACTION_TARGET_DOUBLE_TAP_PREFIX = "tab_action_target_double_tap_";
    private static final String KEY_TAB_ACTION_TARGET_LONG_PRESS_PREFIX = "tab_action_target_long_press_";
    private static final String KEY_QUICK_DIALOG_ACTION_TARGET_DOUBLE_TAP_PREFIX = "quick_dialog_action_target_double_tap_";
    private static final String KEY_QUICK_DIALOG_ACTION_TARGET_LONG_PRESS_PREFIX = "quick_dialog_action_target_long_press_";
    private static final int[] DEFAULT_OPTIONAL_TYPES = new int[] {
            TAB_CONTACTS,
            TAB_SETTINGS,
            TAB_PROFILE
    };
    private static final int[] AVAILABLE_TYPES = new int[] {
            TAB_CHATS,
            TAB_CONTACTS,
            TAB_SETTINGS,
            TAB_PROFILE
    };

    private MainTabsConfigPatch() {
    }

    public static String exportSettingsJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_OPTIONAL_ORDER, serialize(getOptionalVisibleTypes()));
            JSONArray quickDialogs = new JSONArray();
            for (long dialogId : getQuickDialogIds()) {
                quickDialogs.put(dialogId);
            }
            object.put(KEY_QUICK_DIALOGS, quickDialogs);
            JSONObject labels = new JSONObject();
            for (long dialogId : getQuickDialogIds()) {
                String label = getQuickDialogCustomLabel(dialogId);
                if (!TextUtils.isEmpty(label)) {
                    labels.put(String.valueOf(dialogId), label);
                }
            }
            object.put("quick_dialog_labels", labels);
        } catch (Exception ignore) {
        }
        return object.toString();
    }

    public static void importSettingsJson(String json) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        try {
            JSONObject object = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_OPTIONAL_ORDER, sanitizeOptionalOrder(object.optString(KEY_OPTIONAL_ORDER, "")));

            JSONArray quickDialogs = object.optJSONArray(KEY_QUICK_DIALOGS);
            ArrayList<Long> dialogIds = new ArrayList<>();
            if (quickDialogs != null) {
                for (int i = 0; i < quickDialogs.length(); i++) {
                    long dialogId = quickDialogs.optLong(i, 0L);
                    if (dialogId != 0L && !dialogIds.contains(dialogId)) {
                        dialogIds.add(dialogId);
                    }
                }
            }
            editor.putString(KEY_QUICK_DIALOGS, serializeDialogIds(toLongArray(dialogIds)));
            editor.remove(KEY_QUICK_CONTACTS);

            for (String key : preferences.getAll().keySet()) {
                if (key != null && key.startsWith(KEY_QUICK_DIALOG_LABEL_PREFIX)) {
                    editor.remove(key);
                }
            }
            JSONObject labels = object.optJSONObject("quick_dialog_labels");
            if (labels != null) {
                for (long dialogId : toLongArray(dialogIds)) {
                    String label = trimLabel(labels.optString(String.valueOf(dialogId), ""));
                    if (!TextUtils.isEmpty(label)) {
                        editor.putString(getQuickDialogLabelKey(dialogId), label);
                    }
                }
            }
            editor.apply();
        } catch (Exception ignore) {
        }
    }

    public static int[] getVisibleTabTypes() {
        ArrayList<Integer> result = new ArrayList<>();
        result.add(TAB_CHATS);
        for (int type : getOptionalVisibleTypes()) {
            result.add(type);
        }
        return toIntArray(result);
    }

    public static int[] getOptionalVisibleTypes() {
        SharedPreferences preferences = getPreferences();
        String raw = preferences != null ? preferences.getString(KEY_OPTIONAL_ORDER, null) : null;
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT_OPTIONAL_TYPES.clone();
        }

        ArrayList<Integer> parsed = new ArrayList<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            int type = parseType(part.trim());
            if (isOptionalType(type) && !parsed.contains(type)) {
                parsed.add(type);
            }
        }
        if (parsed.isEmpty()) {
            return DEFAULT_OPTIONAL_TYPES.clone();
        }
        return toIntArray(parsed);
    }

    public static int[] getHiddenOptionalTypes() {
        ArrayList<Integer> hidden = new ArrayList<>();
        int[] visible = getOptionalVisibleTypes();
        for (int type : DEFAULT_OPTIONAL_TYPES) {
            if (indexOf(visible, type) < 0) {
                hidden.add(type);
            }
        }
        return toIntArray(hidden);
    }

    public static void moveOptionalTabUp(int type) {
        int[] visible = getOptionalVisibleTypes();
        int index = indexOf(visible, type);
        if (index > 0) {
            int previous = visible[index - 1];
            visible[index - 1] = visible[index];
            visible[index] = previous;
            saveOptionalVisibleTypes(visible);
        }
    }

    public static void moveOptionalTabDown(int type) {
        int[] visible = getOptionalVisibleTypes();
        int index = indexOf(visible, type);
        if (index >= 0 && index < visible.length - 1) {
            int next = visible[index + 1];
            visible[index + 1] = visible[index];
            visible[index] = next;
            saveOptionalVisibleTypes(visible);
        }
    }

    public static void showOptionalTab(int type) {
        if (!isOptionalType(type)) {
            return;
        }
        int[] visible = getOptionalVisibleTypes();
        if (indexOf(visible, type) >= 0) {
            return;
        }
        ArrayList<Integer> updated = new ArrayList<>();
        for (int item : visible) {
            updated.add(item);
        }
        updated.add(type);
        saveOptionalVisibleTypes(toIntArray(updated));
    }

    public static void hideOptionalTab(int type) {
        if (!isOptionalType(type)) {
            return;
        }
        int[] visible = getOptionalVisibleTypes();
        if (visible.length <= 1) {
            return;
        }
        ArrayList<Integer> updated = new ArrayList<>();
        for (int item : visible) {
            if (item != type) {
                updated.add(item);
            }
        }
        if (!updated.isEmpty()) {
            saveOptionalVisibleTypes(toIntArray(updated));
        }
    }

    public static boolean isOptionalTabVisible(int type) {
        return indexOf(getOptionalVisibleTypes(), type) >= 0;
    }

    public static boolean canHideOptionalTab(int type) {
        return isOptionalType(type) && getOptionalVisibleTypes().length > 1 && isOptionalTabVisible(type);
    }

    public static String getConfigSignature() {
        return serialize(getOptionalVisibleTypes())
                + "|"
                + serializeDialogIds(getQuickDialogIds())
                + "|"
                + serializeDialogLabels(getQuickDialogIds());
    }

    public static boolean isOpenSavedMessagesOnDoubleTapEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_OPEN_SAVED_MESSAGES_ON_DOUBLE_TAP, false);
    }

    public static void setOpenSavedMessagesOnDoubleTapEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_OPEN_SAVED_MESSAGES_ON_DOUBLE_TAP, enabled).apply();
    }

    public static int getDoubleTapAction(int tabType) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return getDefaultDoubleTapAction(tabType);
        }
        String key = getTabActionKey(KEY_TAB_ACTION_DOUBLE_TAP_PREFIX, tabType);
        if (!preferences.contains(key)) {
            if (tabType == TAB_CHATS && preferences.contains(KEY_OPEN_SAVED_MESSAGES_ON_DOUBLE_TAP)) {
                return preferences.getBoolean(KEY_OPEN_SAVED_MESSAGES_ON_DOUBLE_TAP, false)
                        ? TAB_ACTION_OPEN_SAVED_MESSAGES
                        : TAB_ACTION_NONE;
            }
            return getDefaultDoubleTapAction(tabType);
        }
        return sanitizeTabAction(preferences.getInt(key, getDefaultDoubleTapAction(tabType)));
    }

    public static void setDoubleTapAction(int tabType, int action) {
        setTabAction(KEY_TAB_ACTION_DOUBLE_TAP_PREFIX, tabType, action);
    }

    public static int getLongPressAction(int tabType) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return getDefaultLongPressAction(tabType);
        }
        return sanitizeTabAction(preferences.getInt(
                getTabActionKey(KEY_TAB_ACTION_LONG_PRESS_PREFIX, tabType),
                getDefaultLongPressAction(tabType)
        ));
    }

    public static void setLongPressAction(int tabType, int action) {
        setTabAction(KEY_TAB_ACTION_LONG_PRESS_PREFIX, tabType, action);
    }

    public static long getDoubleTapTargetDialogId(int tabType) {
        return getTabActionTargetDialogId(KEY_TAB_ACTION_TARGET_DOUBLE_TAP_PREFIX, tabType);
    }

    public static long getLongPressTargetDialogId(int tabType) {
        return getTabActionTargetDialogId(KEY_TAB_ACTION_TARGET_LONG_PRESS_PREFIX, tabType);
    }

    public static void setDoubleTapTargetDialogId(int tabType, long dialogId) {
        setTabActionTargetDialogId(KEY_TAB_ACTION_TARGET_DOUBLE_TAP_PREFIX, tabType, dialogId);
    }

    public static void setLongPressTargetDialogId(int tabType, long dialogId) {
        setTabActionTargetDialogId(KEY_TAB_ACTION_TARGET_LONG_PRESS_PREFIX, tabType, dialogId);
    }

    public static long[] getQuickDialogIds() {
        SharedPreferences preferences = getPreferences();
        String raw = null;
        if (preferences != null) {
            raw = preferences.getString(KEY_QUICK_DIALOGS, null);
            if (raw == null || raw.trim().isEmpty()) {
                raw = preferences.getString(KEY_QUICK_CONTACTS, null);
            }
        }
        if (raw == null || raw.trim().isEmpty()) {
            return new long[0];
        }

        ArrayList<Long> parsed = new ArrayList<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            long dialogId = parseDialogId(part.trim());
            if (dialogId != 0 && !parsed.contains(dialogId)) {
                parsed.add(dialogId);
            }
        }
        return toLongArray(parsed);
    }

    public static void addQuickDialog(long dialogId) {
        if (dialogId == 0) {
            return;
        }
        long[] quickDialogs = getQuickDialogIds();
        if (indexOf(quickDialogs, dialogId) >= 0) {
            return;
        }
        ArrayList<Long> updated = new ArrayList<>();
        for (long item : quickDialogs) {
            updated.add(item);
        }
        updated.add(dialogId);
        saveQuickDialogIds(toLongArray(updated));
    }

    public static void removeQuickDialog(long dialogId) {
        long[] quickDialogs = getQuickDialogIds();
        if (indexOf(quickDialogs, dialogId) < 0) {
            return;
        }
        ArrayList<Long> updated = new ArrayList<>();
        for (long item : quickDialogs) {
            if (item != dialogId) {
                updated.add(item);
            }
        }
        clearQuickDialogCustomLabel(dialogId);
        clearQuickDialogDoubleTapAction(dialogId);
        clearQuickDialogLongPressAction(dialogId);
        clearQuickDialogDoubleTapTargetDialogId(dialogId);
        clearQuickDialogLongPressTargetDialogId(dialogId);
        saveQuickDialogIds(toLongArray(updated));
    }

    public static void moveQuickDialogUp(long dialogId) {
        long[] quickDialogs = getQuickDialogIds();
        int index = indexOf(quickDialogs, dialogId);
        if (index > 0) {
            long previous = quickDialogs[index - 1];
            quickDialogs[index - 1] = quickDialogs[index];
            quickDialogs[index] = previous;
            saveQuickDialogIds(quickDialogs);
        }
    }

    public static void moveQuickDialogDown(long dialogId) {
        long[] quickDialogs = getQuickDialogIds();
        int index = indexOf(quickDialogs, dialogId);
        if (index >= 0 && index < quickDialogs.length - 1) {
            long next = quickDialogs[index + 1];
            quickDialogs[index + 1] = quickDialogs[index];
            quickDialogs[index] = next;
            saveQuickDialogIds(quickDialogs);
        }
    }

    public static int getTabTypeAtPosition(int[] visibleTypes, int position) {
        if (visibleTypes == null || position < 0 || position >= visibleTypes.length) {
            return TAB_CHATS;
        }
        return visibleTypes[position];
    }

    public static int getPositionForType(int[] visibleTypes, int type) {
        return indexOf(visibleTypes, type);
    }

    public static int findTypeByFragmentClassName(String className) {
        if (className == null) {
            return -1;
        }
        if (className.endsWith(".DialogsActivity")) {
            return TAB_CHATS;
        }
        if (className.endsWith(".ContactsActivity")) {
            return TAB_CONTACTS;
        }
        if (className.endsWith(".SettingsActivity") || className.endsWith(".CallLogActivity")) {
            return TAB_SETTINGS;
        }
        if (className.endsWith(".ProfileActivity")) {
            return TAB_PROFILE;
        }
        return -1;
    }

    public static int[] getAvailableTypes() {
        return AVAILABLE_TYPES.clone();
    }

    public static String getQuickDialogCustomLabel(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0) {
            return null;
        }
        String value = preferences.getString(getQuickDialogLabelKey(dialogId), null);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.isEmpty() ? null : value;
    }

    public static void setQuickDialogCustomLabel(long dialogId, String label) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0) {
            return;
        }
        String key = getQuickDialogLabelKey(dialogId);
        String normalized = label == null ? "" : label.trim();
        if (normalized.isEmpty()) {
            preferences.edit().remove(key).apply();
        } else {
            preferences.edit().putString(key, normalized).apply();
        }
    }

    public static int getQuickDialogLongPressAction(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return TAB_ACTION_NONE;
        }
        return sanitizeTabAction(preferences.getInt(getQuickDialogLongPressActionKey(dialogId), TAB_ACTION_NONE));
    }

    public static int getQuickDialogDoubleTapAction(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return TAB_ACTION_NONE;
        }
        return sanitizeTabAction(preferences.getInt(getQuickDialogDoubleTapActionKey(dialogId), TAB_ACTION_NONE));
    }

    public static long getQuickDialogLongPressTargetDialogId(long dialogId) {
        return getQuickDialogActionTargetDialogId(KEY_QUICK_DIALOG_ACTION_TARGET_LONG_PRESS_PREFIX, dialogId);
    }

    public static long getQuickDialogDoubleTapTargetDialogId(long dialogId) {
        return getQuickDialogActionTargetDialogId(KEY_QUICK_DIALOG_ACTION_TARGET_DOUBLE_TAP_PREFIX, dialogId);
    }

    public static void setQuickDialogLongPressAction(long dialogId, int action) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return;
        }
        preferences.edit().putInt(getQuickDialogLongPressActionKey(dialogId), sanitizeTabAction(action)).apply();
    }

    public static void setQuickDialogDoubleTapAction(long dialogId, int action) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return;
        }
        preferences.edit().putInt(getQuickDialogDoubleTapActionKey(dialogId), sanitizeTabAction(action)).apply();
    }

    public static void setQuickDialogLongPressTargetDialogId(long dialogId, long targetDialogId) {
        setQuickDialogActionTargetDialogId(KEY_QUICK_DIALOG_ACTION_TARGET_LONG_PRESS_PREFIX, dialogId, targetDialogId);
    }

    public static void setQuickDialogDoubleTapTargetDialogId(long dialogId, long targetDialogId) {
        setQuickDialogActionTargetDialogId(KEY_QUICK_DIALOG_ACTION_TARGET_DOUBLE_TAP_PREFIX, dialogId, targetDialogId);
    }

    private static void saveQuickDialogIds(long[] dialogIds) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit()
                .putString(KEY_QUICK_DIALOGS, serializeDialogIds(dialogIds))
                .remove(KEY_QUICK_CONTACTS)
                .apply();
    }

    private static void saveOptionalVisibleTypes(int[] types) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putString(KEY_OPTIONAL_ORDER, serialize(types)).apply();
    }

    private static void setTabAction(String prefix, int tabType, int action) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putInt(getTabActionKey(prefix, tabType), sanitizeTabAction(action)).apply();
    }

    private static long getTabActionTargetDialogId(String prefix, int tabType) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return 0L;
        }
        return sanitizeTargetDialogId(preferences.getLong(getTabActionKey(prefix, tabType), 0L));
    }

    private static void setTabActionTargetDialogId(String prefix, int tabType, long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        String key = getTabActionKey(prefix, tabType);
        SharedPreferences.Editor editor = preferences.edit();
        long normalizedDialogId = sanitizeTargetDialogId(dialogId);
        if (normalizedDialogId == 0L) {
            editor.remove(key);
        } else {
            editor.putLong(key, normalizedDialogId);
        }
        editor.apply();
    }

    private static boolean isOptionalType(int type) {
        return type == TAB_CONTACTS || type == TAB_SETTINGS || type == TAB_PROFILE;
    }

    private static String getTabActionKey(String prefix, int tabType) {
        return prefix + getTabName(tabType);
    }

    private static String getTabName(int tabType) {
        switch (tabType) {
            case TAB_CONTACTS:
                return "contacts";
            case TAB_SETTINGS:
                return "settings";
            case TAB_PROFILE:
                return "profile";
            case TAB_CHATS:
            default:
                return "chats";
        }
    }

    private static int getDefaultDoubleTapAction(int tabType) {
        return TAB_ACTION_NONE;
    }

    private static int getDefaultLongPressAction(int tabType) {
        if (tabType == TAB_SETTINGS) {
            return TAB_ACTION_OPEN_TABS_MENU;
        }
        if (tabType == TAB_PROFILE) {
            return TAB_ACTION_OPEN_ACCOUNT_SELECTOR;
        }
        return TAB_ACTION_NONE;
    }

    private static int sanitizeTabAction(int action) {
        if (action < TAB_ACTION_NONE || action > TAB_ACTION_MARK_CUSTOM_CHAT_READ) {
            return TAB_ACTION_NONE;
        }
        return action;
    }

    private static long getQuickDialogActionTargetDialogId(String prefix, long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return 0L;
        }
        return sanitizeTargetDialogId(preferences.getLong(prefix + dialogId, 0L));
    }

    private static void setQuickDialogActionTargetDialogId(String prefix, long dialogId, long targetDialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return;
        }
        String key = prefix + dialogId;
        SharedPreferences.Editor editor = preferences.edit();
        long normalizedDialogId = sanitizeTargetDialogId(targetDialogId);
        if (normalizedDialogId == 0L) {
            editor.remove(key);
        } else {
            editor.putLong(key, normalizedDialogId);
        }
        editor.apply();
    }

    private static long sanitizeTargetDialogId(long dialogId) {
        if (dialogId == 0L || DialogObject.isEncryptedDialog(dialogId)) {
            return 0L;
        }
        if (DialogObject.isUserDialog(dialogId) || DialogObject.isChatDialog(dialogId)) {
            return dialogId;
        }
        return 0L;
    }

    private static int parseType(String raw) {
        if ("contacts".equals(raw)) {
            return TAB_CONTACTS;
        }
        if ("settings".equals(raw)) {
            return TAB_SETTINGS;
        }
        if ("profile".equals(raw)) {
            return TAB_PROFILE;
        }
        if ("chats".equals(raw)) {
            return TAB_CHATS;
        }
        return -1;
    }

    private static String serialize(int[] types) {
        List<String> names = new ArrayList<>();
        for (int type : types) {
            switch (type) {
                case TAB_CONTACTS:
                    names.add("contacts");
                    break;
                case TAB_SETTINGS:
                    names.add("settings");
                    break;
                case TAB_PROFILE:
                    names.add("profile");
                    break;
                case TAB_CHATS:
                    names.add("chats");
                    break;
                default:
                    break;
            }
        }
        return String.join(",", names);
    }

    private static String sanitizeOptionalOrder(String raw) {
        if (TextUtils.isEmpty(raw)) {
            return serialize(DEFAULT_OPTIONAL_TYPES);
        }
        ArrayList<Integer> parsed = new ArrayList<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            int type = parseType(part.trim());
            if (isOptionalType(type) && !parsed.contains(type)) {
                parsed.add(type);
            }
        }
        if (parsed.isEmpty()) {
            return serialize(DEFAULT_OPTIONAL_TYPES);
        }
        return serialize(toIntArray(parsed));
    }

    private static String serializeDialogIds(long[] dialogIds) {
        List<String> ids = new ArrayList<>();
        for (long dialogId : dialogIds) {
            if (dialogId != 0) {
                ids.add(String.valueOf(dialogId));
            }
        }
        return String.join(",", ids);
    }

    private static String trimLabel(String label) {
        String value = label != null ? label.trim() : "";
        if (value.length() > 32) {
            value = value.substring(0, 32);
        }
        return value;
    }

    private static String serializeDialogLabels(long[] dialogIds) {
        List<String> values = new ArrayList<>();
        for (long dialogId : dialogIds) {
            String label = getQuickDialogCustomLabel(dialogId);
            if (label != null) {
                values.add(dialogId + "=" + label);
            }
        }
        return String.join(",", values);
    }

    private static long parseDialogId(String raw) {
        if (raw == null || raw.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignore) {
            return 0;
        }
    }

    public static long[] getQuickContactUserIds() {
        return getQuickDialogIds();
    }

    public static void addQuickContact(long userId) {
        addQuickDialog(userId);
    }

    public static void removeQuickContact(long userId) {
        removeQuickDialog(userId);
    }

    public static void moveQuickContactUp(long userId) {
        moveQuickDialogUp(userId);
    }

    public static void moveQuickContactDown(long userId) {
        moveQuickDialogDown(userId);
    }

    private static void clearQuickDialogCustomLabel(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0) {
            return;
        }
        preferences.edit().remove(getQuickDialogLabelKey(dialogId)).apply();
    }

    private static void clearQuickDialogLongPressAction(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return;
        }
        preferences.edit().remove(getQuickDialogLongPressActionKey(dialogId)).apply();
    }

    private static void clearQuickDialogDoubleTapAction(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return;
        }
        preferences.edit().remove(getQuickDialogDoubleTapActionKey(dialogId)).apply();
    }

    private static void clearQuickDialogLongPressTargetDialogId(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return;
        }
        preferences.edit().remove(KEY_QUICK_DIALOG_ACTION_TARGET_LONG_PRESS_PREFIX + dialogId).apply();
    }

    private static void clearQuickDialogDoubleTapTargetDialogId(long dialogId) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null || dialogId == 0L) {
            return;
        }
        preferences.edit().remove(KEY_QUICK_DIALOG_ACTION_TARGET_DOUBLE_TAP_PREFIX + dialogId).apply();
    }

    private static String getQuickDialogLabelKey(long dialogId) {
        return KEY_QUICK_DIALOG_LABEL_PREFIX + dialogId;
    }

    private static String getQuickDialogLongPressActionKey(long dialogId) {
        return KEY_QUICK_DIALOG_ACTION_LONG_PRESS_PREFIX + dialogId;
    }

    private static String getQuickDialogDoubleTapActionKey(long dialogId) {
        return KEY_QUICK_DIALOG_ACTION_DOUBLE_TAP_PREFIX + dialogId;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] result = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static long[] toLongArray(List<Long> values) {
        long[] result = new long[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }

    private static int indexOf(int[] values, int value) {
        if (values == null) {
            return -1;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static int indexOf(long[] values, long value) {
        if (values == null) {
            return -1;
        }
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }
}
