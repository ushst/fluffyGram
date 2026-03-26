package org.ushastoe.fluffy.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;

import java.io.File;
import java.util.ArrayList;

public final class LocalMessageArchiveStore {

    private static final String DATABASE_NAME = "fluffy_local_message_archive.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ENTRIES = "message_archive_entries";
    private static final String TABLE_META = "message_archive_meta";

    private static final String COL_SEQ = "seq";
    private static final String COL_DIALOG_ID = "dialog_id";
    private static final String COL_MESSAGE_ID = "message_id";
    private static final String COL_TEXT = "text";
    private static final String COL_SAVED_AT = "saved_at";
    private static final String COL_SOURCE = "source";

    private static final String META_KEY = "meta_key";
    private static final String META_VALUE = "meta_value";
    private static final String META_LEGACY_MIGRATED = "legacy_shared_prefs_migrated_v1";

    private static final String LEGACY_PREFS_NAME = "fluffy_message_archive_storage";
    private static final String LEGACY_KEY_RECORDS = "local_message_archive_records_v1";

    private static final String FIELD_HISTORY = "history";
    private static final String FIELD_SAVED_AT = "saved_at";
    private static final String FIELD_SOURCE = "source";
    private static final String FIELD_TEXT = "text";

    public static final String SOURCE_SERVER_EDIT = "server_edit";
    public static final String SOURCE_LOCAL_EDIT = "local_edit";
    public static final String SOURCE_LOCAL_EDIT_RESET = "local_edit_reset";
    public static final String SOURCE_DELETED = "deleted";

    private static final int MAX_ENTRIES_PER_MESSAGE = 25;
    private static final int MAX_MESSAGE_RECORDS = 500;
    private static final Object LOCK = new Object();

    private static ArchiveOpenHelper helper;

    private LocalMessageArchiveStore() {
    }

