package org.ushastoe.fluffy.patches;

import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Channels the user does not want to see among found posts. A channel can be hidden for one query
 * only (searches are kept as projects) or for every posts search at once.
 */
public final class PostsBlacklistPatch {

    public static final int OPTION_HIDE_CHANNEL_HERE = 9001;
    public static final int OPTION_HIDE_CHANNEL_EVERYWHERE = 9002;

    private static final String GLOBAL_KEY = "postssearch_blacklist_all";
    private static final String QUERY_KEY_PREFIX = "postssearch_blacklist_q_";
    private static final String SEPARATOR = ",";

    private PostsBlacklistPatch() {
    }

    private static SharedPreferences prefs(int currentAccount) {
        return MessagesController.getMainSettings(currentAccount);
    }

    private static String queryKey(String query) {
        return QUERY_KEY_PREFIX + (query == null ? "" : query.trim().toLowerCase());
    }

    private static LinkedHashSet<Long> read(int currentAccount, String key) {
        final LinkedHashSet<Long> ids = new LinkedHashSet<>();
        final String value = prefs(currentAccount).getString(key, null);
        if (TextUtils.isEmpty(value)) {
            return ids;
        }
        for (String part : value.split(SEPARATOR)) {
            try {
                ids.add(Long.parseLong(part));
            } catch (NumberFormatException ignore) {
            }
        }
        return ids;
    }

    private static void write(int currentAccount, String key, LinkedHashSet<Long> ids) {
        final SharedPreferences.Editor editor = prefs(currentAccount).edit();
        if (ids.isEmpty()) {
            editor.remove(key);
        } else {
            editor.putString(key, TextUtils.join(SEPARATOR, ids));
        }
        editor.apply();
    }

    public static ArrayList<Long> getHidden(int currentAccount, String query, boolean everywhere) {
        return new ArrayList<>(read(currentAccount, everywhere ? GLOBAL_KEY : queryKey(query)));
    }

    public static boolean isHidden(int currentAccount, String query, long dialogId) {
        if (dialogId == 0) {
            return false;
        }
        if (read(currentAccount, GLOBAL_KEY).contains(dialogId)) {
            return true;
        }
        return !TextUtils.isEmpty(query) && read(currentAccount, queryKey(query)).contains(dialogId);
    }

    public static void hide(int currentAccount, String query, long dialogId, boolean everywhere) {
        final String key = everywhere ? GLOBAL_KEY : queryKey(query);
        final LinkedHashSet<Long> ids = read(currentAccount, key);
        if (ids.add(dialogId)) {
            write(currentAccount, key, ids);
        }
    }

    public static void show(int currentAccount, String query, long dialogId, boolean everywhere) {
        final String key = everywhere ? GLOBAL_KEY : queryKey(query);
        final LinkedHashSet<Long> ids = read(currentAccount, key);
        if (ids.remove(dialogId)) {
            write(currentAccount, key, ids);
        }
    }

    public static int getHiddenCount(int currentAccount, String query) {
        final LinkedHashSet<Long> ids = read(currentAccount, GLOBAL_KEY);
        ids.addAll(read(currentAccount, queryKey(query)));
        return ids.size();
    }

    public static void filterMessages(int currentAccount, String query, List<MessageObject> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (int i = messages.size() - 1; i >= 0; --i) {
            final MessageObject message = messages.get(i);
            if (message != null && isHidden(currentAccount, query, message.getDialogId())) {
                messages.remove(i);
            }
        }
    }

    public static void fillMessageMenu(boolean searchMode, String query, MessageObject message, ArrayList<Integer> icons, ArrayList<CharSequence> items, ArrayList<Integer> options) {
        if (!searchMode || message == null || message.getDialogId() >= 0) {
            return;
        }
        if (!TextUtils.isEmpty(query)) {
            items.add(formatString(R.string.FluffyPostsHideChannelHere, query));
            options.add(OPTION_HIDE_CHANNEL_HERE);
            icons.add(R.drawable.msg_block2);
        }
        items.add(getString(R.string.FluffyPostsHideChannelEverywhere));
        options.add(OPTION_HIDE_CHANNEL_EVERYWHERE);
        icons.add(R.drawable.msg_block2);
    }

    public static boolean processOption(ChatActivity chatActivity, int option, MessageObject message, String query) {
        if (option != OPTION_HIDE_CHANNEL_HERE && option != OPTION_HIDE_CHANNEL_EVERYWHERE) {
            return false;
        }
        if (message == null) {
            return true;
        }
        final int currentAccount = chatActivity.getCurrentAccount();
        final long dialogId = message.getDialogId();
        final boolean everywhere = option == OPTION_HIDE_CHANNEL_EVERYWHERE;

        hide(currentAccount, query, dialogId, everywhere);
        chatActivity.hideMessagesOfDialog(dialogId);

        BulletinFactory.of(chatActivity)
            .createSimpleBulletin(R.raw.chats_infotip, formatString(R.string.FluffyPostsHiddenToast, getChannelName(currentAccount, dialogId)))
            .show();
        return true;
    }

    public static String getChannelName(int currentAccount, long dialogId) {
        final TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        if (chat != null && !TextUtils.isEmpty(chat.title)) {
            return chat.title;
        }
        final TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
        if (user != null) {
            return UserObject.getUserName(user);
        }
        return String.valueOf(dialogId);
    }
}
