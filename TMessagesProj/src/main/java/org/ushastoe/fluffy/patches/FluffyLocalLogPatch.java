package org.ushastoe.fluffy.patches;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FluffyLocalLogPatch {
    private static final String PREFS_NAME = "fluffy_local_logs";
    private static final String KEY_CONSENT_PREFIX = "consent_";
    private static final int CONSENT_UNKNOWN = -1;
    private static final int CONSENT_DISABLED = 0;
    private static final int CONSENT_ENABLED = 1;

    private static final Object LOCK = new Object();
    private static final Map<Long, File> SESSION_FILES = new HashMap<>();
    private static final Set<Long> PROMPTS_IN_FLIGHT = new HashSet<>();

    private static volatile boolean crashHandlerInstalled;
    private static volatile Thread.UncaughtExceptionHandler previousCrashHandler;

    private FluffyLocalLogPatch() {
    }

    public static void onApplicationCreated(ApplicationLoader applicationLoader) {
        installCrashHandler();
    }

    public static void onLaunchActivityResumed(LaunchActivity target) {
        scheduleConsentOrSession(target);
    }

    public static void onSelectedAccountChanged(LaunchActivity target, int account) {
        scheduleConsentOrSession(target);
    }

    public static void onSaveLogClicked(BaseFragment fragment) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }

        int account = fragment.getCurrentAccount();
        long accountUserId = getAccountUserId(account);
        if (accountUserId == 0L) {
            showInfoDialog(fragment, LocaleController.getString(R.string.FluffyLocalLogsTitle), LocaleController.getString(R.string.FluffyLocalLogsUnavailable));
            return;
        }

        int consentState = getConsentState(accountUserId);
        if (consentState != CONSENT_ENABLED) {
            showConsentDialog(fragment, account, true);
            return;
        }

        File savedFile = saveCurrentLog(account);
        if (savedFile == null) {
            showInfoDialog(fragment, LocaleController.getString(R.string.FluffySaveLog), LocaleController.getString(R.string.FluffyLocalLogsSaveFailed));
            return;
        }

        showInfoDialog(
                fragment,
                LocaleController.getString(R.string.FluffySaveLog),
                String.format(Locale.US, "%s\n\n%s", LocaleController.getString(R.string.FluffyLocalLogsSaved), savedFile.getAbsolutePath())
        );
    }

    private static void scheduleConsentOrSession(LaunchActivity target) {
        if (target == null) {
            return;
        }
        AndroidUtilities.runOnUIThread(() -> handleLaunchActivity(target), 350);
    }

    private static void handleLaunchActivity(LaunchActivity target) {
        if (target == null || target.isFinishing()) {
            return;
        }

        int account = UserConfig.selectedAccount;
        if (!UserConfig.getInstance(account).isClientActivated()) {
            return;
        }

        long accountUserId = getAccountUserId(account);
        if (accountUserId == 0L) {
            return;
        }

        int consentState = getConsentState(accountUserId);
        if (consentState == CONSENT_UNKNOWN) {
            showConsentDialog(target, account, false);
        } else if (consentState == CONSENT_ENABLED) {
            File sessionFile = ensureSessionFile(account);
            if (sessionFile != null) {
                appendLine(sessionFile, "INFO", "LaunchActivity resumed for account " + accountUserId);
            }
        }
    }

    private static void showConsentDialog(LaunchActivity target, int account, boolean saveAfterEnable) {
        long accountUserId = getAccountUserId(account);
        if (accountUserId == 0L) {
            return;
        }

        synchronized (LOCK) {
            if (PROMPTS_IN_FLIGHT.contains(accountUserId)) {
                return;
            }
            PROMPTS_IN_FLIGHT.add(accountUserId);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(target);
        builder.setTitle(LocaleController.getString(R.string.FluffyLocalLogsTitle));
        builder.setMessage(LocaleController.getString(R.string.FluffyLocalLogsConsentMessage));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            setConsentState(accountUserId, CONSENT_ENABLED);
            synchronized (LOCK) {
                PROMPTS_IN_FLIGHT.remove(accountUserId);
            }
            File sessionFile = ensureSessionFile(account);
            if (sessionFile != null) {
                appendLine(sessionFile, "INFO", "Local logging enabled");
            }
            if (saveAfterEnable) {
                File savedFile = saveCurrentLog(account);
                showSimpleActivityDialog(
                        target,
                        LocaleController.getString(R.string.FluffySaveLog),
                        savedFile == null
                                ? LocaleController.getString(R.string.FluffyLocalLogsSaveFailed)
                                : String.format(Locale.US, "%s\n\n%s", LocaleController.getString(R.string.FluffyLocalLogsSaved), savedFile.getAbsolutePath())
                );
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> {
            setConsentState(accountUserId, CONSENT_DISABLED);
            synchronized (LOCK) {
                PROMPTS_IN_FLIGHT.remove(accountUserId);
            }
        });

        AlertDialog dialog = (AlertDialog) target.showAlertDialog(builder);
        if (dialog != null) {
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.setOnDismissListener(d -> {
                synchronized (LOCK) {
                    PROMPTS_IN_FLIGHT.remove(accountUserId);
                }
            });
        } else {
            synchronized (LOCK) {
                PROMPTS_IN_FLIGHT.remove(accountUserId);
            }
        }
    }

    private static void showConsentDialog(BaseFragment fragment, int account, boolean saveAfterEnable) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }

        long accountUserId = getAccountUserId(account);
        if (accountUserId == 0L) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyLocalLogsTitle));
        builder.setMessage(LocaleController.getString(R.string.FluffyLocalLogsConsentMessage));
        builder.setPositiveButton(LocaleController.getString(R.string.OK), (dialog, which) -> {
            setConsentState(accountUserId, CONSENT_ENABLED);
            File sessionFile = ensureSessionFile(account);
            if (sessionFile != null) {
                appendLine(sessionFile, "INFO", "Local logging enabled");
            }
            if (saveAfterEnable) {
                File savedFile = saveCurrentLog(account);
                showInfoDialog(
                        fragment,
                        LocaleController.getString(R.string.FluffySaveLog),
                        savedFile == null
                                ? LocaleController.getString(R.string.FluffyLocalLogsSaveFailed)
                                : String.format(Locale.US, "%s\n\n%s", LocaleController.getString(R.string.FluffyLocalLogsSaved), savedFile.getAbsolutePath())
                );
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), (dialog, which) -> setConsentState(accountUserId, CONSENT_DISABLED));

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        fragment.showDialog(dialog);
    }

    private static void showInfoDialog(BaseFragment fragment, CharSequence title, CharSequence message) {
        if (fragment == null || fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        fragment.showDialog(builder.create());
    }

    private static void showSimpleActivityDialog(Activity activity, CharSequence title, CharSequence message) {
        if (activity == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        AlertDialog dialog = builder.show();
        dialog.setCanceledOnTouchOutside(true);
    }

    private static File saveCurrentLog(int account) {
        long accountUserId = getAccountUserId(account);
        if (accountUserId == 0L || getConsentState(accountUserId) != CONSENT_ENABLED) {
            return null;
        }

        File sessionFile = ensureSessionFile(account);
        if (sessionFile == null) {
            return null;
        }

        appendLine(sessionFile, "INFO", "Manual save requested");

        File directory = getLogsDirectory();
        if (directory == null) {
            return null;
        }

        File outputFile = new File(directory, buildFileName("saved", accountUserId));
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8)) {
            writeHeader(writer, accountUserId, "manual-save");
            appendSection(writer, "Fluffy Session Log", sessionFile);
            appendSection(writer, "Telegram App Log", findLatestTelegramLog(false));
            appendSection(writer, "Telegram MTProto Log", findLatestTelegramLog(true));
            writer.flush();
            return outputFile;
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
    }

    private static File ensureSessionFile(int account) {
        long accountUserId = getAccountUserId(account);
        if (accountUserId == 0L || getConsentState(accountUserId) != CONSENT_ENABLED) {
            return null;
        }

        synchronized (LOCK) {
            File existing = SESSION_FILES.get(accountUserId);
            if (existing != null && existing.exists()) {
                return existing;
            }

            File directory = getLogsDirectory();
            if (directory == null) {
                return null;
            }

            File sessionFile = new File(directory, buildFileName("session", accountUserId));
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(sessionFile), StandardCharsets.UTF_8)) {
                writeHeader(writer, accountUserId, "session-start");
                writer.flush();
                SESSION_FILES.put(accountUserId, sessionFile);
                return sessionFile;
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        }
    }

    private static void installCrashHandler() {
        if (crashHandlerInstalled) {
            return;
        }
        synchronized (LOCK) {
            if (crashHandlerInstalled) {
                return;
            }
            previousCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                try {
                    appendCrash(thread, throwable);
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (previousCrashHandler != null) {
                    previousCrashHandler.uncaughtException(thread, throwable);
                }
            });
            crashHandlerInstalled = true;
        }
    }

    private static void appendCrash(Thread thread, Throwable throwable) {
        if (throwable == null) {
            return;
        }

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        printWriter.flush();

        long selectedUserId = getAccountUserId(UserConfig.selectedAccount);
        if (selectedUserId != 0L) {
            File sessionFile = ensureSessionFile(UserConfig.selectedAccount);
            if (sessionFile != null) {
                appendLine(sessionFile, "CRASH", "Uncaught exception in thread " + thread.getName());
                appendLine(sessionFile, "CRASH", stringWriter.toString());
            }
            return;
        }

        synchronized (LOCK) {
            for (File sessionFile : SESSION_FILES.values()) {
                appendLine(sessionFile, "CRASH", "Uncaught exception in thread " + thread.getName());
                appendLine(sessionFile, "CRASH", stringWriter.toString());
            }
        }
    }

    private static void appendSection(OutputStreamWriter writer, String title, File file) throws IOException {
        writer.write("\n===== " + title + " =====\n");
        if (file == null || !file.exists()) {
            writer.write("not available\n");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write('\n');
            }
        }
    }

    private static void writeHeader(OutputStreamWriter writer, long accountUserId, String reason) throws IOException {
        writer.write("Fluffy Local Log\n");
        writer.write("account_id=" + accountUserId + "\n");
        writer.write("created_at=" + formatTimestamp(System.currentTimeMillis()) + "\n");
        writer.write("reason=" + reason + "\n");
        writer.write("build=" + AndroidUtilities.getBuildVersionInfo() + "\n");
        writer.write("device=" + Build.MANUFACTURER + " " + Build.MODEL + " / Android " + Build.VERSION.RELEASE + "\n");
        writer.write("storage=format=session_or_saved_account_<accountId>_<yyyy-MM-dd_HH-mm-ss>.log\n");
        writer.write("after_crash=next app start creates a new session log file\n");
        writer.write('\n');
    }

    private static void appendLine(File file, String level, String message) {
        if (file == null || TextUtils.isEmpty(message)) {
            return;
        }
        synchronized (LOCK) {
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
                writer.write(formatTimestamp(System.currentTimeMillis()));
                writer.write(" [");
                writer.write(level);
                writer.write("] ");
                writer.write(message);
                if (!message.endsWith("\n")) {
                    writer.write('\n');
                }
                writer.flush();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }

    private static File findLatestTelegramLog(boolean mtproto) {
        File logsDir = AndroidUtilities.getLogsDir();
        if (logsDir == null || !logsDir.isDirectory()) {
            return null;
        }

        File[] files = logsDir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File latest = null;
        for (File file : files) {
            if (file == null || !file.isFile()) {
                continue;
            }
            String name = file.getName();
            if (mtproto) {
                if (!name.endsWith("_mtproto.txt")) {
                    continue;
                }
            } else {
                if (!name.endsWith(".txt") || name.endsWith("_mtproto.txt")) {
                    continue;
                }
            }
            if (latest == null || file.lastModified() > latest.lastModified()) {
                latest = file;
            }
        }
        return latest;
    }

    private static File getLogsDirectory() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        File dir = new File(ApplicationLoader.getFilesDirFixed(), "fluffy_logs");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return dir;
    }

    private static String buildFileName(String prefix, long accountUserId) {
        return prefix + "_account_" + accountUserId + "_" + formatFileTimestamp(System.currentTimeMillis()) + ".log";
    }

    private static String formatTimestamp(long time) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date(time));
    }

    private static String formatFileTimestamp(long time) {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date(time));
    }

    private static SharedPreferences getPreferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static int getConsentState(long accountUserId) {
        return getPreferences().getInt(KEY_CONSENT_PREFIX + accountUserId, CONSENT_UNKNOWN);
    }

    private static void setConsentState(long accountUserId, int state) {
        getPreferences().edit().putInt(KEY_CONSENT_PREFIX + accountUserId, state).apply();
    }

    private static long getAccountUserId(int account) {
        if (!UserConfig.isValidAccount(account)) {
            return 0L;
        }
        return UserConfig.getInstance(account).getClientUserId();
    }
}
