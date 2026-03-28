package org.ushastoe.fluffy.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class FluffyConfigFileStore {
    private static final String FILE_PREFIX = "fluffy_";
    private static final String EXPORT_VERSION = "1";

    private FluffyConfigFileStore() {
    }

    public static String buildDefaultFileName() {
        return "fluffy-config-" + System.currentTimeMillis() + ".json";
    }

    public static boolean exportToUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return false;
        }
        try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri, "wt")) {
            if (outputStream == null) {
                return false;
            }
            outputStream.write(exportToJsonString(context.getApplicationContext()).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static boolean importFromUri(Context context, Uri uri) {
        if (context == null || uri == null) {
            return false;
        }
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                return false;
            }
            return importFromJsonString(context.getApplicationContext(), readAll(inputStream));
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static String exportToJsonString(Context context) throws Exception {
        if (context == null) {
            return "";
        }
        return buildExportJson(context.getApplicationContext()).toString(2);
    }

    public static boolean importFromJsonString(Context context, String json) {
        if (context == null || TextUtils.isEmpty(json)) {
            return false;
        }
        try {
            JSONObject root = new JSONObject(json);
            JSONArray files = root.optJSONArray("files");
            if (files == null) {
                return false;
            }
            Context appContext = context.getApplicationContext();
            for (int i = 0; i < files.length(); i++) {
                JSONObject fileObject = files.optJSONObject(i);
                if (fileObject == null) {
                    continue;
                }
                String storage = fileObject.optString("storage", "");
                if (TextUtils.isEmpty(storage) || !storage.startsWith(FILE_PREFIX)) {
                    continue;
                }
                JSONObject values = fileObject.optJSONObject("values");
                if (values == null) {
                    continue;
                }
                SharedPreferences preferences = appContext.getSharedPreferences(storage, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                Iterator<String> keys = values.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    Object value = values.opt(key);
                    putValue(editor, key, value);
                }
                editor.apply();
            }
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    private static JSONObject buildExportJson(Context context) throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", EXPORT_VERSION);
        root.put("app_id", ApplicationLoader.getApplicationId());
        root.put("exported_at", System.currentTimeMillis());
        JSONArray filesArray = new JSONArray();
        for (String storage : getFluffyPreferenceNames(context)) {
            SharedPreferences preferences = context.getSharedPreferences(storage, Context.MODE_PRIVATE);
            JSONObject fileObject = new JSONObject();
            fileObject.put("storage", storage);
            fileObject.put("values", toJson(preferences.getAll()));
            filesArray.put(fileObject);
        }
        root.put("files", filesArray);
        return root;
    }

    private static List<String> getFluffyPreferenceNames(Context context) {
        File sharedPrefsDir = new File(context.getApplicationInfo().dataDir, "shared_prefs");
        if (sharedPrefsDir == null || !sharedPrefsDir.isDirectory()) {
            return Collections.emptyList();
        }
        File[] files = sharedPrefsDir.listFiles((dir, name) -> name.startsWith(FILE_PREFIX) && name.endsWith(".xml"));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        ArrayList<String> names = new ArrayList<>(files.length);
        for (File file : files) {
            String name = file.getName();
            if (name.endsWith(".xml")) {
                names.add(name.substring(0, name.length() - 4));
            }
        }
        Collections.sort(names);
        return names;
    }

    private static JSONObject toJson(Map<String, ?> allValues) throws Exception {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, ?> entry : allValues.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (TextUtils.isEmpty(key) || value == null) {
                continue;
            }
            if (value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof String) {
                object.put(key, value);
            }
        }
        return object;
    }

    private static void putValue(SharedPreferences.Editor editor, String key, Object value) {
        if (editor == null || TextUtils.isEmpty(key) || value == null || value == JSONObject.NULL) {
            return;
        }
        if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Double) {
            editor.putFloat(key, ((Double) value).floatValue());
        } else if (value instanceof String) {
            editor.putString(key, (String) value);
        }
    }

    private static String readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toString(StandardCharsets.UTF_8.name());
    }
}
