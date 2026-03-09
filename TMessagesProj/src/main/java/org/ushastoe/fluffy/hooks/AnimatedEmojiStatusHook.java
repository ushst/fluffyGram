package org.ushastoe.fluffy.hooks;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.ushastoe.fluffy.patches.AnimatedEmojiStatusPatch;

public final class AnimatedEmojiStatusHook {

    private AnimatedEmojiStatusHook() {
    }

    public static void onAnimatedEmojiImageReady(AnimatedEmojiDrawable drawable, int cacheType, boolean attached, long documentId) {
        AnimatedEmojiStatusPatch.onImageReady(drawable, cacheType, attached, documentId);
    }

    public static void onAnimatedEmojiAttachStateChanged(AnimatedEmojiDrawable drawable, int cacheType, boolean attached, long documentId) {
        AnimatedEmojiStatusPatch.onAttachStateChanged(drawable, cacheType, attached, documentId);
    }

    public static void onSwapDrawableParentChanged(Drawable currentDrawable, Drawable previousDrawable, View parentView) {
        AnimatedEmojiStatusPatch.onSwapDrawableParentChanged(currentDrawable, previousDrawable, parentView);
    }
}
