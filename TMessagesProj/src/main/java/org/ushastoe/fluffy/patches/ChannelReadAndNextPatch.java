package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.View;

import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.chat.layouts.ChatActivitySideControlsButtonsLayout;

import java.util.ArrayList;

// "Read and next" side button in channels: marks the channel as read and
// opens the next unread channel, same as the pull-up swipe at the bottom.
public final class ChannelReadAndNextPatch {
    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "channel_read_and_next_button_enabled";

    private ChannelReadAndNextPatch() {
    }

    public static String exportSettingsJson() {
        JSONObject object = new JSONObject();
        try {
            object.put(KEY_ENABLED, isEnabled());
        } catch (Exception ignore) {
        }
        return object.toString();
    }

    public static void importSettingsJson(String json) {
        try {
            JSONObject object = TextUtils.isEmpty(json) ? new JSONObject() : new JSONObject(json);
            setEnabled(object.optBoolean(KEY_ENABLED, true));
        } catch (Exception ignore) {
        }
    }

    public static boolean isEnabled() {
        SharedPreferences preferences = getPreferences();
        return preferences == null || preferences.getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(boolean enabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static void attach(ChatActivity chatActivity, ChatActivitySideControlsButtonsLayout layout) {
        if (chatActivity == null || layout == null) {
            return;
        }
        if (!isSupported(chatActivity)) {
            return;
        }
        layout.showButton(ChatActivitySideControlsButtonsLayout.BUTTON_READ_AND_NEXT, true, false);
        attachCounter(chatActivity, layout);
    }

    // Badge with the number of unread channels the button can still go through.
    private static void attachCounter(ChatActivity chatActivity, ChatActivitySideControlsButtonsLayout layout) {
        int account = chatActivity.getCurrentAccount();
        Runnable update = () -> layout.setButtonCount(ChatActivitySideControlsButtonsLayout.BUTTON_READ_AND_NEXT,
            countUnreadChannels(chatActivity.getMessagesController(), chatActivity.getDialogId()), true);
        NotificationCenter.NotificationCenterDelegate delegate = (id, acc, args) -> {
            if (id == NotificationCenter.updateInterfaces) {
                int mask = args.length > 0 && args[0] instanceof Integer ? (Integer) args[0] : MessagesController.UPDATE_MASK_ALL;
                if ((mask & (MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE | MessagesController.UPDATE_MASK_NEW_MESSAGE)) == 0) {
                    return;
                }
            }
            update.run();
        };
        layout.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                NotificationCenter center = NotificationCenter.getInstance(account);
                center.addObserver(delegate, NotificationCenter.dialogsNeedReload);
                center.addObserver(delegate, NotificationCenter.updateInterfaces);
                update.run();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                NotificationCenter center = NotificationCenter.getInstance(account);
                center.removeObserver(delegate, NotificationCenter.dialogsNeedReload);
                center.removeObserver(delegate, NotificationCenter.updateInterfaces);
            }
        });
        layout.setButtonCount(ChatActivitySideControlsButtonsLayout.BUTTON_READ_AND_NEXT,
            countUnreadChannels(chatActivity.getMessagesController(), chatActivity.getDialogId()), false);
    }

    // Same criteria as ChatPullingDownDrawable.getNextUnreadDialog, across all folders.
    private static int countUnreadChannels(MessagesController controller, long currentDialogId) {
        int count = 0;
        for (int i = 0; i < controller.dialogsByFolder.size(); i++) {
            ArrayList<TLRPC.Dialog> dialogs = controller.dialogsByFolder.valueAt(i);
            if (dialogs == null) {
                continue;
            }
            for (int j = 0; j < dialogs.size(); j++) {
                TLRPC.Dialog dialog = dialogs.get(j);
                if (dialog.id == currentDialogId || dialog.unread_count <= 0 || !DialogObject.isChannel(dialog)) {
                    continue;
                }
                TLRPC.Chat chat = controller.getChat(-dialog.id);
                if (chat == null || chat.megagroup || controller.isPromoDialog(dialog.id, false)
                    || controller.getRestrictionReason(chat.restriction_reason) != null) {
                    continue;
                }
                count++;
            }
        }
        return count;
    }

    public static void onClick(ChatActivity chatActivity) {
        if (chatActivity == null || !isSupported(chatActivity)) {
            return;
        }
        markAsRead(chatActivity);
        if (!chatActivity.fluffyAnimateToNextChat()) {
            chatActivity.finishFragment();
        }
    }

    private static boolean isSupported(ChatActivity chatActivity) {
        return isEnabled()
            && chatActivity.getChatMode() == 0
            && !chatActivity.isInPreviewMode()
            && ChatObject.isChannelAndNotMegaGroup(chatActivity.getCurrentChat());
    }

    private static void markAsRead(ChatActivity chatActivity) {
        long dialogId = chatActivity.getDialogId();
        MessagesController controller = chatActivity.getMessagesController();
        TLRPC.Dialog dialog = controller.dialogs_dict.get(dialogId);
        if (dialog == null) {
            return;
        }
        controller.markMentionsAsRead(dialogId, 0);
        controller.markDialogAsRead(dialogId, dialog.top_message, dialog.top_message, dialog.last_message_date, false, 0, 0, true, 0);
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
