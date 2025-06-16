package org.ushastoe.fluffy.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;

public class UserStatusLoggerService extends Service implements NotificationCenter.NotificationCenterDelegate {

    private static final String TAG = "fluffy_service";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "UserStatusLoggerService onCreate");
        for (int currentAccount = 0; currentAccount < UserConfig.MAX_ACCOUNT_COUNT; currentAccount++) {
            if (UserConfig.getInstance(currentAccount).isClientActivated()) {
                NotificationCenter.getInstance(currentAccount).addObserver(this, NotificationCenter.updateInterfaces);
                Log.d(TAG, "Subscribed to updateInterfaces for account " + currentAccount);
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
                NotificationCenter.getInstance(currentAccount).removeObserver(this, NotificationCenter.updateInterfaces);
            }
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        Log.d(TAG, "didReceivedNotification - ID: " + id + ", Account: " + account);

        if (args == null) {
            Log.d(TAG, "Arguments (args): null");
        } else if (args.length == 0) {
            Log.d(TAG, "Arguments (args): empty (length 0)");
        } else {
            Log.d(TAG, "Received " + args.length + " arguments:");
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg != null) {
                    Log.d(TAG, "args[" + i + "]: " + arg.toString() + " (Type: " + arg.getClass().getName() + ")");
                } else {
                    Log.d(TAG, "args[" + i + "]: null");
                }
            }
        }
    }
//    @Override
//    public void didReceivedNotification(int id, int account, Object... args) {
//        Log.d(TAG, "didReceivedNotification - ID: " + id + ", Account: " + account); // Лог получения любого уведомления
//
//        if (id == NotificationCenter.updateInterfaces) {
//            Log.d(TAG, "Received updateInterfaces for account " + account);
//            if (args == null || args.length == 0) {
//                Log.e(TAG, "updateInterfaces received with no arguments!");
//                return;
//            }
//
//            if (!(args[0] instanceof Integer)) {
//                Log.e(TAG, "updateInterfaces: args[0] is not an Integer! It's: " + (args[0] != null ? args[0].getClass().getName() : "null"));
//                return;
//            }
//            int updateMask = (Integer) args[0];
//            Log.d(TAG, "updateMask: " + updateMask);
//
//            boolean isStatusUpdate = (updateMask & MessagesController.UPDATE_MASK_STATUS) != 0;
//            boolean isPrintUpdate = (updateMask & MessagesController.UPDATE_MASK_USER_PRINT) != 0;
//
//            Log.d(TAG, "isStatusUpdate: " + isStatusUpdate + ", isPrintUpdate: " + isPrintUpdate);
//
//            if (isStatusUpdate || isPrintUpdate) {
//                long dialogIdFromArgs = 0;
//
//                if (args.length > 1) {
//                    if (args[1] instanceof Long) {
//                        dialogIdFromArgs = (Long) args[1];
//                        Log.d(TAG, "dialogIdFromArgs (Long): " + dialogIdFromArgs);
//                    } else {
//                        Log.e(TAG, "updateInterfaces: args[1] is not a Long! It's: " + (args[1] != null ? args[1].getClass().getName() : "null"));
//                        // Можно попробовать извлечь dialogId из других аргументов, если структура другая
//                        // Например, иногда он может быть в args[0] для других типов уведомлений, но для updateInterfaces обычно в args[1]
//                        // if (args[0] instanceof Long) dialogIdFromArgs = (Long) args[0];
//                    }
//                } else {
//                    Log.w(TAG, "updateInterfaces: args.length <= 1, cannot get dialogIdFromArgs");
//                }
//
//                if (dialogIdFromArgs > 0) { // Пользователи (положительные ID)
//                    long userId = dialogIdFromArgs;
//                    Log.d(TAG, "Attempting to get user with ID: " + userId + " for account " + account);
//                    TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
//
//                    if (user != null) {
//                        Log.d(TAG, "User found: " + UserObject.getUserName(user));
//                        CharSequence statusString = LocaleController.formatUserStatus(account, user);
//                        String userName = UserObject.getUserName(user);
//
//                        if (isStatusUpdate) {
//                            if (statusString != null) {
//                                Log.d(TAG, "ACCOUNT_SPECIFIC_LOG - Account " + account + " - User: " + userName + " (ID: " + userId + "), Status: " + statusString.toString());
//                            } else {
//                                Log.d(TAG, "ACCOUNT_SPECIFIC_LOG - Account " + account + " - User: " + userName + " (ID: " + userId + "), Status: null (possibly hidden or offline with no last seen)");
//                            }
//                        }
//
//                        if (isPrintUpdate) {
//                            // args[1] должен быть dialog_id, args[2] - объект TLRPC.SendMessageAction
//                            if (args.length > 2 && args[1] instanceof Long && (Long)args[1] == dialogIdFromArgs) {
//                                Log.d(TAG, "ACCOUNT_SPECIFIC_LOG - Account " + account + " - User: " + userName + " (ID: " + userId + ") is typing/performing an action. Action object: " + (args[2] != null ? args[2].getClass().getSimpleName() : "null"));
//                            } else {
//                                Log.w(TAG, "Print update for user " + userName + " (ID: " + userId + ") but args structure is unexpected or dialogId mismatch. args.length: " + args.length);
//                                if (args.length > 1) Log.w(TAG, "args[1] type: " + (args[1] != null ? args[1].getClass().getName() : "null") + ", value: " + args[1]);
//                            }
//                        }
//                    } else {
//                        Log.w(TAG, "User with ID " + userId + " not found in MessagesController cache for account " + account + " during status update.");
//                    }
//                } else {
//                    Log.d(TAG, "dialogIdFromArgs is not > 0 (value: " + dialogIdFromArgs + "). Not a user dialog or unexpected format.");
//                }
//            } else {
//                Log.d(TAG, "updateMask does not contain STATUS or PRINT flags.");
//            }
//        }
//        // Добавьте сюда логи для других ID уведомлений, если они вас интересуют
//        // else if (id == NotificationCenter.anotherNotification) { ... }
//    }
}