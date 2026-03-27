package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.text.TextUtils;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.AlertsCreator;
import org.ushastoe.fluffy.hooks.LocalMessageArchiveHook;
import org.ushastoe.fluffy.utils.LocalMessageFakeEditStore;

import java.util.ArrayList;
import java.util.WeakHashMap;

public final class LocalMessageFakeEditPatch {

    public static final int OPTION_LOCAL_FAKE_EDIT = 9994;
    public static final int OPTION_RESET_LOCAL_FAKE_EDIT = 9995;

    private static final WeakHashMap<ChatActivity, EditSession> ACTIVE_SESSIONS = new WeakHashMap<>();

    private LocalMessageFakeEditPatch() {
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (items == null || options == null || icons == null || selectedMessage == null || selectedMessage.messageOwner == null) {
            return;
        }
        if (!PremiumSettingsPatch.isLocalMessageFakeEditEnabled()) {
            return;
        }
        if (canFakeEdit(selectedMessage)) {
            items.add(LocaleController.getString(R.string.FluffyEditMessageLocally));
            options.add(OPTION_LOCAL_FAKE_EDIT);
            icons.add(R.drawable.msg_edit);
        }
        if (isFakeEdited(selectedMessage)) {
            items.add(LocaleController.getString(R.string.FluffyResetLocalMessageEdit));
            options.add(OPTION_RESET_LOCAL_FAKE_EDIT);
            icons.add(R.drawable.msg_reset);
        }
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        if (fragment == null || selectedMessage == null || selectedMessage.messageOwner == null) {
            return false;
        }
        if (option == OPTION_LOCAL_FAKE_EDIT) {
            startFakeEditComposer(fragment, selectedMessage);
            return true;
        }
        if (option == OPTION_RESET_LOCAL_FAKE_EDIT) {
            resetFakeEdit(fragment, selectedMessage);
            return true;
        }
        return false;
    }

    public static void applyStoredEdit(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        LocalMessageFakeEditStore.Record record = LocalMessageFakeEditStore.get(messageObject.messageOwner.dialog_id, messageObject.getId());
        if (record == null) {
            return;
        }
        applyRecord(messageObject, record);
    }

    public static boolean isFakeEdited(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        return LocalMessageFakeEditStore.get(messageObject.messageOwner.dialog_id, messageObject.getId()) != null;
    }

    private static boolean canFakeEdit(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        boolean fakeEdited = isFakeEdited(messageObject);
        if (messageObject.messageOwner instanceof TLRPC.TL_messageService) {
            return false;
        }
        if (!fakeEdited && messageObject.isEdited() && !isBroadcastChannelMessage(messageObject)) {
            return false;
        }
        if (!fakeEdited && TextUtils.isEmpty(messageObject.messageOwner.message)) {
            return false;
        }
        return !messageObject.isSponsored();
    }

