package org.ushastoe.fluffy.smartreply;

import android.content.Context;

import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;

import java.util.Collections;
import java.util.List;

final class LocalHistoryProvider implements SmartReplyProvider {

    /** Replies given to the current contact beat replies given to other people. */
    private static final float OTHER_DIALOG_FACTOR = 0.8f;

    private final LocalReplyCorpus corpus;

    LocalHistoryProvider(LocalReplyCorpus corpus) {
        this.corpus = corpus;
    }

    @Override
    public String id() {
        return "local_history";
    }

    @Override
    public boolean isEnabled() {
        return AppearanceSettingsHook.isSmartReplyHistoryEnabled();
    }

    @Override
    public boolean prepare(Context app, int account) {
        return corpus.get(account) != null;
    }

    @Override
    public List<String> suggest(SmartReplyContext ctx, int limit) {
        LocalReplyCorpus.Snapshot snapshot = corpus.get(ctx.account);
        if (snapshot == null) {
            return Collections.emptyList();
        }
        return snapshot.index.query(ctx.cleanedIncoming, limit, ctx.dialogId, OTHER_DIALOG_FACTOR);
    }
}
