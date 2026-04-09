package org.ushastoe.fluffy.patches;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.ushastoe.fluffy.receivers.NotificationReactionReceiver;

import java.util.HashMap;

public final class NotificationReactionPatch {

    private static final String HEART_REACTION = "❤";
    private static final String LOG_PREFIX = "fluffy_notification_diag";

    private NotificationReactionPatch() {
    }

    private static void log(String message) {
        if (!NotificationDiagnosticsSettingsPatch.isNotificationDiagnosticsEnabled()) {
            return;
        }
        FileLog.d(LOG_PREFIX + " " + message);
    }

    public static NotificationCompat.Action createHeartReactionAction(MessageObject messageObject, long dialogId, int maxId, long topicId, int currentAccount, int requestCode, boolean waitingForPasscode, boolean isStory) {
        if (!shouldShowHeartReactionAction(messageObject, dialogId, currentAccount, waitingForPasscode, isStory)) {
            log("reactionAction:skip account=" + currentAccount + " dialogId=" + dialogId + " messageId=" + maxId + " waitingForPasscode=" + waitingForPasscode + " isStory=" + isStory);
            return null;
        }

        Context context = ApplicationLoader.applicationContext;
        Intent reactionIntent = new Intent(context, NotificationReactionReceiver.class);
        reactionIntent.putExtra("dialog_id", dialogId);
        reactionIntent.putExtra("max_id", maxId);
        reactionIntent.putExtra("topic_id", topicId);
        reactionIntent.putExtra("currentAccount", currentAccount);
        PendingIntent reactionPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                reactionIntent,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        log("reactionAction:add account=" + currentAccount + " dialogId=" + dialogId + " messageId=" + maxId + " topicId=" + topicId);
        return new NotificationCompat.Action.Builder(R.drawable.msg_input_like, HEART_REACTION, reactionPendingIntent)
                .setShowsUserInterface(false)
                .build();
    }

    public static void handleHeartReactionIntent(long dialogId, int maxId, long topicId, int currentAccount) {
        log("reactionIntent:received account=" + currentAccount + " dialogId=" + dialogId + " messageId=" + maxId + " topicId=" + topicId);
        if (dialogId == 0 || maxId == 0 || !UserConfig.isValidAccount(currentAccount)) {
            log("reactionIntent:skip_invalid account=" + currentAccount + " dialogId=" + dialogId + " messageId=" + maxId);
            return;
        }
        AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
        ensureDialogPeerLoaded(accountInstance, dialogId, () -> sendHeartReaction(accountInstance, dialogId, maxId, topicId));
    }

    private static void sendHeartReaction(AccountInstance accountInstance, long dialogId, int maxId, long topicId) {
        TLRPC.Message storedMessage = accountInstance.getMessagesStorage().getMessage(dialogId, maxId);
        if (storedMessage == null) {
            log("reactionSend:message_not_found account=" + accountInstance.getCurrentAccount() + " dialogId=" + dialogId + " messageId=" + maxId);
            return;
        }
        AndroidUtilities.runOnUIThread(() -> {
            MessageObject messageObject = new MessageObject(accountInstance.getCurrentAccount(), storedMessage, false, false);
            if (!shouldAllowHeartReaction(messageObject, dialogId, accountInstance.getCurrentAccount())) {
                log("reactionSend:not_allowed account=" + accountInstance.getCurrentAccount() + " dialogId=" + dialogId + " messageId=" + maxId);
                return;
            }
            TLRPC.TL_messages_sendReaction req = new TLRPC.TL_messages_sendReaction();
            if (messageObject.messageOwner.isThreadMessage && messageObject.messageOwner.fwd_from != null) {
                req.peer = accountInstance.getMessagesController().getInputPeer(messageObject.getFromChatId());
                req.msg_id = messageObject.messageOwner.fwd_from.saved_from_msg_id;
            } else {
                req.peer = accountInstance.getMessagesController().getInputPeer(messageObject.getDialogId());
                req.msg_id = messageObject.getId();
            }
            if (req.peer == null) {
                log("reactionSend:peer_null account=" + accountInstance.getCurrentAccount() + " dialogId=" + dialogId + " messageId=" + maxId);
                return;
            }
            TLRPC.TL_reactionEmoji reaction = new TLRPC.TL_reactionEmoji();
            reaction.emoticon = HEART_REACTION;
            req.reaction.add(reaction);
            req.flags |= 1;
            log("reactionSend:request account=" + accountInstance.getCurrentAccount() + " dialogId=" + dialogId + " messageId=" + req.msg_id + " topicId=" + topicId);
            accountInstance.getConnectionsManager().sendRequest(req, (response, error) -> {
                if (response instanceof TLRPC.Updates) {
                    log("reactionSend:success account=" + accountInstance.getCurrentAccount() + " dialogId=" + dialogId + " messageId=" + req.msg_id);
                    accountInstance.getMessagesController().processUpdates((TLRPC.Updates) response, false);
                } else if (error != null) {
                    log("reactionSend:error account=" + accountInstance.getCurrentAccount() + " dialogId=" + dialogId + " messageId=" + req.msg_id + " error=" + error.text);
                } else {
                    log("reactionSend:empty_response account=" + accountInstance.getCurrentAccount() + " dialogId=" + dialogId + " messageId=" + req.msg_id);
                }
            });
            accountInstance.getMessagesController().markDialogAsRead(dialogId, maxId, maxId, 0, false, topicId, 0, true, 0);
            accountInstance.getMessagesController().markReactionsAsRead(dialogId, topicId);
        });
    }

