package org.ushastoe.fluffy.patches;

import android.view.View;
import android.view.Gravity;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

public final class DialogsCenteredTitlePatch {
    private static final CopyOnWriteArrayList<WeakReference<ActionBar>> attachedActionBars = new CopyOnWriteArrayList<>();
    private static final AppearanceSettingsPatch.Listener appearanceListener = DialogsCenteredTitlePatch::invalidateAttachedActionBars;
    private static boolean listenerRegistered;

    private DialogsCenteredTitlePatch() {
    }

    public static void attach(ActionBar actionBar) {
        if (actionBar == null) {
            return;
        }

        boolean alreadyAttached = false;
        for (WeakReference<ActionBar> reference : attachedActionBars) {
            ActionBar attached = reference.get();
            if (attached == null) {
                attachedActionBars.remove(reference);
            } else if (attached == actionBar) {
                alreadyAttached = true;
            }
        }
        if (alreadyAttached) {
            actionBar.post(() -> centerTitleViews(actionBar));
            return;
        }

        attachedActionBars.add(new WeakReference<>(actionBar));
        if (!listenerRegistered) {
            AppearanceSettingsPatch.addListener(appearanceListener);
            listenerRegistered = true;
        }

        View.OnLayoutChangeListener listener = (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                centerTitleViews(actionBar);

        actionBar.addOnLayoutChangeListener(listener);
        if (actionBar.getTitlesContainer() != null) {
            actionBar.getTitlesContainer().addOnLayoutChangeListener(listener);
        }
        actionBar.getViewTreeObserver().addOnGlobalLayoutListener(() -> centerTitleViews(actionBar));
        actionBar.post(() -> centerTitleViews(actionBar));
    }

    public static void onTitleChanged(ActionBar actionBar) {
        if (actionBar == null || !isAttached(actionBar)) {
            return;
        }
        scheduleCenterPasses(actionBar);
    }

    public static float getCollapsedTitleLeft(ActionBar actionBar, View titleView, float defaultLeft) {
        if (actionBar == null || titleView == null) {
            return defaultLeft;
        }
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_DEFAULT || actionBar.getMeasuredWidth() <= 0 || titleView.getMeasuredWidth() <= 0) {
            return defaultLeft;
        }

        int[] bounds = calculateBounds(actionBar, mode);
        int leftBound = bounds[0];
        int rightBound = bounds[1];
        if (rightBound <= leftBound) {
            return defaultLeft;
        }
        int availableWidth = rightBound - leftBound;
        return leftBound + Math.max(0, (availableWidth - titleView.getMeasuredWidth()) / 2f);
    }

    private static void centerTitleViews(ActionBar actionBar) {
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_DEFAULT) {
            applyGravity(actionBar.getTitleTextView(), false);
            applyGravity(actionBar.getTitleTextView2(), false);
            resetTitleView(actionBar.getTitleTextView());
            resetTitleView(actionBar.getTitleTextView2());
            return;
        }

        applyGravity(actionBar.getTitleTextView(), true);
        applyGravity(actionBar.getTitleTextView2(), true);

        if (actionBar.getMeasuredWidth() <= 0) {
            return;
        }

        int[] bounds = calculateBounds(actionBar, mode);
        int leftBound = bounds[0];
        int rightBound = bounds[1];

        if (rightBound <= leftBound) {
            return;
        }

