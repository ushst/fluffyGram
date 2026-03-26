package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.AlertsCreator;
import org.ushastoe.fluffy.hooks.LocalMessageArchiveHook;
import org.ushastoe.fluffy.utils.LocalMessageFakeEditStore;

import java.util.ArrayList;

public final class LocalMessageFakeEditPatch {

    public static final int OPTION_LOCAL_FAKE_EDIT = 9994;
    public static final int OPTION_RESET_LOCAL_FAKE_EDIT = 9995;

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
            showFakeEditDialog(fragment, selectedMessage);
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
        if (!fakeEdited && messageObject.isEdited()) {
            return false;
        }
        if (!fakeEdited && TextUtils.isEmpty(messageObject.messageOwner.message)) {
            return false;
        }
        return !messageObject.isSponsored();
    }

    private static void showFakeEditDialog(ChatActivity fragment, MessageObject messageObject) {
        if (!PremiumSettingsPatch.isLocalMessageFakeEditEnabled() || !canFakeEdit(messageObject) || fragment.getParentActivity() == null) {
            return;
        }
        Context context = fragment.getParentActivity();
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setText(messageObject.messageOwner.message != null ? messageObject.messageOwner.message : "");
        editText.setSelection(editText.length());
        editText.setGravity(Gravity.START | Gravity.TOP);
        editText.setMinLines(1);
        editText.setMaxLines(5);
        editText.setPadding(0, 0, 0, AndroidUtilities.dp(6));
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyLocalMessageEditTitle));
        builder.setMessage(LocaleController.getString(R.string.FluffyLocalMessageEditHint));
        builder.setView(frameLayout);
        builder.setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> applyFakeEdit(fragment, messageObject, editText.getText().toString()));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        fragment.showDialog(dialog);
        editText.requestFocus();
        AndroidUtilities.runOnUIThread(() -> AndroidUtilities.showKeyboard(editText));
    }

    private static void applyFakeEdit(ChatActivity fragment, MessageObject messageObject, String newText) {
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
        messageObject.messageOwner.entities = cloneEntities(record.originalEntities);

        LocalMessageFakeEditStore.remove(messageObject.messageOwner.dialog_id, messageObject.getId());
        refreshMessageObject(fragment, messageObject);
    }

    private static void applyRecord(MessageObject messageObject, LocalMessageFakeEditStore.Record record) {
        messageObject.messageOwner.message = record.fakeText != null ? record.fakeText : "";
        messageObject.messageOwner.flags |= TLRPC.MESSAGE_FLAG_EDITED;
        messageObject.messageOwner.edit_date = record.fakeEditDate;
        messageObject.messageOwner.edit_hide = false;
        messageObject.messageOwner.entities = new ArrayList<>();
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
}
