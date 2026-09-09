package org.ushastoe.fluffy.smartreply;

import android.util.Log;

/** Always-on logcat tag for Smart Reply debugging (FileLog is gated by LOGS_ENABLED). */
public final class SmartReplyLog {

    public static final String TAG = "SmartReply";

    private SmartReplyLog() {
    }

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void e(String message, Throwable error) {
        Log.e(TAG, message, error);
    }
}
