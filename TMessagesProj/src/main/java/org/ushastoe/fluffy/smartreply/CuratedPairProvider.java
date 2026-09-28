package org.ushastoe.fluffy.smartreply;

import android.content.Context;

import java.util.List;

final class CuratedPairProvider implements SmartReplyProvider {

    @Override
    public String id() {
        return "curated";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean prepare(Context app, int account) {
        return CuratedReplyDb.getInstance().ensureLoaded(app);
    }

    @Override
    public List<String> suggest(SmartReplyContext ctx, int limit) {
        return CuratedReplyDb.getInstance().pairs().query(ctx.cleanedIncoming, limit, ctx.dialogId, 1f);
    }
}
