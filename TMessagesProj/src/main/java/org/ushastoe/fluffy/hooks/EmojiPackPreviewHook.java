package org.ushastoe.fluffy.hooks;

import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.patches.EmojiPackPreviewPatch;

public final class EmojiPackPreviewHook {

    private EmojiPackPreviewHook() {
    }

    public static boolean shouldSkipEmojiPackMosaic(TLRPC.WebPage webpage) {
        return EmojiPackPreviewPatch.shouldSkipEmojiPackMosaic(webpage);
    }
}
