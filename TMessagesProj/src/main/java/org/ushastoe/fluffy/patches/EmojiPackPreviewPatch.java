package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.utils.tlutils.TlUtils;

/**
 * Debug toggle that skips emoji-pack mosaic previews in chat link cards.
 * Useful when a corrupt /addemoji pack crashes {@code StickerSetLinkIcon}.
 */
public final class EmojiPackPreviewPatch {

    private static final String PREFS_NAME = "fluffy_debug_settings";
    public static final String KEY_DISABLE_EMOJI_PACK_LINK_PREVIEW = "disable_emoji_pack_link_preview";

    private EmojiPackPreviewPatch() {
    }

    public static boolean isEmojiPackLinkPreviewDisabled() {
        SharedPreferences preferences = getPreferences();
        return preferences != null && preferences.getBoolean(KEY_DISABLE_EMOJI_PACK_LINK_PREVIEW, false);
    }

    public static void setEmojiPackLinkPreviewDisabled(boolean disabled) {
        SharedPreferences preferences = getPreferences();
        if (preferences == null) {
            return;
        }
        preferences.edit().putBoolean(KEY_DISABLE_EMOJI_PACK_LINK_PREVIEW, disabled).apply();
    }

    /**
     * @return true when the sticker-set webpage should render without the emoji mosaic.
     */
    public static boolean shouldSkipEmojiPackMosaic(TLRPC.WebPage webpage) {
        if (!isEmojiPackLinkPreviewDisabled() || webpage == null) {
            return false;
        }
        if (!"telegram_stickerset".equals(webpage.type)) {
            return false;
        }
        TLRPC.TL_webPageAttributeStickerSet attr =
                TlUtils.findFirstInstance(webpage.attributes, TLRPC.TL_webPageAttributeStickerSet.class);
        return attr != null && attr.emojis;
    }

    private static SharedPreferences getPreferences() {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
