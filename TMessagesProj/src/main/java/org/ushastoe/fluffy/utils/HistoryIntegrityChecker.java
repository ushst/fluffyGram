package org.ushastoe.fluffy.utils;

import android.content.SharedPreferences;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Detects sudden mass gaps ("holes") in locally cached message history, the kind
 * produced by MessagesStorage#clearLocalDatabase(). A single dialog having an
 * unloaded-history hole is normal; a large jump in total hole coverage across many
 * dialogs between two checks is the signature of an accidental cache wipe.
 */
public final class HistoryIntegrityChecker {

    private static final String PREFS_NAME = "fluffy_history_integrity";
    private static final long ANOMALY_DELTA_THRESHOLD = 200_000L;

    private HistoryIntegrityChecker() {
    }

    public static class CheckResult {
        public final boolean anomalous;
        public final int affectedDialogs;
        public final long totalGap;
        public final long deltaGap;
        public final ArrayList<MessagesStorage.DialogHoleInfo> dialogs;

        CheckResult(boolean anomalous, int affectedDialogs, long totalGap, long deltaGap, ArrayList<MessagesStorage.DialogHoleInfo> dialogs) {
            this.anomalous = anomalous;
            this.affectedDialogs = affectedDialogs;
            this.totalGap = totalGap;
            this.deltaGap = deltaGap;
            this.dialogs = dialogs;
        }
    }

    public interface Callback {
        void onResult(CheckResult result);
    }

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
    }

    /** Runs the summary query off the UI thread and reports back on it. Does not touch the stored baseline. */
    public static void loadSummaryAsync(int account, Callback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<MessagesStorage.DialogHoleInfo> dialogs = MessagesStorage.getInstance(account).getMessageHolesSummary();
            long totalGap = 0;
            for (MessagesStorage.DialogHoleInfo info : dialogs) {
                totalGap += info.totalGap;
            }
            long previousTotal = prefs().getLong("lastTotalGap_" + account, -1);
            long deltaGap = previousTotal < 0 ? 0 : totalGap - previousTotal;
            final long finalTotalGap = totalGap;
            final long finalDeltaGap = deltaGap;
            AndroidUtilities.runOnUIThread(() -> callback.onResult(new CheckResult(false, dialogs.size(), finalTotalGap, finalDeltaGap, dialogs)));
        });
    }

    /**
     * Runs the check, compares against the last stored baseline for this account, updates the
     * baseline to the current snapshot, and reports whether the jump looks like a mass wipe.
     * Safe to call once per app resume.
     */
    public static void checkAndUpdateBaseline(int account, Callback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<MessagesStorage.DialogHoleInfo> dialogs = MessagesStorage.getInstance(account).getMessageHolesSummary();
            long totalGap = 0;
            for (MessagesStorage.DialogHoleInfo info : dialogs) {
                totalGap += info.totalGap;
            }
            SharedPreferences prefs = prefs();
            long previousTotal = prefs.getLong("lastTotalGap_" + account, -1);
            long deltaGap = previousTotal < 0 ? 0 : totalGap - previousTotal;
            boolean anomalous = previousTotal >= 0 && deltaGap > ANOMALY_DELTA_THRESHOLD;

            prefs.edit()
                    .putLong("lastTotalGap_" + account, totalGap)
                    .putInt("lastAffectedDialogs_" + account, dialogs.size())
                    .putLong("lastCheckTime_" + account, System.currentTimeMillis())
                    .apply();

            ArrayList<MessagesStorage.DialogHoleInfo> sorted = new ArrayList<>(dialogs);
            Collections.sort(sorted, (a, b) -> Long.compare(b.totalGap, a.totalGap));

            final boolean finalAnomalous = anomalous;
            final long finalTotalGap = totalGap;
            final long finalDeltaGap = deltaGap;
            AndroidUtilities.runOnUIThread(() -> callback.onResult(new CheckResult(finalAnomalous, sorted.size(), finalTotalGap, finalDeltaGap, sorted)));
        });
    }
}