    private static boolean isBroadcastChannelMessage(MessageObject messageObject) {
        if (messageObject == null) {
            return false;
        }
        long dialogId = messageObject.getDialogId();
        if (dialogId >= 0) {
            return false;
        }
        TLRPC.Chat chat = MessagesController.getInstance(messageObject.currentAccount).getChat(-dialogId);
        return chat != null && ChatObject.isChannel(chat) && !chat.megagroup;
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
        applyFakeEdit(fragment, editingMessageObject, text != null ? text.toString() : "", entities);
        ACTIVE_SESSIONS.remove(fragment);
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

    private static void startFakeEditComposer(ChatActivity fragment, MessageObject messageObject) {
        if (!PremiumSettingsPatch.isLocalMessageFakeEditEnabled() || !canFakeEdit(messageObject) || fragment == null) {
            return;
        }
        ACTIVE_SESSIONS.put(fragment, new EditSession(messageObject));
        fragment.startLocalFakeEditComposer(messageObject);
    }

    private static void applyFakeEdit(ChatActivity fragment, MessageObject messageObject, String newText,
            ArrayList<TLRPC.MessageEntity> entities) {
        if (fragment == null || messageObject == null || messageObject.messageOwner == null || newText == null) {
            return;
        }
        if (!canFakeEdit(messageObject)) {
            return;
        }
        String originalText = messageObject.messageOwner.message != null ? messageObject.messageOwner.message : "";
        LocalMessageFakeEditStore.Record existingRecord = LocalMessageFakeEditStore.get(messageObject.messageOwner.dialog_id, messageObject.getId());
        if (existingRecord != null && TextUtils.equals(existingRecord.fakeText, newText)) {
            return;
        }
        if (existingRecord == null && TextUtils.equals(originalText, newText)) {
            return;
        }

        LocalMessageFakeEditStore.Record record = existingRecord != null ? existingRecord : new LocalMessageFakeEditStore.Record();
        if (existingRecord == null) {
            record.originalText = originalText;
            record.originalFlags = messageObject.messageOwner.flags;
            record.originalEditDate = messageObject.messageOwner.edit_date;
            record.originalEditHide = messageObject.messageOwner.edit_hide;
            record.originalEntities = cloneEntities(messageObject.messageOwner.entities);
        }
        LocalMessageArchiveHook.captureLocalEdit(messageObject, originalText, false);
        record.fakeText = newText;
        record.fakeEditDate = fragment.getConnectionsManager().getCurrentTime();
        record.fakeEntities = cloneEntities(entities);
        LocalMessageFakeEditStore.put(messageObject.messageOwner.dialog_id, messageObject.getId(), record);

        applyRecord(messageObject, record);
        refreshMessageObject(fragment, messageObject);
    }

    private static void resetFakeEdit(ChatActivity fragment, MessageObject messageObject) {
        if (fragment == null || messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        LocalMessageFakeEditStore.Record record = LocalMessageFakeEditStore.get(messageObject.messageOwner.dialog_id, messageObject.getId());
        if (record == null) {
            return;
        }
        if (record.originalText == null) {
            String errorText = LocaleController.getString(R.string.FluffyLocalMessageEditResetFailed);
            if (BulletinFactory.canShowBulletin(fragment)) {
                BulletinFactory.of(fragment).createErrorBulletin(errorText, fragment.getResourceProvider()).show();
            } else {
                AlertsCreator.showSimpleToast(fragment, errorText);
            }
            return;
        }

        LocalMessageArchiveHook.captureLocalEdit(messageObject, messageObject.messageOwner.message, true);
        messageObject.messageOwner.message = record.originalText;
        messageObject.messageOwner.flags = record.originalFlags;
        messageObject.messageOwner.edit_date = record.originalEditDate;
        messageObject.messageOwner.edit_hide = record.originalEditHide;
        messageObject.messageOwner.entities = ensureEntitiesList(cloneEntities(record.originalEntities));

        LocalMessageFakeEditStore.remove(messageObject.messageOwner.dialog_id, messageObject.getId());
        refreshMessageObject(fragment, messageObject);
    }

    private static void applyRecord(MessageObject messageObject, LocalMessageFakeEditStore.Record record) {
        messageObject.messageOwner.message = record.fakeText != null ? record.fakeText : "";
        messageObject.messageOwner.flags |= TLRPC.MESSAGE_FLAG_EDITED;
        messageObject.messageOwner.edit_date = record.fakeEditDate;
        messageObject.messageOwner.edit_hide = false;
        messageObject.messageOwner.entities = ensureEntitiesList(cloneEntities(record.fakeEntities));
    }

    private static void refreshMessageObject(ChatActivity fragment, MessageObject messageObject) {
        messageObject.generateCaption();
        messageObject.updateMessageText();
        messageObject.resetLayout();
        fragment.updateVisibleRows();
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

    private static boolean isSameMessage(EditSession session, MessageObject messageObject) {
        return session != null && messageObject != null && session.dialogId == messageObject.getDialogId() && session.messageId == messageObject.getId();
    }

    private static ArrayList<TLRPC.MessageEntity> ensureEntitiesList(ArrayList<TLRPC.MessageEntity> entities) {
        return entities != null ? entities : new ArrayList<>();
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
