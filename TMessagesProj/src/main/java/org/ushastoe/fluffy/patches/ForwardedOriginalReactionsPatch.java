package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Optionally overlays reactions from the original channel post onto a forwarded
 * message for display. Does not persist them to the local DB.
 */
public final class ForwardedOriginalReactionsPatch {

    private static final String PREFS_NAME = "fluffy_general_settings";
    private static final String KEY_ENABLED = "forwarded_original_reactions_enabled";

    private static final HashSet<String> IN_FLIGHT = new HashSet<>();
    private static final HashSet<String> APPLIED = new HashSet<>();

    private ForwardedOriginalReactionsPatch() {
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
            setEnabled(object.optBoolean(KEY_ENABLED, false));
        } catch (Exception ignore) {
        }
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

    public static void maybeLoadForVisibleMessages(ChatActivity chatActivity, ArrayList<MessageObject> visibleObjects) {
        try {
            if (!isEnabled()
                    || chatActivity == null
                    || visibleObjects == null
                    || visibleObjects.isEmpty()) {
                return;
            }
            for (int i = 0; i < visibleObjects.size(); i++) {
                maybeLoadForMessage(chatActivity, visibleObjects.get(i));
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static void maybeLoadForMessage(ChatActivity chatActivity, MessageObject messageObject) {
        try {
            if (messageObject == null || messageObject.messageOwner == null || messageObject.getId() <= 0) {
                return;
            }
            if (messageObject.hasReactions()) {
                return;
            }
            OriginalRef original = resolveOriginalChannelPost(messageObject);
            if (original == null) {
                return;
            }
            final String key = requestKey(chatActivity.getDialogId(), messageObject.getId(), original);
            if (IN_FLIGHT.contains(key) || APPLIED.contains(key)) {
                return;
            }
            MessagesController messagesController = chatActivity.getMessagesController();
            if (messagesController == null) {
                return;
            }
            TLRPC.InputChannel inputChannel = resolveInputChannel(chatActivity, messagesController, original, messageObject.getId());
            if (inputChannel == null || inputChannel instanceof TLRPC.TL_inputChannelEmpty) {
                return;
            }

            IN_FLIGHT.add(key);
            TLRPC.TL_channels_getMessages req = new TLRPC.TL_channels_getMessages();
            req.channel = inputChannel;
            req.id.add(original.messageId);
            final int forwardMsgId = messageObject.getId();
            chatActivity.getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                try {
                    IN_FLIGHT.remove(key);
                    if (error != null) {
                        APPLIED.add(key);
                        return;
                    }
                    if (!(response instanceof TLRPC.messages_Messages)) {
                        APPLIED.add(key);
                        return;
                    }
                    TLRPC.Message originalMessage = findOriginalMessage((TLRPC.messages_Messages) response, original.messageId);
                    if (originalMessage == null || originalMessage instanceof TLRPC.TL_messageEmpty) {
                        APPLIED.add(key);
                        return;
                    }
                    TLRPC.TL_messageReactions reactions = sanitizeOverlayReactions(originalMessage.reactions);
                    if (reactions == null || reactions.results == null || reactions.results.isEmpty()) {
                        APPLIED.add(key);
                        return;
                    }
                    if (!isEnabled()) {
                        return;
                    }
                    MessageObject live = findLiveMessage(chatActivity, forwardMsgId);
                    if (live == null || live.hasReactions()) {
                        APPLIED.add(key);
                        return;
                    }
                    MessageObject.updateReactions(live.messageOwner, reactions);
                    live.reactionsChanged = true;
                    live.forceUpdate = true;
                    APPLIED.add(key);
                    ArrayList<MessageObject> update = new ArrayList<>(1);
                    update.add(live);
                    chatActivity.updateMessages(update, false);
                } catch (Throwable t) {
                    FileLog.e(t);
                    IN_FLIGHT.remove(key);
                }
            }));
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }

    private static TLRPC.InputChannel resolveInputChannel(
            ChatActivity chatActivity,
            MessagesController messagesController,
            OriginalRef original,
            int forwardMsgId
    ) {
        try {
            TLRPC.Chat channel = messagesController.getChat(original.channelId);
            if (ChatObject.isChannel(channel)) {
                TLRPC.InputChannel inputChannel = messagesController.getInputChannel(channel);
                if (inputChannel != null && !(inputChannel instanceof TLRPC.TL_inputChannelEmpty)) {
                    return inputChannel;
                }
            }
            TLRPC.InputPeer peer = messagesController.getInputPeer(chatActivity.getDialogId());
            if (peer == null || peer instanceof TLRPC.TL_inputPeerEmpty) {
                return null;
            }
            TLRPC.TL_inputChannelFromMessage fromMessage = new TLRPC.TL_inputChannelFromMessage();
            fromMessage.channel_id = original.channelId;
            fromMessage.peer = peer;
            fromMessage.msg_id = forwardMsgId;
            return fromMessage;
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    private static TLRPC.Message findOriginalMessage(TLRPC.messages_Messages messages, int messageId) {
        if (messages == null || messages.messages == null) {
            return null;
        }
        for (int i = 0; i < messages.messages.size(); i++) {
            TLRPC.Message message = messages.messages.get(i);
            if (message != null && message.id == messageId) {
                return message;
            }
        }
        return null;
    }

    private static TLRPC.TL_messageReactions sanitizeOverlayReactions(TLRPC.MessageReactions source) {
        if (!(source instanceof TLRPC.TL_messageReactions)) {
            return null;
        }
        TLRPC.TL_messageReactions reactions = (TLRPC.TL_messageReactions) source;
        if (reactions.results == null || reactions.results.isEmpty()) {
            return null;
        }
        for (int i = 0; i < reactions.results.size(); i++) {
            TLRPC.ReactionCount count = reactions.results.get(i);
            if (count == null) {
                continue;
            }
            count.chosen = false;
            count.chosen_order = 0;
            count.flags &= ~TLObject.FLAG_0;
        }
        reactions.reactions_as_tags = false;
        reactions.can_see_list = false;
        reactions.min = true;
        return reactions;
    }

    private static MessageObject findLiveMessage(ChatActivity chatActivity, int messageId) {
        try {
            if (chatActivity == null || chatActivity.messages == null) {
                return null;
            }
            for (int i = 0; i < chatActivity.messages.size(); i++) {
                MessageObject candidate = chatActivity.messages.get(i);
                if (candidate != null && candidate.getId() == messageId) {
                    return candidate;
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return null;
    }

    private static OriginalRef resolveOriginalChannelPost(MessageObject messageObject) {
        try {
            TLRPC.MessageFwdHeader fwd = messageObject.messageOwner.fwd_from;
            if (fwd == null || fwd.channel_post <= 0) {
                return null;
            }
            long channelId = 0;
            if (fwd.from_id instanceof TLRPC.TL_peerChannel) {
                channelId = fwd.from_id.channel_id;
            } else if (fwd.saved_from_peer instanceof TLRPC.TL_peerChannel) {
                channelId = fwd.saved_from_peer.channel_id;
            }
            if (channelId == 0) {
                return null;
            }
            return new OriginalRef(channelId, fwd.channel_post);
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    private static String requestKey(long dialogId, int forwardMsgId, OriginalRef original) {
        return dialogId + ":" + forwardMsgId + ":" + original.channelId + ":" + original.messageId;
    }

    private static SharedPreferences getPreferences() {
        Context context = ApplicationLoader.applicationContext;
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static final class OriginalRef {
        final long channelId;
        final int messageId;

        OriginalRef(long channelId, int messageId) {
            this.channelId = channelId;
            this.messageId = messageId;
        }
    }
}
