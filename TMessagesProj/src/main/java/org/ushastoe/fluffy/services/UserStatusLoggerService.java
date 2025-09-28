package org.ushastoe.fluffy.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ConnectionsManager;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.storage.UserStatusStorage;

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
        if (id != NotificationCenter.updateInterfaces || args == null || args.length == 0) {
            return;
        }
        if (!(args[0] instanceof Integer)) {
            Log.w(TAG, "updateInterfaces args[0] is not Integer");
            return;
        }
        int updateMask = (Integer) args[0];
        boolean isStatusUpdate = (updateMask & MessagesController.UPDATE_MASK_STATUS) != 0;
        boolean isPrintUpdate = (updateMask & MessagesController.UPDATE_MASK_USER_PRINT) != 0;
        if (!isStatusUpdate && !isPrintUpdate) {
            return;
        }

        long dialogId = 0;
        if (args.length > 1 && args[1] instanceof Long) {
            dialogId = (Long) args[1];
        }
        if (dialogId <= 0 || !DialogObject.isUserDialog(dialogId)) {
            return;
        }

        long userId = dialogId;
        TLRPC.User user = MessagesController.getInstance(account).getUser(userId);
        if (user == null) {
            return;
        }

        CharSequence statusSequence = isStatusUpdate ? LocaleController.formatUserStatus(account, user) : null;
        String statusText = statusSequence != null ? statusSequence.toString() : null;
        String userName = UserObject.getUserName(user);
        long lastSeenAt = extractLastSeen(user);
        long statusExpiresAt = extractStatusExpires(user);
        boolean isOnline = isUserOnline(account, user);
        String actionText = null;
        if (isPrintUpdate && args.length > 2 && args[2] instanceof TLRPC.SendMessageAction) {
            actionText = describeAction((TLRPC.SendMessageAction) args[2]);
        }
        String statusClass = user.status != null ? user.status.getClass().getSimpleName() : null;

        final String actionTextFinal = actionText;
        final String statusTextFinal = statusText;
        final String userNameFinal = userName;
        final String statusClassFinal = statusClass;

        Utilities.globalQueue.postRunnable(() -> {
            UserStatusStorage storage = UserStatusStorage.getInstance(ApplicationLoader.applicationContext);
            storage.insertStatus(account, userId, userNameFinal, statusTextFinal, statusClassFinal, lastSeenAt, statusExpiresAt, isOnline, actionTextFinal, updateMask);
            AndroidUtilities.runOnUIThread(() ->
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.userStatusLogUpdated));
        });
    }

    private long extractLastSeen(TLRPC.User user) {
        if (user == null || user.status == null) {
            return 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusOffline) {
            return ((TLRPC.TL_userStatusOffline) user.status).was_online * 1000L;
        }
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            return ((TLRPC.TL_userStatusOnline) user.status).expires * 1000L;
        }
        return 0;
    }

    private long extractStatusExpires(TLRPC.User user) {
        if (user == null || user.status == null) {
            return 0;
        }
        if (user.status instanceof TLRPC.TL_userStatusOnline) {
            return ((TLRPC.TL_userStatusOnline) user.status).expires * 1000L;
        }
        if (user.status instanceof TLRPC.TL_userStatusOffline) {
            return ((TLRPC.TL_userStatusOffline) user.status).was_online * 1000L;
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
            return ((TLRPC.TL_userStatusOffline) user.status).was_online > currentTime;
        }
        return false;
    }

    private String describeAction(TLRPC.SendMessageAction action) {
        if (action == null) {
            return null;
        }
        if (action instanceof TLRPC.TL_sendMessageTypingAction) {
            return LocaleController.getString("UserStatusLogActionTyping", R.string.UserStatusLogActionTyping);
        } else if (action instanceof TLRPC.TL_sendMessageRecordVideoAction) {
            return LocaleController.getString("UserStatusLogActionRecordingVideo", R.string.UserStatusLogActionRecordingVideo);
        } else if (action instanceof TLRPC.TL_sendMessageUploadVideoAction) {
            return LocaleController.getString("UserStatusLogActionUploadingVideo", R.string.UserStatusLogActionUploadingVideo);
        } else if (action instanceof TLRPC.TL_sendMessageRecordAudioAction) {
            return LocaleController.getString("UserStatusLogActionRecordingVoice", R.string.UserStatusLogActionRecordingVoice);
        } else if (action instanceof TLRPC.TL_sendMessageUploadAudioAction) {
            return LocaleController.getString("UserStatusLogActionUploadingVoice", R.string.UserStatusLogActionUploadingVoice);
        } else if (action instanceof TLRPC.TL_sendMessageUploadPhotoAction) {
            return LocaleController.getString("UserStatusLogActionUploadingPhoto", R.string.UserStatusLogActionUploadingPhoto);
        } else if (action instanceof TLRPC.TL_sendMessageUploadDocumentAction) {
            return LocaleController.getString("UserStatusLogActionUploadingDocument", R.string.UserStatusLogActionUploadingDocument);
        } else if (action instanceof TLRPC.TL_sendMessageUploadRoundAction) {
            return LocaleController.getString("UserStatusLogActionUploadingRound", R.string.UserStatusLogActionUploadingRound);
        } else if (action instanceof TLRPC.TL_sendMessageRecordRoundAction) {
            return LocaleController.getString("UserStatusLogActionRecordingRound", R.string.UserStatusLogActionRecordingRound);
        } else if (action instanceof TLRPC.TL_sendMessageGamePlayAction) {
            return LocaleController.getString("UserStatusLogActionPlayingGame", R.string.UserStatusLogActionPlayingGame);
        } else if (action instanceof TLRPC.TL_sendMessageGeoLocationAction) {
            return LocaleController.getString("UserStatusLogActionSendingLocation", R.string.UserStatusLogActionSendingLocation);
        } else if (action instanceof TLRPC.TL_sendMessageChooseContactAction) {
            return LocaleController.getString("UserStatusLogActionChoosingContact", R.string.UserStatusLogActionChoosingContact);
        } else if (action instanceof TLRPC.TL_sendMessageChooseStickerAction) {
            return LocaleController.getString("UserStatusLogActionChoosingSticker", R.string.UserStatusLogActionChoosingSticker);
        } else if (action instanceof TLRPC.TL_sendMessageEmojiInteraction) {
            return LocaleController.getString("UserStatusLogActionEmoji", R.string.UserStatusLogActionEmoji);
        } else if (action instanceof TLRPC.TL_sendMessageEmojiInteractionSeen) {
            return LocaleController.getString("UserStatusLogActionEmojiSeen", R.string.UserStatusLogActionEmojiSeen);
        } else if (action instanceof TLRPC.TL_sendMessageHistoryImportAction) {
            return LocaleController.getString("UserStatusLogActionImportingHistory", R.string.UserStatusLogActionImportingHistory);
        } else if (action instanceof TLRPC.TL_sendMessageCancelAction) {
            return LocaleController.getString("UserStatusLogActionCancelling", R.string.UserStatusLogActionCancelling);
        }
        return action.getClass().getSimpleName();
    }
}