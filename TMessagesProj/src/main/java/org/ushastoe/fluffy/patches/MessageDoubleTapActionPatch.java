package org.ushastoe.fluffy.patches;

import android.view.View;

import androidx.annotation.Nullable;

import org.telegram.messenger.MessageObject;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;

public final class MessageDoubleTapActionPatch {

    private MessageDoubleTapActionPatch() {
    }

    @Nullable
    public static Boolean hasDoubleTapOverride(ChatActivity chatActivity, View view) {
        MessageObject messageObject = getDoubleTapMessage(chatActivity, view);
        if (messageObject == null) {
            return null;
        }
        int action = getConfiguredAction(messageObject);
        if (action == AppearanceSettingsPatch.DOUBLE_TAP_ACTION_NONE) {
            return Boolean.FALSE;
        }
        if (action == AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REACTION) {
            return null;
        }
        return canPerformAction(chatActivity, messageObject, action) ? Boolean.TRUE : Boolean.FALSE;
    }

    public static boolean onDoubleTap(ChatActivity chatActivity, View view) {
        MessageObject messageObject = getDoubleTapMessage(chatActivity, view);
        if (messageObject == null) {
            return false;
        }
        int action = getConfiguredAction(messageObject);
        if (!canPerformAction(chatActivity, messageObject, action)) {
            return false;
        }
        switch (action) {
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REPLY:
                return chatActivity.fluffyPerformDoubleTapAction(messageObject, ChatActivity.OPTION_REPLY);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_COPY:
                return chatActivity.fluffyPerformDoubleTapAction(messageObject, ChatActivity.OPTION_COPY);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_FORWARD:
                return chatActivity.fluffyPerformDoubleTapAction(messageObject, ChatActivity.OPTION_FORWARD);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_EDIT:
                chatActivity.fluffyStartEditingMessageObject(messageObject);
                return true;
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_SAVE: {
                int option = resolveSaveOption(messageObject);
                return option != -1 && chatActivity.fluffyPerformDoubleTapAction(messageObject, option);
            }
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_DELETE:
                return chatActivity.fluffyPerformDoubleTapAction(messageObject, ChatActivity.OPTION_DELETE);
            default:
                return false;
        }
    }

    public static void onStartEditingMessageObject(ChatActivity chatActivity, MessageObject messageObject, RecyclerListView chatListView, int blurredViewBottomOffset, ChatActivityEnterView chatActivityEnterView) {
        if (chatListView == null || chatActivityEnterView == null || messageObject == null) {
            return;
        }
        if (!shouldApplyEditAutoscroll(chatActivity, messageObject)) {
            return;
        }
        chatActivity.scrollToMessageId(messageObject.getId(), 0, true, 0, true, 0);
        chatListView.post(() -> {
            int childCount = chatListView.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = chatListView.getChildAt(i);
                MessageObject childMessageObject = child instanceof ChatMessageCell ? ((ChatMessageCell) child).getMessageObject() : null;
                if (childMessageObject == null || childMessageObject.getId() != messageObject.getId()) {
                    continue;
                }
                int bottomSpace = chatListView.getMeasuredHeight() - blurredViewBottomOffset - chatActivityEnterView.getMeasuredHeight();
                int dy = bottomSpace - (child.getTop() + child.getHeight());
                if (dy > 0) {
                    chatListView.smoothScrollBy(0, dy);
                }
                break;
            }
        });
    }

    private static int getConfiguredAction(MessageObject messageObject) {
        boolean outgoing = messageObject != null && (messageObject.isOutOwner() || messageObject.isOut());
        return outgoing
                ? AppearanceSettingsPatch.getDoubleTapOutAction()
                : AppearanceSettingsPatch.getDoubleTapInAction();
    }

    @Nullable
    private static MessageObject getDoubleTapMessage(ChatActivity chatActivity, View view) {
        if (!(view instanceof ChatMessageCell)) {
            return null;
        }
        MessageObject messageObject = ((ChatMessageCell) view).getPrimaryMessageObject();
        return isEligibleMessage(chatActivity, messageObject) ? messageObject : null;
    }

    private static boolean isEligibleMessage(ChatActivity chatActivity, MessageObject messageObject) {
        if (messageObject == null || chatActivity.getParentActivity() == null || chatActivity.isInScheduleMode() || chatActivity.isInPreviewMode()) {
            return false;
        }
        if (messageObject.isDateObject || messageObject.isSending() || messageObject.isEditing() || messageObject.isSponsored()) {
            return false;
        }
        if (messageObject.getDialogId() != chatActivity.getDialogId()) {
            return false;
        }
        return messageObject.type != MessageObject.TYPE_STORY
                && messageObject.type != MessageObject.TYPE_JOINED_CHANNEL;
    }

    private static boolean canPerformAction(ChatActivity chatActivity, MessageObject messageObject, int action) {
        switch (action) {
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_NONE:
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REACTION:
                return false;
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_EDIT: {
                ChatActivityEnterView chatActivityEnterView = chatActivity.getChatActivityEnterView();
                return chatActivityEnterView != null
                        && !chatActivityEnterView.hasAudioToSend()
                        && !messageObject.scheduled
                        && !messageObject.isQuickReply()
                        && messageObject.canEditMessage(chatActivity.getCurrentChat());
            }
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_SAVE:
                return resolveSaveOption(messageObject) != -1;
            default:
                return true;
        }
    }

    private static int resolveSaveOption(MessageObject messageObject) {
        if (messageObject == null) {
            return -1;
        }
        if (messageObject.isPhoto() || messageObject.isVideo()) {
            return ChatActivity.OPTION_SAVE_TO_GALLERY;
        }
        if (messageObject.isDocument() || messageObject.isMusic() || messageObject.isGif()) {
            return ChatActivity.OPTION_SAVE_TO_DOWNLOADS_OR_MUSIC;
        }
        return -1;
    }

    private static boolean shouldApplyEditAutoscroll(ChatActivity chatActivity, MessageObject messageObject) {
        return messageObject.getDialogId() == chatActivity.getDialogId() && !messageObject.scheduled && !messageObject.isQuickReply();
    }
}
