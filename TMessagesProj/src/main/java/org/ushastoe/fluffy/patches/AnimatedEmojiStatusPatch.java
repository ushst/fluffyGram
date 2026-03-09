package org.ushastoe.fluffy.patches;

import android.graphics.drawable.Drawable;
import android.view.View;

import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.Components.AnimatedEmojiDrawable;

public final class AnimatedEmojiStatusPatch {

    private AnimatedEmojiStatusPatch() {
    }

    public static void onImageReady(AnimatedEmojiDrawable drawable, int cacheType, boolean attached, long documentId) {
        if (!isStatusCacheType(cacheType) || drawable == null) {
            return;
        }
        ImageReceiver imageReceiver = drawable.getImageReceiver();
        if (attached && imageReceiver != null) {
            imageReceiver.startAnimation(true);
            imageReceiver.invalidate();
            View parentView = imageReceiver.getParentView();
            if (parentView != null) {
                parentView.invalidate();
            }
        }
    }

    public static void onAttachStateChanged(AnimatedEmojiDrawable drawable, int cacheType, boolean attached, long documentId) {
        if (!isStatusCacheType(cacheType) || drawable == null) {
            return;
        }
        ImageReceiver imageReceiver = drawable.getImageReceiver();
        if (attached && imageReceiver != null && imageReceiver.hasBitmapImage()) {
            imageReceiver.startAnimation(true);
            imageReceiver.invalidate();
            View parentView = imageReceiver.getParentView();
            if (parentView != null) {
                parentView.invalidate();
            }
        }
    }

    public static void onSwapDrawableParentChanged(Drawable currentDrawable, Drawable previousDrawable, View parentView) {
        syncDrawableParent(currentDrawable, parentView);
        if (previousDrawable != currentDrawable) {
            syncDrawableParent(previousDrawable, null);
        }
    }

    private static boolean isStatusCacheType(int cacheType) {
        return cacheType == AnimatedEmojiDrawable.CACHE_TYPE_EMOJI_STATUS
                || cacheType == AnimatedEmojiDrawable.CACHE_TYPE_ALERT_EMOJI_STATUS;
    }

    private static void syncDrawableParent(Drawable drawable, View parentView) {
        if (!(drawable instanceof AnimatedEmojiDrawable)) {
            return;
        }
        AnimatedEmojiDrawable animatedEmojiDrawable = (AnimatedEmojiDrawable) drawable;
        ImageReceiver imageReceiver = animatedEmojiDrawable.getImageReceiver();
        if (imageReceiver == null) {
            return;
        }
        imageReceiver.setParentView(parentView);
        if (parentView != null && imageReceiver.hasBitmapImage()) {
            imageReceiver.invalidate();
            parentView.invalidate();
            if (imageReceiver.isAttachedToWindow()) {
                imageReceiver.startAnimation(true);
            }
        }
    }
}
