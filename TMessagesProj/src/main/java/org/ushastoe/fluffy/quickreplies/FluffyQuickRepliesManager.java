package org.ushastoe.fluffy.quickreplies;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.ushastoe.fluffy.fluffyConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * Менеджер, отвечающий за хранение и управление кастомными быстрыми командами fluffy.
 */
public class FluffyQuickRepliesManager {

    private static final Object LOCK = new Object();
    private static FluffyQuickRepliesManager instance;

    private final ArrayList<FluffyQuickReply> replies = new ArrayList<>();
    private int nextId = 1;

    private static final Comparator<FluffyQuickReply> ORDER_COMPARATOR = (o1, o2) -> Integer.compare(o1.order, o2.order);

    public static final char[] SUPPORTED_PREFIXES = new char[]{'.', '!', '*'};

    public static FluffyQuickRepliesManager getInstance() {
        synchronized (LOCK) {
            if (instance == null) {
                instance = new FluffyQuickRepliesManager();
            }
        }
        return instance;
    }

    private FluffyQuickRepliesManager() {
        loadFromPreferences();
    }

    private void loadFromPreferences() {
        replies.clear();
        nextId = Math.max(1, fluffyConfig.getCustomQuickRepliesNextId());
        String data = fluffyConfig.getCustomQuickRepliesJson();
        if (TextUtils.isEmpty(data)) {
            sortReplies();
            return;
        }
        try {
            JSONArray array = new JSONArray(data);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                FluffyQuickReply reply = new FluffyQuickReply();
                reply.id = object.optInt("id", 0);
                String prefixString = object.optString("prefix", ".");
                reply.prefix = prefixString.isEmpty() ? '.' : prefixString.charAt(0);
                reply.command = object.optString("command", "");
                reply.message = object.optString("message", "");
                reply.order = object.optInt("order", i);
                if (reply.id == 0) {
                    reply.id = nextId++;
                } else if (reply.id >= nextId) {
                    nextId = reply.id + 1;
                }
                replies.add(reply);
            }
        } catch (JSONException e) {
            FileLog.e(e);
        }
        sortReplies();
    }

    private void sortReplies() {
        Collections.sort(replies, ORDER_COMPARATOR);
    }

    private void persist() {
        JSONArray array = new JSONArray();
        for (FluffyQuickReply reply : replies) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", reply.id);
                object.put("prefix", String.valueOf(reply.prefix));
                object.put("command", reply.command);
                object.put("message", reply.message);
                object.put("order", reply.order);
            } catch (JSONException e) {
                FileLog.e(e);
            }
            array.put(object);
        }
        fluffyConfig.saveCustomQuickReplies(array.toString(), nextId);
    }

    public synchronized ArrayList<FluffyQuickReply> getReplies() {
        ArrayList<FluffyQuickReply> copy = new ArrayList<>(replies.size());
        for (FluffyQuickReply reply : replies) {
            copy.add(reply.copy());
        }
        return copy;
    }

    public synchronized int getRepliesCount() {
        return replies.size();
    }

    public synchronized boolean hasRepliesForPrefix(char prefix) {
        for (FluffyQuickReply reply : replies) {
            if (reply.prefix == prefix) {
                return true;
            }
        }
        return false;
    }

    public synchronized ArrayList<FluffyQuickReply> search(char prefix, String query) {
        ArrayList<FluffyQuickReply> result = new ArrayList<>();
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        for (FluffyQuickReply reply : replies) {
            if (reply.prefix != prefix) {
                continue;
            }
            if (TextUtils.isEmpty(normalized)) {
                result.add(reply.copy());
                continue;
            }
            String commandLower = reply.command.toLowerCase(Locale.ROOT);
            if (commandLower.startsWith(normalized)) {
                result.add(reply.copy());
                continue;
            }
            String translit = AndroidUtilities.translitSafe(reply.command).toLowerCase(Locale.ROOT);
            if (!TextUtils.isEmpty(translit) && translit.startsWith(normalized)) {
                result.add(reply.copy());
            }
        }
        Collections.sort(result, ORDER_COMPARATOR);
        return result;
    }

    public synchronized void addOrUpdate(FluffyQuickReply updated) {
        if (updated == null) {
            return;
        }
        FluffyQuickReply existing = null;
        for (FluffyQuickReply reply : replies) {
            if (reply.id == updated.id && updated.id != 0) {
                existing = reply;
                break;
            }
        }
        if (existing == null) {
            FluffyQuickReply copy = updated.copy();
            if (copy.id == 0) {
                copy.id = nextId++;
            }
            if (replies.isEmpty()) {
                copy.order = 0;
            } else {
                copy.order = replies.get(replies.size() - 1).order + 1;
            }
            replies.add(copy);
        } else {
            existing.prefix = updated.prefix;
            existing.command = updated.command;
            existing.message = updated.message;
        }
        sortReplies();
        persist();
    }

    public synchronized void delete(int replyId) {
        for (int i = 0; i < replies.size(); i++) {
            if (replies.get(i).id == replyId) {
                replies.remove(i);
                break;
            }
        }
        sortReplies();
        persist();
    }

    public synchronized boolean hasDuplicate(int excludeId, char prefix, String command) {
        if (TextUtils.isEmpty(command)) {
            return false;
        }
        String compare = command.toLowerCase(Locale.ROOT);
        for (FluffyQuickReply reply : replies) {
            if (reply.prefix != prefix) {
                continue;
            }
            if (reply.id == excludeId) {
                continue;
            }
            if (reply.command != null && reply.command.toLowerCase(Locale.ROOT).equals(compare)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSupportedPrefix(char prefix) {
        for (char supportedPrefix : SUPPORTED_PREFIXES) {
            if (supportedPrefix == prefix) {
                return true;
            }
        }
        return false;
    }
}
