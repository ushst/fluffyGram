package org.ushastoe.fluffy.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple storage for user status updates recorded by {@code UserStatusLoggerService}.
 */
public class UserStatusStorage {

    private static final String DATABASE_NAME = "user_status_logs.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_LOGS = "user_status_logs";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_ACCOUNT_ID = "account_id";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USER_NAME = "user_name";
    private static final String COLUMN_STATUS_TEXT = "status_text";
    private static final String COLUMN_STATUS_CLASS = "status_class";
    private static final String COLUMN_LAST_SEEN = "last_seen";
    private static final String COLUMN_STATUS_EXPIRES = "status_expires";
    private static final String COLUMN_IS_ONLINE = "is_online";
    private static final String COLUMN_ACTION_TEXT = "action_text";
    private static final String COLUMN_UPDATE_MASK = "update_mask";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    private static volatile UserStatusStorage instance;

    private final DatabaseHelper databaseHelper;

    private UserStatusStorage(Context context) {
        databaseHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public static UserStatusStorage getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (UserStatusStorage.class) {
                if (instance == null) {
                    instance = new UserStatusStorage(context);
                }
            }
        }
        return instance;
    }

    public void insertStatus(int accountId,
                              long userId,
                              @Nullable String userName,
                              @Nullable String statusText,
                              @Nullable String statusClass,
                              long lastSeenAt,
                              long statusExpiresAt,
                              boolean isOnline,
                              @Nullable String actionText,
                              int updateMask) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCOUNT_ID, accountId);
        values.put(COLUMN_USER_ID, userId);
        values.put(COLUMN_USER_NAME, userName);
        values.put(COLUMN_STATUS_TEXT, statusText);
        values.put(COLUMN_STATUS_CLASS, statusClass);
        if (lastSeenAt > 0) {
            values.put(COLUMN_LAST_SEEN, lastSeenAt);
        } else {
            values.putNull(COLUMN_LAST_SEEN);
        }
        if (statusExpiresAt > 0) {
            values.put(COLUMN_STATUS_EXPIRES, statusExpiresAt);
        } else {
            values.putNull(COLUMN_STATUS_EXPIRES);
        }
        values.put(COLUMN_IS_ONLINE, isOnline ? 1 : 0);
        values.put(COLUMN_ACTION_TEXT, actionText);
        values.put(COLUMN_UPDATE_MASK, updateMask);
        values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());

        db.insert(TABLE_LOGS, null, values);
    }

    public List<LogEntry> getLatestPerUser(int limit) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        List<LogEntry> result = new ArrayList<>();
        String query = "SELECT l." + COLUMN_ID + ", l." + COLUMN_ACCOUNT_ID + ", l." + COLUMN_USER_ID + ", l." + COLUMN_USER_NAME +
                ", l." + COLUMN_STATUS_TEXT + ", l." + COLUMN_STATUS_CLASS + ", l." + COLUMN_LAST_SEEN + ", l." + COLUMN_STATUS_EXPIRES +
                ", l." + COLUMN_IS_ONLINE + ", l." + COLUMN_ACTION_TEXT + ", l." + COLUMN_UPDATE_MASK + ", l." + COLUMN_UPDATED_AT +
                " FROM " + TABLE_LOGS + " l INNER JOIN (" +
                "SELECT " + COLUMN_ACCOUNT_ID + ", " + COLUMN_USER_ID + ", MAX(" + COLUMN_UPDATED_AT + ") AS " + COLUMN_UPDATED_AT +
                " FROM " + TABLE_LOGS + " GROUP BY " + COLUMN_ACCOUNT_ID + ", " + COLUMN_USER_ID +
                ") grouped ON grouped." + COLUMN_ACCOUNT_ID + " = l." + COLUMN_ACCOUNT_ID +
                " AND grouped." + COLUMN_USER_ID + " = l." + COLUMN_USER_ID +
                " AND grouped." + COLUMN_UPDATED_AT + " = l." + COLUMN_UPDATED_AT +
                " ORDER BY l." + COLUMN_UPDATED_AT + " DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(query, null);
        try {
            while (cursor.moveToNext()) {
                result.add(buildEntryFromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return Collections.unmodifiableList(result);
    }


    @Nullable
    public LogEntry getLatestForUser(long userId, int accountId) {
        List<LogEntry> history = getHistoryForUser(userId, accountId, 1);
        if (history.isEmpty()) {
            return null;
        }
        return history.get(0);
    }

    public List<LogEntry> getHistoryForUser(long userId, int accountId, int limit) {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        List<LogEntry> result = new ArrayList<>();
        String query = "SELECT " + COLUMN_ID + ", " + COLUMN_ACCOUNT_ID + ", " + COLUMN_USER_ID + ", " + COLUMN_USER_NAME +
                ", " + COLUMN_STATUS_TEXT + ", " + COLUMN_STATUS_CLASS + ", " + COLUMN_LAST_SEEN + ", " + COLUMN_STATUS_EXPIRES +
                ", " + COLUMN_IS_ONLINE + ", " + COLUMN_ACTION_TEXT + ", " + COLUMN_UPDATE_MASK + ", " + COLUMN_UPDATED_AT +
                " FROM " + TABLE_LOGS +
                " WHERE " + COLUMN_USER_ID + " = ? AND " + COLUMN_ACCOUNT_ID + " = ?" +
                " ORDER BY " + COLUMN_UPDATED_AT + " DESC LIMIT " + limit;
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), String.valueOf(accountId)});
        try {
            while (cursor.moveToNext()) {
                result.add(buildEntryFromCursor(cursor));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    private LogEntry buildEntryFromCursor(Cursor cursor) {
        long id = cursor.getLong(0);
        int accountId = cursor.getInt(1);
        long userId = cursor.getLong(2);
        String userName = cursor.isNull(3) ? null : cursor.getString(3);
        String statusText = cursor.isNull(4) ? null : cursor.getString(4);
        String statusClass = cursor.isNull(5) ? null : cursor.getString(5);
        long lastSeen = cursor.isNull(6) ? 0L : cursor.getLong(6);
        long statusExpires = cursor.isNull(7) ? 0L : cursor.getLong(7);
        boolean isOnline = cursor.getInt(8) == 1;
        String actionText = cursor.isNull(9) ? null : cursor.getString(9);
        int updateMask = cursor.getInt(10);
        long updatedAt = cursor.getLong(11);
        return new LogEntry(id, accountId, userId, userName, statusText, statusClass, lastSeen, statusExpires, isOnline, actionText, updateMask, updatedAt);
    }

    public long getEntryCount() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, TABLE_LOGS);
    }

    public void clearAll() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(TABLE_LOGS, null, null);
    }

    public static class LogEntry {
        public final long rowId;
        public final int accountId;
        public final long userId;
        @Nullable
        public final String userName;
        @Nullable
        public final String statusText;
        @Nullable
        public final String statusClass;
        public final long lastSeenAt;
        public final long statusExpiresAt;
        public final boolean isOnline;
        @Nullable
        public final String actionText;
        public final int updateMask;
        public final long updatedAt;

        LogEntry(long rowId, int accountId, long userId, @Nullable String userName, @Nullable String statusText,
                 @Nullable String statusClass, long lastSeenAt, long statusExpiresAt, boolean isOnline,
                 @Nullable String actionText, int updateMask, long updatedAt) {
            this.rowId = rowId;
            this.accountId = accountId;
            this.userId = userId;
            this.userName = userName;
            this.statusText = statusText;
            this.statusClass = statusClass;
            this.lastSeenAt = lastSeenAt;
            this.statusExpiresAt = statusExpiresAt;
            this.isOnline = isOnline;
            this.actionText = actionText;
            this.updateMask = updateMask;
            this.updatedAt = updatedAt;
        }
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + TABLE_LOGS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_ACCOUNT_ID + " INTEGER NOT NULL, " +
                    COLUMN_USER_ID + " INTEGER NOT NULL, " +
                    COLUMN_USER_NAME + " TEXT, " +
                    COLUMN_STATUS_TEXT + " TEXT, " +
                    COLUMN_STATUS_CLASS + " TEXT, " +
                    COLUMN_LAST_SEEN + " INTEGER, " +
                    COLUMN_STATUS_EXPIRES + " INTEGER, " +
                    COLUMN_IS_ONLINE + " INTEGER NOT NULL DEFAULT 0, " +
                    COLUMN_ACTION_TEXT + " TEXT, " +
                    COLUMN_UPDATE_MASK + " INTEGER NOT NULL, " +
                    COLUMN_UPDATED_AT + " INTEGER NOT NULL" +
                    ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_status_account_user ON " + TABLE_LOGS +
                    " (" + COLUMN_ACCOUNT_ID + ", " + COLUMN_USER_ID + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_user_status_updated_at ON " + TABLE_LOGS +
                    " (" + COLUMN_UPDATED_AT + ")");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // No upgrades yet.
        }
    }
}
