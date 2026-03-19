package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.List;

public final class MainTabsConfigPatch {
    public static final int TAB_CHATS = 0;
    public static final int TAB_CONTACTS = 1;
    public static final int TAB_SETTINGS = 2;
    public static final int TAB_PROFILE = 3;

    private static final String PREFS_NAME = "fluffy_main_tabs_settings";
    private static final String KEY_OPTIONAL_ORDER = "optional_order";
    private static final String KEY_QUICK_DIALOGS = "quick_dialogs";
    private static final String KEY_QUICK_CONTACTS = "quick_contacts";
    private static final String KEY_QUICK_DIALOG_LABEL_PREFIX = "quick_dialog_label_";
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

    private static boolean isOptionalType(int type) {
        return type == TAB_CONTACTS || type == TAB_SETTINGS || type == TAB_PROFILE;
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

    private static String getQuickDialogLabelKey(long dialogId) {
        return KEY_QUICK_DIALOG_LABEL_PREFIX + dialogId;
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