    public static boolean hasRecords(long dialogId, int messageId) {
        if (messageId == 0) {
            return false;
        }
        synchronized (LOCK) {
            SQLiteDatabase database = getDatabaseLocked();
            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_ENTRIES, new String[]{COL_SEQ},
                        COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ?",
                        new String[]{String.valueOf(dialogId), String.valueOf(messageId)},
                        null, null, COL_SEQ + " DESC", "1");
                return cursor.moveToFirst();
            } finally {
                closeCursor(cursor);
            }
        }
    }

    public static ArrayList<Entry> getHistory(long dialogId, int messageId) {
        ArrayList<Entry> entries = new ArrayList<>();
        if (messageId == 0) {
            return entries;
        }
        synchronized (LOCK) {
            SQLiteDatabase database = getDatabaseLocked();
            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_ENTRIES,
                        new String[]{COL_TEXT, COL_SAVED_AT, COL_SOURCE},
                        COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ?",
                        new String[]{String.valueOf(dialogId), String.valueOf(messageId)},
                        null, null, COL_SEQ + " DESC");
                while (cursor.moveToNext()) {
                    Entry entry = new Entry();
                    entry.text = cursor.getString(0);
                    entry.savedAt = cursor.getInt(1);
                    entry.source = cursor.getString(2);
                    entries.add(entry);
                }
            } finally {
                closeCursor(cursor);
            }
        }
        return entries;
    }

    public static boolean hasDeletedSnapshot(long dialogId, int messageId) {
        if (messageId == 0) {
            return false;
        }
        synchronized (LOCK) {
            SQLiteDatabase database = getDatabaseLocked();
            Cursor cursor = null;
            try {
                cursor = database.query(TABLE_ENTRIES, new String[]{COL_SEQ},
                        COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ? AND " + COL_SOURCE + " = ?",
                        new String[]{String.valueOf(dialogId), String.valueOf(messageId), SOURCE_DELETED},
                        null, null, COL_SEQ + " DESC", "1");
                return cursor.moveToFirst();
            } finally {
                closeCursor(cursor);
            }
        }
    }

    public static void appendSnapshot(long dialogId, int messageId, String text, int savedAt, String source) {
        if (messageId == 0 || TextUtils.isEmpty(text) || TextUtils.isEmpty(source)) {
            return;
        }
        synchronized (LOCK) {
            SQLiteDatabase database = getDatabaseLocked();
            Entry lastEntry = getLastEntryLocked(database, dialogId, messageId);
            if (lastEntry != null && TextUtils.equals(lastEntry.text, text) && TextUtils.equals(lastEntry.source, source)) {
                return;
            }

            database.beginTransaction();
            try {
                ContentValues values = new ContentValues();
                values.put(COL_DIALOG_ID, dialogId);
                values.put(COL_MESSAGE_ID, messageId);
                values.put(COL_TEXT, text);
                values.put(COL_SAVED_AT, savedAt);
                values.put(COL_SOURCE, source);
                database.insert(TABLE_ENTRIES, null, values);
                trimHistoryLocked(database, dialogId, messageId);
                trimRecordsLocked(database);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
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

    private static Entry getLastEntryLocked(SQLiteDatabase database, long dialogId, int messageId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_ENTRIES,
                    new String[]{COL_TEXT, COL_SAVED_AT, COL_SOURCE},
                    COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)},
                    null, null, COL_SEQ + " DESC", "1");
            if (!cursor.moveToFirst()) {
                return null;
            }
            Entry entry = new Entry();
            entry.text = cursor.getString(0);
            entry.savedAt = cursor.getInt(1);
            entry.source = cursor.getString(2);
            return entry;
        } finally {
            closeCursor(cursor);
        }
    }

    private static void trimHistoryLocked(SQLiteDatabase database, long dialogId, int messageId) {
        Cursor cursor = null;
        try {
            cursor = database.query(TABLE_ENTRIES, new String[]{COL_SEQ},
                    COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId)},
                    null, null, COL_SEQ + " DESC",
                    "1 OFFSET " + (MAX_ENTRIES_PER_MESSAGE - 1));
            if (!cursor.moveToFirst()) {
                return;
            }
            long minSeqToKeep = cursor.getLong(0);
            database.delete(TABLE_ENTRIES,
                    COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ? AND " + COL_SEQ + " < ?",
                    new String[]{String.valueOf(dialogId), String.valueOf(messageId), String.valueOf(minSeqToKeep)});
        } finally {
            closeCursor(cursor);
        }
    }

    private static void trimRecordsLocked(SQLiteDatabase database) {
        Cursor cursor = null;
        try {
            cursor = database.rawQuery(
                    "SELECT " + COL_DIALOG_ID + ", " + COL_MESSAGE_ID + ", MAX(" + COL_SAVED_AT + ") AS last_saved " +
                            "FROM " + TABLE_ENTRIES + " GROUP BY " + COL_DIALOG_ID + ", " + COL_MESSAGE_ID +
                            " ORDER BY last_saved ASC", null);
            int count = cursor.getCount();
            while (count > MAX_MESSAGE_RECORDS && cursor.moveToNext()) {
                long dialogId = cursor.getLong(0);
                int messageId = cursor.getInt(1);
                database.delete(TABLE_ENTRIES,
                        COL_DIALOG_ID + " = ? AND " + COL_MESSAGE_ID + " = ?",
                        new String[]{String.valueOf(dialogId), String.valueOf(messageId)});
                count--;
            }
        } finally {
            closeCursor(cursor);
        }
    }

    private static SQLiteDatabase getDatabaseLocked() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            throw new IllegalStateException("Application context is not available");
        }
        if (helper == null) {
            helper = new ArchiveOpenHelper(context.getApplicationContext());
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
                JSONArray names = records.names();
                if (names != null) {
                    for (int i = 0; i < names.length(); i++) {
                        String recordKey = names.optString(i, null);
                        if (TextUtils.isEmpty(recordKey)) {
                            continue;
                        }
                        long dialogId = parseDialogId(recordKey);
                        int messageId = parseMessageId(recordKey);
                        if (messageId == 0) {
                            continue;
                        }
                        JSONObject record = records.optJSONObject(recordKey);
                        JSONArray history = record != null ? record.optJSONArray(FIELD_HISTORY) : null;
                        if (history == null || history.length() == 0) {
                            continue;
                        }
                        for (int j = 0; j < history.length(); j++) {
                            JSONObject object = history.optJSONObject(j);
                            Entry entry = Entry.fromJson(object);
                            if (entry == null || TextUtils.isEmpty(entry.text) || TextUtils.isEmpty(entry.source)) {
                                continue;
                            }
                            ContentValues values = new ContentValues();
                            values.put(COL_DIALOG_ID, dialogId);
                            values.put(COL_MESSAGE_ID, messageId);
                            values.put(COL_TEXT, entry.text);
                            values.put(COL_SAVED_AT, entry.savedAt);
                            values.put(COL_SOURCE, entry.source);
                            database.insert(TABLE_ENTRIES, null, values);
                        }
                        trimHistoryLocked(database, dialogId, messageId);
                    }
                }
                trimRecordsLocked(database);
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

    public static final class Entry {
        public String text;
        public int savedAt;
        public String source;

        private static Entry fromJson(JSONObject object) {
            if (object == null) {
                return null;
            }
            Entry entry = new Entry();
            entry.text = object.optString(FIELD_TEXT, "");
            entry.savedAt = object.optInt(FIELD_SAVED_AT, 0);
            entry.source = object.optString(FIELD_SOURCE, "");
            return entry;
        }
    }

    private static final class ArchiveOpenHelper extends SQLiteOpenHelper {

        ArchiveOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_ENTRIES + " (" +
                    COL_SEQ + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_DIALOG_ID + " INTEGER NOT NULL, " +
                    COL_MESSAGE_ID + " INTEGER NOT NULL, " +
                    COL_TEXT + " TEXT NOT NULL, " +
                    COL_SAVED_AT + " INTEGER NOT NULL DEFAULT 0, " +
                    COL_SOURCE + " TEXT NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_archive_message ON " + TABLE_ENTRIES +
                    " (" + COL_DIALOG_ID + ", " + COL_MESSAGE_ID + ", " + COL_SEQ + " DESC)");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_message_archive_saved_at ON " + TABLE_ENTRIES +
                    " (" + COL_SAVED_AT + ")");
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
