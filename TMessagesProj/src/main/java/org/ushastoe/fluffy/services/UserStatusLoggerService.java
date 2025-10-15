package org.ushastoe.fluffy.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.storage.UserStatusStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserStatusLoggerService extends Service implements NotificationCenter.NotificationCenterDelegate {

    private static final String TAG = "fluffy_service";

    private final Map<Long, TLRPC.UserStatus> previousStatuses = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "UserStatusLoggerService onCreate");
        for (int currentAccount = 0; currentAccount < UserConfig.MAX_ACCOUNT_COUNT; currentAccount++) {
            if (UserConfig.getInstance(currentAccount).isClientActivated()) {
                NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.userStatusChanged);
                NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
                Log.d(TAG, "Subscribed to userStatusChanged and updateInterfaces for account " + currentAccount);
                seedAccountStatuses(currentAccount);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "UserStatusLoggerService onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "UserStatusLoggerService onDestroy");
        for (int currentAccount = 0; currentAccount < UserConfig.MAX_ACCOUNT_COUNT; currentAccount++) {
            if (UserConfig.getInstance(currentAccount).isClientActivated()) {
                NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.userStatusChanged);
                NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.userStatusChanged) {
            handleUserStatusChanged(account, args);
        } else if (id == NotificationCenter.updateInterfaces) {
            handleUpdateInterfaces(account, args);
        }
    }

    private void handleUserStatusChanged(int account, Object... args) {
        Log.d(TAG, "handleUserStatusChanged called for account " + account);
        if (args == null || args.length == 0) {
            Log.d(TAG, "handleUserStatusChanged: args is null or empty");
            return;
        }
        TLRPC.User user = null;
        Object firstArg = args[0];
        if (firstArg instanceof TLRPC.User) {
            user = resolveUser(account, (TLRPC.User) firstArg);
        } else if (firstArg instanceof Integer) {
            user = MessagesController.getInstance(account).getUser(((Integer) firstArg).longValue());
        } else if (firstArg instanceof Long) {
            user = MessagesController.getInstance(account).getUser((Long) firstArg);
        }
        if (user == null) {
            Log.d(TAG, "handleUserStatusChanged: user is null");
            return;
        }
        Log.d(TAG, "handleUserStatusChanged: user " + user.id + ", status " + (user.status != null ? user.status.getClass().getSimpleName() : "null"));
        
        TLRPC.UserStatus oldStatus = previousStatuses.get(user.id);
        TLRPC.UserStatus newStatus = user.status;
        Log.d(TAG, "handleUserStatusChanged: oldStatus " + (oldStatus != null ? oldStatus.getClass().getSimpleName() : "null"));
        
        boolean wasHiddenOrOffline = oldStatus != null && (
            oldStatus instanceof TLRPC.TL_userStatusHidden ||
            oldStatus instanceof TLRPC.TL_userStatusRecently ||
            oldStatus instanceof TLRPC.TL_userStatusLastWeek ||
            oldStatus instanceof TLRPC.TL_userStatusLastMonth ||
            oldStatus instanceof TLRPC.TL_userStatusEmpty ||
            (oldStatus instanceof TLRPC.TL_userStatusOffline && !isUserOnline(account, oldStatus))
        );
        
        boolean isNowOnline = isUserOnline(account, user);
        boolean isNowHidden = newStatus != null && (
            newStatus instanceof TLRPC.TL_userStatusHidden ||
            newStatus instanceof TLRPC.TL_userStatusRecently ||
            newStatus instanceof TLRPC.TL_userStatusLastWeek ||
            newStatus instanceof TLRPC.TL_userStatusLastMonth ||
            newStatus instanceof TLRPC.TL_userStatusEmpty
        );
        
        Log.d(TAG, "handleUserStatusChanged: wasHiddenOrOffline=" + wasHiddenOrOffline + ", isNowOnline=" + isNowOnline + ", isNowHidden=" + isNowHidden);
        
        // Обновляем previousStatuses в любом случае
        previousStatuses.put(user.id, user.status);

        boolean shouldLog = false;
        String logReason = "";
        
        // 1. Переход от скрытого/оффлайн к онлайн
        if (wasHiddenOrOffline && isNowOnline) {
            shouldLog = true;
            logReason = "was hidden/offline -> now online";
        }
        // 2. Переход от онлайн к скрытому (пользователь скрыл статус)
        else if (oldStatus instanceof TLRPC.TL_userStatusOnline && isNowHidden) {
            shouldLog = true;
            logReason = "was online -> now hidden";
        }
        // 3. Обновление скрытого статуса (КЛЮЧЕВОЕ: для пользователей с постоянно скрытым статусом 
        // это единственный способ узнать, что они онлайн)
        // Логируем ЛЮБОЕ изменение для пользователей со скрытым статусом
        else if (isNowHidden) {
            shouldLog = true;
            logReason = "hidden status update (likely online activity)";
        }
        
        if (shouldLog) {
            Log.d(TAG, "handleUserStatusChanged: logging status change for user " + user.id + " (" + logReason + ")");
            long changeTimestamp = System.currentTimeMillis();
            StatusData data = buildStatusData(account, user, null, MessagesController.UPDATE_MASK_STATUS, changeTimestamp);
            if (data == null) {
                Log.d(TAG, "handleUserStatusChanged: data is null");
                return;
            }
            queueInsert(account, data);
        }
        else {
            Log.d(TAG, "handleUserStatusChanged: not logging for user " + user.id);
        }
    }

    private void handleUpdateInterfaces(int account, Object... args) {
        if (args == null || args.length == 0) {
            return;
        }
        int mask = (int) args[0];
        // UPDATE_MASK_USER_PRINT = 0x00000040 (typing indicator)
        if ((mask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
            Log.d(TAG, "handleUpdateInterfaces: typing activity detected for account " + account);
            // Получаем список активных typing пользователей и логируем их онлайн статус
            // Это происходит неявно - когда пользователь печатает, мы знаем что он онлайн
        }
    }

    private void seedAccountStatuses(int account) {
        ContactsController contactsController = ContactsController.getInstance(account);
        ArrayList<TLRPC.TL_contact> contactsSnapshot = new ArrayList<>(contactsController.contacts);
        if (contactsSnapshot.isEmpty()) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            MessagesController messagesController = MessagesController.getInstance(account);
            UserStatusStorage storage = UserStatusStorage.getInstance(ApplicationLoader.applicationContext);
            boolean inserted = false;
            for (TLRPC.TL_contact contact : contactsSnapshot) {
                TLRPC.User user = messagesController.getUser(contact.user_id);
                if (!shouldCaptureStatus(account, user)) {
                    continue;
                }
                long changeTimestamp = System.currentTimeMillis();
                StatusData data = buildStatusData(account, user, null, MessagesController.UPDATE_MASK_STATUS, changeTimestamp);
                if (data == null) {
                    continue;
                }
                storage.insertStatus(account, data.userId, data.userName, data.statusText, data.statusClass, data.lastSeenAt, data.statusExpiresAt, data.isOnline, data.actionText, data.updateMask);
                previousStatuses.put(user.id, user.status);
                Log.d(TAG, "seedAccountStatuses: seeded user " + user.id + " with status " + (user.status != null ? user.status.getClass().getSimpleName() : "null"));
                inserted = true;
            }
            if (inserted) {
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.userStatusLogUpdated));
            }
        });
    }

    private void queueInsert(int account, StatusData data) {
        Log.d(TAG, "queueInsert: inserting status for user " + data.userId + ", account " + account);
        Utilities.globalQueue.postRunnable(() -> {
            UserStatusStorage storage = UserStatusStorage.getInstance(ApplicationLoader.applicationContext);
            storage.insertStatus(account, data.userId, data.userName, data.statusText, data.statusClass, data.lastSeenAt, data.statusExpiresAt, data.isOnline, data.actionText, data.updateMask);
            AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.userStatusLogUpdated));
        });
    }

    private StatusData buildStatusData(int account, TLRPC.User user, String actionText, int updateMask, long fallbackTimestamp) {
        if (user == null) {
            return null;
        }
        CharSequence statusSequence = LocaleController.formatUserStatus(account, user);
        String statusText = statusSequence != null ? statusSequence.toString() : null;
        String userName = UserObject.getUserName(user);
        if (TextUtils.isEmpty(userName)) {
            userName = String.valueOf(user.id);
        }
        String statusClass = user.status != null ? user.status.getClass().getSimpleName() : null;
        long lastSeenAt = extractLastSeen(user, fallbackTimestamp);
        long statusExpiresAt = extractStatusExpires(user, fallbackTimestamp);
        boolean isOnline = isUserOnline(account, user);
        return new StatusData(user.id, userName, statusText, statusClass, lastSeenAt, statusExpiresAt, isOnline, actionText, updateMask);
    }

    private boolean shouldCaptureStatus(int account, TLRPC.User user) {
        if (user == null || user.status == null) {
            return false;
        }
        return user.status instanceof TLRPC.TL_userStatusHidden
                || user.status instanceof TLRPC.TL_userStatusRecently
                || user.status instanceof TLRPC.TL_userStatusLastWeek
                || user.status instanceof TLRPC.TL_userStatusLastMonth
                || user.status instanceof TLRPC.TL_userStatusEmpty;
    }

    private TLRPC.User resolveUser(int account, TLRPC.User candidate) {
        TLRPC.User cached = MessagesController.getInstance(account).getUser(candidate.id);
        return cached != null ? cached : candidate;
    }

    private long extractLastSeen(TLRPC.User user, long fallbackTimestamp) {
        if (user == null || user.status == null) {
            return 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusOffline) {
            int expires = ((TLRPC.TL_userStatusOffline) user.status).expires;
            if (expires > 0) {
                return expires * 1000L;
            }
            return fallbackTimestamp > 0 ? fallbackTimestamp : 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            int expires = ((TLRPC.TL_userStatusOnline) user.status).expires;
            if (expires > 0) {
                return expires * 1000L;
            }
            return fallbackTimestamp > 0 ? fallbackTimestamp : 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusRecently
                || user.status instanceof TLRPC.TL_userStatusLastWeek
                || user.status instanceof TLRPC.TL_userStatusLastMonth
                || user.status instanceof TLRPC.TL_userStatusHidden
                || user.status instanceof TLRPC.TL_userStatusEmpty) {
            return fallbackTimestamp > 0 ? fallbackTimestamp : 0;
        }
        return 0;
    }

    private long extractStatusExpires(TLRPC.User user, long fallbackTimestamp) {
        if (user == null || user.status == null) {
            return 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            int expires = ((TLRPC.TL_userStatusOnline) user.status).expires;
            if (expires > 0) {
                return expires * 1000L;
            }
            return fallbackTimestamp > 0 ? fallbackTimestamp : 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusOffline) {
            int expires = ((TLRPC.TL_userStatusOffline) user.status).expires;
            if (expires > 0) {
                return expires * 1000L;
            }
            return fallbackTimestamp > 0 ? fallbackTimestamp : 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusRecently
                || user.status instanceof TLRPC.TL_userStatusLastWeek
                || user.status instanceof TLRPC.TL_userStatusLastMonth
                || user.status instanceof TLRPC.TL_userStatusHidden
                || user.status instanceof TLRPC.TL_userStatusEmpty) {
            return fallbackTimestamp > 0 ? fallbackTimestamp : 0;
        }
        return 0;
    }

    private boolean isUserOnline(int account, TLRPC.User user) {
        if (user == null || user.status == null) {
            return false;
        }
        int currentTime = ConnectionsManager.getInstance(account).getCurrentTime();
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            return ((TLRPC.TL_userStatusOnline) user.status).expires > currentTime;
        }
        if (user.status instanceof TLRPC.TL_userStatusOffline) {
            return ((TLRPC.TL_userStatusOffline) user.status).expires > currentTime;
        }
        return false;
    }

    private boolean isUserOnline(int account, TLRPC.UserStatus status) {
        if (status == null) {
            return false;
        }
        int currentTime = ConnectionsManager.getInstance(account).getCurrentTime();
        if (status instanceof TLRPC.TL_userStatusOnline) {
            return ((TLRPC.TL_userStatusOnline) status).expires > currentTime;
        }
        if (status instanceof TLRPC.TL_userStatusOffline) {
            return ((TLRPC.TL_userStatusOffline) status).expires > currentTime;
        }
        return false;
    }

    private static final class StatusData {
        final long userId;
        final String userName;
        final String statusText;
        final String statusClass;
        final long lastSeenAt;
        final long statusExpiresAt;
        final boolean isOnline;
        final String actionText;
        final int updateMask;

        StatusData(long userId, String userName, String statusText, String statusClass, long lastSeenAt, long statusExpiresAt, boolean isOnline, String actionText, int updateMask) {
            this.userId = userId;
            this.userName = userName;
            this.statusText = statusText;
            this.statusClass = statusClass;
            this.lastSeenAt = lastSeenAt;
            this.statusExpiresAt = statusExpiresAt;
            this.isOnline = isOnline;
            this.actionText = actionText;
            this.updateMask = updateMask;
        }
    }
}