        centerTitleView(actionBar.getTitleTextView(), leftBound, rightBound);
        centerTitleView(actionBar.getTitleTextView2(), leftBound, rightBound);
    }

    private static int[] calculateBounds(ActionBar actionBar, int mode) {
        int leftBound = 0;
        int rightBound = actionBar.getMeasuredWidth();
        float centerX = actionBar.getMeasuredWidth() / 2.0f;

        for (int i = 0; i < actionBar.getChildCount(); i++) {
            View child = actionBar.getChildAt(i);
            if (!shouldAffectBounds(actionBar, child, mode)) {
                continue;
            }

            float childCenterX = child.getLeft() + child.getTranslationX() + child.getMeasuredWidth() / 2.0f;
            if (childCenterX <= centerX) {
                leftBound = Math.max(leftBound, Math.round(child.getRight() + child.getTranslationX()));
            } else {
                rightBound = Math.min(rightBound, Math.round(child.getLeft() + child.getTranslationX()));
            }
        }
        return new int[] {leftBound, rightBound};
    }

    private static boolean shouldAffectBounds(ActionBar actionBar, View child, int mode) {
        if (child == null || child.getVisibility() != View.VISIBLE || child.getMeasuredWidth() <= 0 || child.getAlpha() <= 0f) {
            return false;
        }
        if (child == actionBar.getTitleTextView()
                || child == actionBar.getTitleTextView2()
                || child == actionBar.getTitlesContainer()) {
            return false;
        }
        if (mode != AppearanceSettingsPatch.DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS) {
            return true;
        }
        return child != actionBar.menu && child != actionBar.getBackButton();
    }

    private static void centerTitleView(SimpleTextView titleView, int leftBound, int rightBound) {
        if (titleView == null || titleView.getVisibility() != View.VISIBLE || titleView.getMeasuredWidth() <= 0) {
            return;
        }

        View parent = titleView.getParent() instanceof View ? (View) titleView.getParent() : null;
        int parentLeft = parent != null ? parent.getLeft() : 0;
        int availableWidth = rightBound - leftBound;
        int desiredLeft = leftBound + Math.max(0, (availableWidth - titleView.getMeasuredWidth()) / 2);
        float translationX = desiredLeft - parentLeft - titleView.getLeft();
        if (translationX != titleView.getTranslationX()) {
            titleView.setTranslationX(translationX);
        }
    }

    private static void invalidateAttachedActionBars() {
        for (WeakReference<ActionBar> reference : attachedActionBars) {
            ActionBar actionBar = reference.get();
            if (actionBar == null) {
                attachedActionBars.remove(reference);
                continue;
            }
            actionBar.post(() -> {
                centerTitleViews(actionBar);
                actionBar.requestLayout();
                actionBar.invalidate();
                if (actionBar.getTitlesContainer() != null) {
                    actionBar.getTitlesContainer().requestLayout();
                    actionBar.getTitlesContainer().invalidate();
                }
                invalidateTitleView(actionBar.getTitleTextView());
                invalidateTitleView(actionBar.getTitleTextView2());
            });
        }
    }

    private static boolean isAttached(ActionBar actionBar) {
        for (WeakReference<ActionBar> reference : attachedActionBars) {
            ActionBar attached = reference.get();
            if (attached == null) {
                attachedActionBars.remove(reference);
            } else if (attached == actionBar) {
                return true;
            }
        }
        return false;
    }

    private static void scheduleCenterPasses(ActionBar actionBar) {
        int[] delays = {0, 16, 48, 120, 250};
        for (int delay : delays) {
            actionBar.postDelayed(() -> {
                centerTitleViews(actionBar);
                actionBar.requestLayout();
                actionBar.invalidate();
                if (actionBar.getTitlesContainer() != null) {
                    actionBar.getTitlesContainer().requestLayout();
                    actionBar.getTitlesContainer().invalidate();
                }
                invalidateTitleView(actionBar.getTitleTextView());
                invalidateTitleView(actionBar.getTitleTextView2());
            }, delay);
        }
    }

    private static void resetTitleView(SimpleTextView titleView) {
        if (titleView != null && titleView.getTranslationX() != 0f) {
            titleView.setTranslationX(0f);
        }
    }

    private static void invalidateTitleView(SimpleTextView titleView) {
        if (titleView != null) {
            titleView.requestLayout();
            titleView.invalidate();
        }
    }

    private static void applyGravity(SimpleTextView titleView, boolean centered) {
        if (titleView == null) {
            return;
        }
        int gravity = centered ? Gravity.CENTER : Gravity.LEFT | Gravity.CENTER_VERTICAL;
        titleView.setGravity(gravity);
    }
}
