package org.ushastoe.fluffy.smartreply;

import android.content.Context;

import java.util.List;

/**
 * One source of reply suggestions. All methods run on the engine thread,
 * so implementations may block (asset load, DB scan, model inference).
 */
interface SmartReplyProvider {

    String id();

    boolean isEnabled();

    /** Lazily loads whatever the provider needs; false when it cannot serve this account. */
    boolean prepare(Context app, int account);

    List<String> suggest(SmartReplyContext ctx, int limit);
}
