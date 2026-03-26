package org.ushastoe.fluffy.patches;

import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.utils.LocalMessageArchiveStore;

public final class LocalMessageArchivePatch {

    private LocalMessageArchivePatch() {
    }

    public static void captureServerEdit(TLRPC.Message oldMessage, TLRPC.Message newMessage) {
        if (!PremiumSettingsPatch.isLocalMessageHistoryEnabled() || oldMessage == null || newMessage == null) {
            return;
        }
        String oldText = oldMessage.message;
        String newText = newMessage.message;
        if (TextUtils.isEmpty(oldText) || TextUtils.equals(oldText, newText)) {
            return;
        }
        long dialogId = newMessage.dialog_id != 0 ? newMessage.dialog_id : oldMessage.dialog_id;
        int savedAt = Math.max(newMessage.edit_date, newMessage.date);
        LocalMessageArchiveStore.appendSnapshot(dialogId, newMessage.id, oldText, savedAt, LocalMessageArchiveStore.SOURCE_SERVER_EDIT);
    }

    public static boolean preserveDeletedMessage(ChatActivity fragment, MessageObject messageObject) {
        if (!PremiumSettingsPatch.isSaveDeletedMessagesEnabled() || fragment == null || messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        if (messageObject.scheduled || messageObject.messageOwner.id <= 0) {
            return false;
        }
        String text = getArchivedText(messageObject);
        if (!TextUtils.isEmpty(text)) {
            LocalMessageArchiveStore.appendSnapshot(messageObject.messageOwner.dialog_id, messageObject.getId(),
                    text, fragment.getConnectionsManager().getCurrentTime(), LocalMessageArchiveStore.SOURCE_DELETED);
        }
        messageObject.deleted = false;
        messageObject.forceUpdate = true;
        messageObject.generateCaption();
        messageObject.updateMessageText();
        messageObject.resetLayout();
        return true;
    }

    public static void captureLocalEdit(MessageObject messageObject, String previousText, boolean reset) {
        if (!PremiumSettingsPatch.isLocalMessageHistoryEnabled() || messageObject == null || messageObject.messageOwner == null || TextUtils.isEmpty(previousText)) {
            return;
        }
        int savedAt = Math.max(messageObject.messageOwner.edit_date, messageObject.messageOwner.date);
        LocalMessageArchiveStore.appendSnapshot(messageObject.messageOwner.dialog_id, messageObject.getId(), previousText,
                savedAt, reset ? LocalMessageArchiveStore.SOURCE_LOCAL_EDIT_RESET : LocalMessageArchiveStore.SOURCE_LOCAL_EDIT);
    }

    public static boolean hasLocalHistory(MessageObject messageObject) {
        return messageObject != null && messageObject.messageOwner != null
                && LocalMessageArchiveStore.hasRecords(messageObject.messageOwner.dialog_id, messageObject.getId());
    }

    public static String getArchiveEntryTitle(LocalMessageArchiveStore.Entry entry) {
        if (entry == null) {
            return "";
        }
        String date = entry.savedAt > 0 ? LocaleController.formatDateChat(entry.savedAt) : "";
        if (LocalMessageArchiveStore.SOURCE_SERVER_EDIT.equals(entry.source)) {
            return date;
        }
        int resId;
        if (LocalMessageArchiveStore.SOURCE_DELETED.equals(entry.source)) {
            resId = R.string.FluffyLocalMessageHistoryDeletedEntry;
        } else if (LocalMessageArchiveStore.SOURCE_LOCAL_EDIT_RESET.equals(entry.source)) {
            resId = R.string.FluffyLocalMessageHistoryResetEntry;
        } else if (LocalMessageArchiveStore.SOURCE_LOCAL_EDIT.equals(entry.source)) {
            resId = R.string.FluffyLocalMessageHistoryLocalEditEntry;
        } else {
            resId = R.string.FluffyLocalMessageHistoryEditedEntry;
        }
        return date.isEmpty() ? LocaleController.getString(resId) : LocaleController.getString(resId) + " " + date;
    }

    public static String getCurrentText(MessageObject messageObject) {
        return getArchivedText(messageObject);
    }

    public static boolean isLocallyDeleted(MessageObject messageObject) {
        return messageObject != null && messageObject.messageOwner != null
                && LocalMessageArchiveStore.hasDeletedSnapshot(messageObject.messageOwner.dialog_id, messageObject.getId());
    }

    private static String getArchivedText(MessageObject messageObject) {
        if (messageObject == null) {
            return "";
        }
        if (!TextUtils.isEmpty(messageObject.messageOwner != null ? messageObject.messageOwner.message : null)) {
            return messageObject.messageOwner.message;
        }
        CharSequence messageText = messageObject.messageText;
        return messageText != null ? messageText.toString() : "";
    }
}
