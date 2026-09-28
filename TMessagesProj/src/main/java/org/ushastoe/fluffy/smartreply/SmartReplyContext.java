package org.ushastoe.fluffy.smartreply;

import java.util.Collections;
import java.util.List;

public final class SmartReplyContext {

    public final int account;
    public final long dialogId;
    public final String incomingText;
    /** Already {@link SmartReplyText#clean cleaned}. */
    final String cleanedIncoming;
    /** Oldest first; empty when not collected. For conversation-aware providers. */
    public final List<Turn> recentTurns;

    public SmartReplyContext(int account, long dialogId, String incomingText, List<Turn> recentTurns) {
        this.account = account;
        this.dialogId = dialogId;
        this.incomingText = incomingText;
        this.cleanedIncoming = SmartReplyText.clean(incomingText);
        this.recentTurns = recentTurns != null ? recentTurns : Collections.emptyList();
    }

    public static final class Turn {
        public final boolean outgoing;
        public final String text;
        public final int date;

        public Turn(boolean outgoing, String text, int date) {
            this.outgoing = outgoing;
            this.text = text;
            this.date = date;
        }
    }
}
