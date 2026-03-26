package org.ushastoe.fluffy.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;

import java.io.File;
import java.util.ArrayList;

public final class LocalMessageFakeEditStore {

    private static final String DATABASE_NAME = "fluffy_local_fake_edit.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_RECORDS = "fake_edit_records";
    private static final String TABLE_META = "fake_edit_meta";

    private static final String COL_DIALOG_ID = "dialog_id";
    private static final String COL_MESSAGE_ID = "message_id";
    private static final String COL_ORIGINAL_TEXT = "original_text";
    private static final String COL_ORIGINAL_FLAGS = "original_flags";
    private static final String COL_ORIGINAL_EDIT_DATE = "original_edit_date";
    private static final String COL_ORIGINAL_EDIT_HIDE = "original_edit_hide";
    private static final String COL_ORIGINAL_ENTITIES = "original_entities";
    private static final String COL_FAKE_TEXT = "fake_text";
    private static final String COL_FAKE_EDIT_DATE = "fake_edit_date";

    private static final String META_KEY = "meta_key";
    private static final String META_VALUE = "meta_value";
    private static final String META_LEGACY_MIGRATED = "legacy_shared_prefs_migrated_v1";

    private static final String LEGACY_PREFS_NAME = "fluffy_fake_edit_storage";
    private static final String LEGACY_KEY_RECORDS = "local_message_fake_edit_records_v1";

    private static final String FIELD_ORIGINAL_TEXT = "original_text";
    private static final String FIELD_ORIGINAL_FLAGS = "original_flags";
    private static final String FIELD_ORIGINAL_EDIT_DATE = "original_edit_date";
    private static final String FIELD_ORIGINAL_EDIT_HIDE = "original_edit_hide";
    private static final String FIELD_ORIGINAL_ENTITIES = "original_entities";
    private static final String FIELD_FAKE_TEXT = "fake_text";
    private static final String FIELD_FAKE_EDIT_DATE = "fake_edit_date";

    private static final Object LOCK = new Object();

    private static FakeEditOpenHelper helper;

    private LocalMessageFakeEditStore() {
    }

