package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ChatActivity;

import java.util.ArrayList;
import java.util.Collections;

public final class CombineMessagesPatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "combine_selected_messages_enabled";

    private CombineMessagesPatch() {
    }

    public static boolean isEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_ENABLED, false);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void addActionModeItem(ActionBarMenu actionMode, int menuId) {
        if (actionMode == null || actionMode.getItem(menuId) != null) {
            return;
        }
        actionMode.addItemWithWidth(menuId, R.drawable.msg_replace, AndroidUtilities.dp(54), LocaleController.getString(R.string.FluffyCombineMessages));
    }

    public static void updateActionModeItem(ActionBarMenu actionMode, int menuId, boolean visible) {
        if (actionMode == null) {
            return;
        }
        ActionBarMenuItem item = actionMode.getItem(menuId);
        if (item == null) {
            return;
        }
        item.setVisibility(visible && isEnabled() ? View.VISIBLE : View.GONE);
    }

    public static boolean onActionModeItemClick(ChatActivity chatActivity, int id, int menuId, SparseArray<MessageObject>[] selectedMessagesCanCopyIds) {
        if (id != menuId || chatActivity == null || selectedMessagesCanCopyIds == null || !isEnabled()) {
            return false;
        }
        StringBuilder combinedText = new StringBuilder();
        ArrayList<Integer> ownMessageIds = new ArrayList<>();
        MessageObject replyTo = chatActivity.getThreadMessage();
        boolean firstMessage = true;
        long selfUserId = UserConfig.getInstance(chatActivity.getCurrentAccount()).getClientUserId();
        for (int a = 1; a >= 0; a--) {
            SparseArray<MessageObject> selectedMessages = selectedMessagesCanCopyIds[a];
            if (selectedMessages == null || selectedMessages.size() == 0) {
                continue;
            }
            ArrayList<Integer> ids = new ArrayList<>(selectedMessages.size());
            for (int b = 0; b < selectedMessages.size(); b++) {
                ids.add(selectedMessages.keyAt(b));
            }
            if (chatActivity.isSecretChat()) {
                Collections.sort(ids, Collections.reverseOrder());
            } else {
                Collections.sort(ids);
            }
            for (int b = 0; b < ids.size(); b++) {
                Integer messageId = ids.get(b);
                MessageObject messageObject = selectedMessages.get(messageId);
                String part = sanitizePart(messageObject);
                if (TextUtils.isEmpty(part)) {
                    continue;
                }
                if (firstMessage) {
                    replyTo = messageObject.replyMessageObject != null ? messageObject.replyMessageObject : chatActivity.getThreadMessage();
                    firstMessage = false;
                } else if (combinedText.length() > 0) {
                    combinedText.append('\n');
                }
                combinedText.append(part);
                if (messageObject.getSenderId() == selfUserId) {
                    ownMessageIds.add(messageId);
                }
            }
        }
        chatActivity.clearSelectionMode();
        if (combinedText.length() == 0) {
            return true;
        }
        SendMessagesHelper.getInstance(chatActivity.getCurrentAccount()).sendMessage(
                SendMessagesHelper.SendMessageParams.of(
                        combinedText.toString(),
                        chatActivity.getDialogId(),
                        replyTo,
                        chatActivity.getThreadMessage(),
                        null,
                        false,
                        null,
                        null,
                        null,
                        true,
                        0,
                        0,
                        null,
                        false
                )
        );
        if (!ownMessageIds.isEmpty()) {
            MessagesController.getInstance(chatActivity.getCurrentAccount()).deleteMessages(
                    ownMessageIds,
                    null,
                    null,
                    chatActivity.getDialogId(),
                    (int) chatActivity.getTopicId(),
                    true,
                    chatActivity.getChatMode()
            );
        }
        return true;
    }

    private static String sanitizePart(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageText == null) {
            return "";
        }
        return messageObject.messageText.toString().trim();
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
