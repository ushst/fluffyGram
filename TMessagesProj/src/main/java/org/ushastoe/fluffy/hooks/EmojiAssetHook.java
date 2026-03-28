package org.ushastoe.fluffy.hooks;

import android.graphics.Bitmap;

import org.ushastoe.fluffy.patches.EmojiAssetPatch;

public final class EmojiAssetHook {

    private EmojiAssetHook() {
    }

    public static Bitmap applyOptionalAlphaMask(Bitmap bitmap, byte page, short page2) {
        return EmojiAssetPatch.applyOptionalAlphaMask(bitmap, page, page2);
    }

    public static String mapAssetPath(String path) {
        return EmojiAssetPatch.mapAssetPath(path);
    }

    public static void onEmojiSetChanged(Bitmap[][] emojiBitmaps) {
        EmojiAssetPatch.onEmojiSetChanged(emojiBitmaps);
    }
}
