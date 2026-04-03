package org.ushastoe.fluffy.patches;

import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.messenger.R;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;

import java.util.ArrayList;
import java.util.WeakHashMap;

public final class PendingSendMessageEditPatch {

    public static final int OPTION_EDIT_PENDING_SEND = 10001;

    private static final WeakHashMap<ChatActivity, EditSession> ACTIVE_SESSIONS = new WeakHashMap<>();

    private PendingSendMessageEditPatch() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (items == null || options == null || icons == null || !canEditPendingSend(selectedMessage)) {
            return;
        }
        items.add(LocaleController.getString(R.string.Edit));
        options.add(OPTION_EDIT_PENDING_SEND);
        icons.add(R.drawable.msg_edit);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        if (option != OPTION_EDIT_PENDING_SEND || fragment == null || !canEditPendingSend(selectedMessage)) {
            return false;
        }
        ACTIVE_SESSIONS.put(fragment, new EditSession(selectedMessage));
        fragment.startLocalPendingEditComposer(selectedMessage);
        return true;
    }

    public static boolean handleComposerDoneEditing(ChatActivity fragment, MessageObject editingMessageObject,
            CharSequence text, ArrayList<TLRPC.MessageEntity> entities) {
        if (fragment == null || editingMessageObject == null) {
            return false;
        }
        EditSession session = ACTIVE_SESSIONS.get(fragment);
        if (!isSameMessage(session, editingMessageObject)) {
            return false;
        }
        ACTIVE_SESSIONS.remove(fragment);
        applyPendingEdit(editingMessageObject, text != null ? text.toString() : "", entities);
        fragment.updateVisibleRows();
        return true;
    }

    public static void onEditingSessionChanged(ChatActivity fragment, MessageObject previousEditingMessageObject,
            MessageObject currentEditingMessageObject) {
        if (fragment == null) {
            return;
        }
        EditSession session = ACTIVE_SESSIONS.get(fragment);
        if (session == null) {
            return;
        }
        if (currentEditingMessageObject == null || !isSameMessage(session, currentEditingMessageObject)) {
            ACTIVE_SESSIONS.remove(fragment);
        }
    }

    private static boolean canEditPendingSend(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        if (!messageObject.isSending() || !messageObject.isOutOwner() || messageObject.isSendError() || messageObject.isSponsored()) {
            return false;
        }
        if (messageObject.messageOwner.action != null) {
            return false;
        }
        return messageObject.type == MessageObject.TYPE_TEXT || messageObject.type == MessageObject.TYPE_EMOJIS;
    }

    private static void applyPendingEdit(MessageObject messageObject, String newText, ArrayList<TLRPC.MessageEntity> entities) {
        if (!canEditPendingSend(messageObject)) {
            return;
        }
        String normalizedText = newText != null ? newText : "";
        if (TextUtils.equals(normalizedText, messageObject.messageOwner.message)
                && entitiesEqual(messageObject.messageOwner.entities, entities)) {
            return;
        }
        messageObject.messageOwner.message = normalizedText;
        messageObject.messageOwner.entities = ensureEntitiesList(cloneEntities(entities));
        messageObject.generateCaption();
        messageObject.updateMessageText();
        messageObject.resetLayout();

        ArrayList<TLRPC.Message> messages = new ArrayList<>(1);
        messages.add(messageObject.messageOwner);
        MessagesStorage.getInstance(messageObject.currentAccount).putMessages(messages, false, true, false, 0, false, 0, 0);
        reschedulePendingSend(messageObject);
    }

    private static void reschedulePendingSend(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null || !messageObject.isSending()) {
            return;
        }
        if (messageObject.messageOwner.reqId != 0) {
            ConnectionsManager.getInstance(messageObject.currentAccount).cancelRequest(messageObject.messageOwner.reqId, true);
            messageObject.messageOwner.reqId = 0;
        }
        SendMessagesHelper.getInstance(messageObject.currentAccount).retrySendMessage(messageObject, false, 0);
    }

    private static boolean entitiesEqual(ArrayList<TLRPC.MessageEntity> left, ArrayList<TLRPC.MessageEntity> right) {
        if (left == null || left.isEmpty()) {
            return right == null || right.isEmpty();
        }
        if (right == null || left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            TLRPC.MessageEntity l = left.get(i);
            TLRPC.MessageEntity r = right.get(i);
            if (l == null || r == null) {
                if (l != r) {
                    return false;
                }
                continue;
            }
            if (l.getClass() != r.getClass() || l.offset != r.offset || l.length != r.length) {
                return false;
            }
        }
        return true;
    }

    private static ArrayList<TLRPC.MessageEntity> cloneEntities(ArrayList<TLRPC.MessageEntity> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        ArrayList<TLRPC.MessageEntity> result = new ArrayList<>(source.size());
        for (int i = 0; i < source.size(); i++) {
            TLRPC.MessageEntity entity = source.get(i);
            if (entity == null) {
                continue;
            }
            try {
                SerializedData data = new SerializedData(entity.getObjectSize());
                entity.serializeToStream(data);
                byte[] bytes = data.toByteArray();
                data.cleanup();
                SerializedData reader = new SerializedData(bytes);
                TLRPC.MessageEntity copy = TLRPC.MessageEntity.TLdeserialize(reader, reader.readInt32(true), true);
                reader.cleanup();
                if (copy != null) {
                    result.add(copy);
                }
            } catch (Exception ignore) {
            }
        }
        return result.isEmpty() ? null : result;
    }

    private static ArrayList<TLRPC.MessageEntity> ensureEntitiesList(ArrayList<TLRPC.MessageEntity> entities) {
        return entities != null ? entities : new ArrayList<>();
    }

    private static boolean isSameMessage(EditSession session, MessageObject messageObject) {
        return session != null && messageObject != null
                && session.dialogId == messageObject.getDialogId()
                && session.messageId == messageObject.getId();
    }

    private static final class EditSession {
        private final long dialogId;
        private final int messageId;

        private EditSession(MessageObject messageObject) {
            dialogId = messageObject != null ? messageObject.getDialogId() : 0L;
            messageId = messageObject != null ? messageObject.getId() : 0;
        }
    }
}
