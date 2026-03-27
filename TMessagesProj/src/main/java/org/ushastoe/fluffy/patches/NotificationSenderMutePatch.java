package org.ushastoe.fluffy.patches;

import android.text.TextUtils;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.ushastoe.fluffy.hooks.NotificationSenderMuteHook;
import org.ushastoe.fluffy.utils.NotificationMutedSenderStore;

import java.util.ArrayList;

public final class NotificationSenderMutePatch {

    public static final int OPTION_TOGGLE_SENDER_NOTIFICATIONS = 9998;

    private NotificationSenderMutePatch() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, int currentAccount, MessageObject selectedMessage) {
        if (items == null || options == null || icons == null || !canToggle(currentAccount, selectedMessage)) {
            return;
        }
        boolean muted = isMuted(currentAccount, selectedMessage);
        items.add(LocaleController.getString(muted ? R.string.FluffyUnmuteSenderNotifications : R.string.FluffyMuteSenderNotifications));
        options.add(OPTION_TOGGLE_SENDER_NOTIFICATIONS);
        icons.add(muted ? R.drawable.msg_unmute : R.drawable.msg_mute);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        if (option != OPTION_TOGGLE_SENDER_NOTIFICATIONS || fragment == null || selectedMessage == null) {
            return false;
        }
        int currentAccount = fragment.getCurrentAccount();
        if (!canToggle(currentAccount, selectedMessage)) {
            return true;
        }
        long dialogId = selectedMessage.getDialogId();
        long senderId = getSenderPeerId(selectedMessage);
        boolean muted = isMuted(currentAccount, selectedMessage);
        if (!NotificationMutedSenderStore.setMuted(currentAccount, dialogId, senderId, !muted)) {
            return true;
        }
        if (!muted) {
            NotificationSenderMuteHook.removeQueuedMessagesForMutedSender(currentAccount, dialogId, senderId);
        }

        String senderName = getSenderName(currentAccount, senderId);
        String text;
        if (muted) {
            text = TextUtils.isEmpty(senderName)
                    ? LocaleController.getString(R.string.FluffySenderNotificationsUnmuted)
                    : LocaleController.formatString(R.string.FluffySenderNotificationsUnmutedNamed, senderName);
        } else {
            text = TextUtils.isEmpty(senderName)
                    ? LocaleController.getString(R.string.FluffySenderNotificationsMuted)
                    : LocaleController.formatString(R.string.FluffySenderNotificationsMutedNamed, senderName);
        }
        BulletinFactory.of(fragment).createSimpleBulletin(muted ? R.raw.ic_unmute : R.raw.ic_mute, text).show();
        return true;
    }

    public static boolean shouldSuppressNotification(int currentAccount, MessageObject messageObject) {
        if (!canToggle(currentAccount, messageObject)) {
            return false;
        }
        return isMuted(currentAccount, messageObject);
    }

    public static boolean isQueuedMessageForSender(int currentAccount, MessageObject messageObject, long dialogId, long senderId) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        if (messageObject.getDialogId() != dialogId) {
            return false;
        }
        return getSenderPeerId(messageObject) == senderId;
    }

    private static boolean canToggle(int currentAccount, MessageObject selectedMessage) {
        if (selectedMessage == null || selectedMessage.messageOwner == null) {
            return false;
        }
        long dialogId = selectedMessage.getDialogId();
        if (!DialogObject.isChatDialog(dialogId)) {
            return false;
        }
        long senderId = getSenderPeerId(selectedMessage);
        if (senderId == 0 || senderId == UserConfig.getInstance(currentAccount).getClientUserId()) {
            return false;
        }
        if (selectedMessage.messageOwner.action != null || selectedMessage.isOutOwner()) {
            return false;
        }
        return true;
    }

    private static boolean isMuted(int currentAccount, MessageObject selectedMessage) {
        long dialogId = selectedMessage.getDialogId();
        long senderId = getSenderPeerId(selectedMessage);
        return NotificationMutedSenderStore.isMuted(currentAccount, dialogId, senderId);
    }

    private static long getSenderPeerId(MessageObject messageObject) {
        TLRPC.Peer fromId = messageObject != null && messageObject.messageOwner != null ? messageObject.messageOwner.from_id : null;
        long senderId = fromId != null ? DialogObject.getPeerDialogId(fromId) : 0;
        if (senderId == 0 && messageObject != null) {
            senderId = messageObject.getFromChatId();
        }
        return senderId;
    }

    private static String getSenderName(int currentAccount, long senderId) {
        return DialogObject.getShortName(currentAccount, senderId);
    }
}
