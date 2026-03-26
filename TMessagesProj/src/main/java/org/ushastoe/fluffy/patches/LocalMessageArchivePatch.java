package org.ushastoe.fluffy.patches;

import android.text.TextUtils;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.utils.LocalMessageArchiveStore;

import java.util.HashMap;

public final class LocalMessageArchivePatch {
    private static final String PARAM_HISTORY_DELETED_OVERRIDE = "fl_history_deleted_override";

    private LocalMessageArchivePatch() {
    }

    public static boolean shouldCaptureDeletedMessages() {
        return PremiumSettingsPatch.isSaveDeletedMessagesEnabled();
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

    public static void captureDeletedMessage(TLRPC.Message message, long topicId) {
        if (!PremiumSettingsPatch.isSaveDeletedMessagesEnabled() || message == null || message.id <= 0) {
            return;
        }
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("fluffy_local_archive capture_deleted dialogId=" + message.dialog_id + " messageId=" + message.id + " topicId=" + topicId);
        }
        String text = !TextUtils.isEmpty(message.message) ? message.message : "";
        if (!TextUtils.isEmpty(text)) {
            LocalMessageArchiveStore.appendSnapshot(message.dialog_id, message.id,
                    text, Math.max(message.edit_date, message.date), LocalMessageArchiveStore.SOURCE_DELETED);
        }
        LocalMessageArchiveStore.putDeletedSnapshot(message, topicId);
    }

    public static void restoreDeletedMessages(long dialogId, long topicId, int mode, java.util.ArrayList<TLRPC.Message> messages) {
        if (!PremiumSettingsPatch.isSaveDeletedMessagesEnabled()) {
            return;
        }
        int before = messages != null ? messages.size() : -1;
        LocalMessageArchiveStore.restoreDeletedMessages(dialogId, topicId, mode, messages);
        if (BuildVars.LOGS_ENABLED) {
            int after = messages != null ? messages.size() : -1;
            FileLog.d("fluffy_local_archive restore_deleted dialogId=" + dialogId + " topicId=" + topicId + " mode=" + mode + " before=" + before + " after=" + after);
        }
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
        if (LocalMessageArchiveStore.SOURCE_SERVER_EDIT.equals(entry.source)
                || LocalMessageArchiveStore.SOURCE_DELETED.equals(entry.source)) {
            return date;
        }
        int resId;
        if (LocalMessageArchiveStore.SOURCE_LOCAL_EDIT_RESET.equals(entry.source)) {
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
        if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.params != null) {
            String override = messageObject.messageOwner.params.get(PARAM_HISTORY_DELETED_OVERRIDE);
            if (override != null) {
                return "1".equals(override);
            }
        }
        return messageObject != null && messageObject.messageOwner != null
                && LocalMessageArchiveStore.hasDeletedSnapshot(messageObject.messageOwner.dialog_id, messageObject.getId());
    }

    public static void setHistoryDeletedOverride(MessageObject messageObject, boolean deleted) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        if (messageObject.messageOwner.params == null) {
            messageObject.messageOwner.params = new HashMap<>();
        }
        messageObject.messageOwner.params.put(PARAM_HISTORY_DELETED_OVERRIDE, deleted ? "1" : "0");
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
