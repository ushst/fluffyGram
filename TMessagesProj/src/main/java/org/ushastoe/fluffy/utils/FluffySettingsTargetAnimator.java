package org.ushastoe.fluffy.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RecyclerListView;

public final class FluffySettingsTargetAnimator {

    private static final long ANIMATION_DURATION = 650L;
    private static final int RETRY_COUNT = 8;
    private static final int RETRY_DELAY_MS = 40;

    private FluffySettingsTargetAnimator() {
    }

    public static void scrollAndPulseTarget(RecyclerListView listView, int adapterPosition) {
        if (listView == null || adapterPosition < 0) {
            return;
        }
        RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();
        if (layoutManager instanceof LinearLayoutManager) {
            ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(adapterPosition, 0);
        } else {
            listView.scrollToPosition(adapterPosition);
        }
        postAnimate(listView, adapterPosition, RETRY_COUNT);
    }

    private static void postAnimate(RecyclerListView listView, int adapterPosition, int attemptsLeft) {
        listView.postDelayed(() -> {
            if (listView == null) {
                return;
            }
            RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(adapterPosition);
            if (holder == null || holder.itemView == null) {
                if (attemptsLeft > 0) {
                    postAnimate(listView, adapterPosition, attemptsLeft - 1);
                }
                return;
            }
            animateView(holder.itemView);
        }, RETRY_DELAY_MS);
    }

    private static void animateView(View view) {
        if (view == null) {
            return;
        }
        int baseColor = Theme.getColor(Theme.key_windowBackgroundWhite);
        int accentColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), baseColor, blend(baseColor, accentColor, 0.14f), baseColor);
        animator.setDuration(ANIMATION_DURATION);
        animator.addUpdateListener(animation -> view.setBackgroundColor((Integer) animation.getAnimatedValue()));
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                view.setBackgroundColor(baseColor);
            }
        });
        animator.start();
    }

    private static int blend(int baseColor, int accentColor, float ratio) {
        int baseA = (baseColor >> 24) & 0xff;
        int baseR = (baseColor >> 16) & 0xff;
        int baseG = (baseColor >> 8) & 0xff;
        int baseB = baseColor & 0xff;

        int accentR = (accentColor >> 16) & 0xff;
        int accentG = (accentColor >> 8) & 0xff;
        int accentB = accentColor & 0xff;

        int outR = (int) (baseR + (accentR - baseR) * ratio);
        int outG = (int) (baseG + (accentG - baseG) * ratio);
        int outB = (int) (baseB + (accentB - baseB) * ratio);
        return (baseA << 24) | (outR << 16) | (outG << 8) | outB;
    }
}
