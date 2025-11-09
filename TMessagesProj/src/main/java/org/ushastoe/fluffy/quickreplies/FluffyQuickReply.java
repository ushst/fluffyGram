package org.ushastoe.fluffy.quickreplies;

import androidx.annotation.NonNull;

/**
 * Модель кастомной быстрой команды fluffy.
 */
public class FluffyQuickReply {
    public int id;
    public char prefix;
    public String command;
    public String message;
    int order;

    public FluffyQuickReply copy() {
        FluffyQuickReply copy = new FluffyQuickReply();
        copy.id = id;
        copy.prefix = prefix;
        copy.command = command;
        copy.message = message;
        copy.order = order;
        return copy;
    }

    @NonNull
    @Override
    public String toString() {
        return "FluffyQuickReply{" +
                "id=" + id +
                ", prefix=" + prefix +
                ", command='" + command + '\'' +
                ", message='" + (message == null ? "" : message.replace('\n', ' ')) + '\'' +
                ", order=" + order +
                '}';
    }
}