    public static Record get(long dialogId, int messageId) {
        if (messageId == 0) {
            return null;
        }
        synchronized (LOCK) {
            SQLiteDatabase database = getDatabaseLocked();
            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_RECORDS, null,
                        COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ?",
                        new String[]{String.valueOf(dialogId), String.valueOf(messageId)},
                        null, null, null, "1");
                if (!cursor.moveToFirst()) {
                    return null;
                }
                Record record = new Record();
                record.originalText = cursor.getString(cursor.getColumnIndexOrThrow(COL_ORIGINAL_TEXT));
                record.originalFlags = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ORIGINAL_FLAGS));
                record.originalEditDate = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ORIGINAL_EDIT_DATE));
                record.originalEditHide = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ORIGINAL_EDIT_HIDE)) != 0;
                record.originalEntities = deserializeEntities(cursor.getString(cursor.getColumnIndexOrThrow(COL_ORIGINAL_ENTITIES)));
                record.fakeText = cursor.getString(cursor.getColumnIndexOrThrow(COL_FAKE_TEXT));
                record.fakeEditDate = cursor.getInt(cursor.getColumnIndexOrThrow(COL_FAKE_EDIT_DATE));
                return record;
            } finally {
                closeCursor(cursor);
            }
        }
    }

    public static void put(long dialogId, int messageId, Record record) {
        if (record == null || messageId == 0) {
            return;
        }
        synchronized (LOCK) {
            SQLiteDatabase database = getDatabaseLocked();
            ContentValues values = new ContentValues();
            values.put(COL_DIALOG_ID, dialogId);
            values.put(COL_MESSAGE_ID, messageId);
            values.put(COL_ORIGINAL_TEXT, valueOrEmpty(record.originalText));
            values.put(COL_ORIGINAL_FLAGS, record.originalFlags);
            values.put(COL_ORIGINAL_EDIT_DATE, record.originalEditDate);
            values.put(COL_ORIGINAL_EDIT_HIDE, record.originalEditHide ? 1 : 0);
            values.put(COL_ORIGINAL_ENTITIES, serializeEntities(record.originalEntities));
            values.put(COL_FAKE_TEXT, valueOrEmpty(record.fakeText));
            values.put(COL_FAKE_EDIT_DATE, record.fakeEditDate);
            database.insertWithOnConflict(TABLE_RECORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public static void remove(long dialogId, int messageId) {
        if (messageId == 0) {
            return;
        }
        synchronized (LOCK) {
            SQLiteDatabase database = getDatabaseLocked();
            database.delete(TABLE_RECORDS,
                    COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
        }
    }

    public static long getDatabaseSizeBytes() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return 0L;
        }
        File file = context.getDatabasePath(DATABASE_NAME);
        return file != null && file.isFile() ? file.length() : 0L;
    }

    private static String valueOrEmpty(String value) {
        return value != null ? value : "";
    }

    private static SQLiteDatabase getDatabaseLocked() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            throw new IllegalStateException("Application context is not available");
        }
        if (helper == null) {
            helper = new FakeEditOpenHelper(context.getApplicationContext());
        }
        SQLiteDatabase database = helper.getWritableDatabase();
        migrateLegacyPrefsIfNeededLocked(database);
        return database;
    }

    private static void migrateLegacyPrefsIfNeededLocked(SQLiteDatabase database) {
        if (getMetaValueLocked(database, META_LEGACY_MIGRATED) != null) {
            return;
        }
        SharedPreferences preferences = getLegacyPreferences();
        String raw = preferences != null ? preferences.getString(LEGACY_KEY_RECORDS, null) : null;
        if (TextUtils.isEmpty(raw)) {
            putMetaValueLocked(database, META_LEGACY_MIGRATED, "1");
            return;
        }

        try {
            JSONObject records = new JSONObject(raw);
            database.beginTransaction();
            try {
                for (java.util.Iterator<String> it = records.keys(); it.hasNext();) {
                    String recordKey = it.next();
                    if (TextUtils.isEmpty(recordKey)) {
                        continue;
                    }
                    JSONObject object = records.optJSONObject(recordKey);
                    Record record = Record.fromJson(object);
                    if (record == null) {
                        continue;
                    }
                    long dialogId = parseDialogId(recordKey);
                    int messageId = parseMessageId(recordKey);
                    if (messageId == 0) {
                        continue;
                    }
                    ContentValues values = new ContentValues();
                    values.put(COL_DIALOG_ID, dialogId);
                    values.put(COL_MESSAGE_ID, messageId);
                    values.put(COL_ORIGINAL_TEXT, valueOrEmpty(record.originalText));
                    values.put(COL_ORIGINAL_FLAGS, record.originalFlags);
                    values.put(COL_ORIGINAL_EDIT_DATE, record.originalEditDate);
                    values.put(COL_ORIGINAL_EDIT_HIDE, record.originalEditHide ? 1 : 0);
                    values.put(COL_ORIGINAL_ENTITIES, serializeEntities(record.originalEntities));
                    values.put(COL_FAKE_TEXT, valueOrEmpty(record.fakeText));
                    values.put(COL_FAKE_EDIT_DATE, record.fakeEditDate);
                    database.insertWithOnConflict(TABLE_RECORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
                putMetaValueLocked(database, META_LEGACY_MIGRATED, "1");
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
            preferences.edit().remove(LEGACY_KEY_RECORDS).apply();
        } catch (Exception ignore) {
            putMetaValueLocked(database, META_LEGACY_MIGRATED, "1");
        }
    }

    private static long parseDialogId(String recordKey) {
        int separator = recordKey.lastIndexOf('_');
        if (separator <= 0) {
            return 0L;
        }
        try {
            return Long.parseLong(recordKey.substring(0, separator));
        } catch (Exception ignore) {
            return 0L;
        }
    }

    private static int parseMessageId(String recordKey) {
        int separator = recordKey.lastIndexOf('_');
        if (separator <= 0 || separator >= recordKey.length() - 1) {
            return 0;
        }
        try {
            return Integer.parseInt(recordKey.substring(separator + 1));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private static String getMetaValueLocked(SQLiteDatabase database, String key) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_META, new String[]{META_VALUE},
                    META_KEY + " = ?", new String[]{key},
                    null, null, null, "1");
            if (!cursor.moveToFirst()) {
                return null;
            }
            return cursor.getString(0);
        } finally {
            closeCursor(cursor);
        }
    }

    private static void putMetaValueLocked(SQLiteDatabase database, String key, String value) {
        ContentValues values = new ContentValues();
        values.put(META_KEY, key);
        values.put(META_VALUE, value);
        database.insertWithOnConflict(TABLE_META, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static SharedPreferences getLegacyPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }

    private static String serializeEntities(ArrayList<TLRPC.MessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return null;
        }
        try {
            SerializedData data = new SerializedData();
            Vector.serialize(data, entities);
            byte[] bytes = data.toByteArray();
            data.cleanup();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception ignore) {
            return null;
        }
    }

    private static ArrayList<TLRPC.MessageEntity> deserializeEntities(String encoded) {
        if (TextUtils.isEmpty(encoded)) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            SerializedData data = new SerializedData(bytes);
            ArrayList<TLRPC.MessageEntity> entities = Vector.deserialize(data, TLRPC.MessageEntity::TLdeserialize, false);
            data.cleanup();
            return entities == null || entities.isEmpty() ? null : entities;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static final class Record {
        public String originalText = "";
        public int originalFlags;
        public int originalEditDate;
        public boolean originalEditHide;
        public ArrayList<TLRPC.MessageEntity> originalEntities;
        public String fakeText = "";
        public int fakeEditDate;

        private static Record fromJson(JSONObject object) {
            if (object == null) {
                return null;
            }
            Record record = new Record();
            record.originalText = object.optString(FIELD_ORIGINAL_TEXT, "");
            record.originalFlags = object.optInt(FIELD_ORIGINAL_FLAGS, 0);
            record.originalEditDate = object.optInt(FIELD_ORIGINAL_EDIT_DATE, 0);
            record.originalEditHide = object.optBoolean(FIELD_ORIGINAL_EDIT_HIDE, false);
            record.originalEntities = deserializeEntities(object.optString(FIELD_ORIGINAL_ENTITIES, null));
            record.fakeText = object.optString(FIELD_FAKE_TEXT, "");
            record.fakeEditDate = object.optInt(FIELD_FAKE_EDIT_DATE, 0);
            return record;
        }
    }

    private static final class FakeEditOpenHelper extends SQLiteOpenHelper {

        FakeEditOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_RECORDS + " (" +
                    COL_DIALOG_ID + " INTEGER NOT NULL, " +
                    COL_MESSAGE_ID + " INTEGER NOT NULL, " +
                    COL_ORIGINAL_TEXT + " TEXT NOT NULL, " +
                    COL_ORIGINAL_FLAGS + " INTEGER NOT NULL DEFAULT 0, " +
                    COL_ORIGINAL_EDIT_DATE + " INTEGER NOT NULL DEFAULT 0, " +
                    COL_ORIGINAL_EDIT_HIDE + " INTEGER NOT NULL DEFAULT 0, " +
                    COL_ORIGINAL_ENTITIES + " TEXT, " +
                    COL_FAKE_TEXT + " TEXT NOT NULL, " +
                    COL_FAKE_EDIT_DATE + " INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (" + COL_DIALOG_ID + ", " + COL_MESSAGE_ID + "))");
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_META + " (" +
                    META_KEY + " TEXT PRIMARY KEY, " +
                    META_VALUE + " TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onCreate(db);
        }
    }
}