    private static boolean shouldShowHeartReactionAction(MessageObject messageObject, long dialogId, int currentAccount, boolean waitingForPasscode, boolean isStory) {
        if (waitingForPasscode || isStory || messageObject == null) {
            return false;
        }
        return shouldAllowHeartReaction(messageObject, dialogId, currentAccount);
    }

    private static boolean shouldAllowHeartReaction(MessageObject messageObject, long dialogId, int currentAccount) {
        if (messageObject == null || !messageObject.isReactionsAvailable() || DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        }
        HashMap<String, TLRPC.TL_availableReaction> reactionsMap = MediaDataController.getInstance(currentAccount).getReactionsMap();
        if (reactionsMap != null && !reactionsMap.isEmpty() && !reactionsMap.containsKey(HEART_REACTION)) {
            return false;
        }
        if (DialogObject.isUserDialog(dialogId)) {
            return true;
        }
        if (!DialogObject.isChatDialog(dialogId)) {
            return false;
        }
        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        if (chat == null) {
            return false;
        }
        TLRPC.ChatFull chatFull = MessagesController.getInstance(currentAccount).getChatFull(chat.id);
        if (chatFull == null) {
            return !ChatObject.isChannel(chat);
        }
        if (chatFull.available_reactions instanceof TLRPC.TL_chatReactionsNone) {
            return false;
        }
        if (chatFull.available_reactions instanceof TLRPC.TL_chatReactionsSome) {
            return ChatObject.reactionIsAvailable(chatFull, HEART_REACTION);
        }
        return true;
    }

    private static void ensureDialogPeerLoaded(AccountInstance accountInstance, long dialogId, Runnable onLoaded) {
        if (accountInstance == null || onLoaded == null) {
            return;
        }
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = accountInstance.getMessagesController().getUser(dialogId);
            if (user != null) {
                onLoaded.run();
                return;
            }
            org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                TLRPC.User loadedUser = accountInstance.getMessagesStorage().getUserSync(dialogId);
                AndroidUtilities.runOnUIThread(() -> {
                    accountInstance.getMessagesController().putUser(loadedUser, true);
                    onLoaded.run();
                });
            });
            return;
        }
        if (DialogObject.isChatDialog(dialogId)) {
            TLRPC.Chat chat = accountInstance.getMessagesController().getChat(-dialogId);
            if (chat != null) {
                onLoaded.run();
                return;
            }
            org.telegram.messenger.Utilities.globalQueue.postRunnable(() -> {
                TLRPC.Chat loadedChat = accountInstance.getMessagesStorage().getChatSync(-dialogId);
                AndroidUtilities.runOnUIThread(() -> {
                    accountInstance.getMessagesController().putChat(loadedChat, true);
                    onLoaded.run();
                });
            });
            return;
        }
        onLoaded.run();
    }
}
