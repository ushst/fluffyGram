package org.ushastoe.fluffy.patches;

import android.view.View;
import android.view.Gravity;
import android.text.TextUtils;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.ChatAvatarContainer;
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

        int[] bounds = calculateBounds(actionBar, mode, null);
        int leftBound = bounds[0];
        int rightBound = bounds[1];
        if (rightBound <= leftBound) {
            return defaultLeft;
        }
        int availableWidth = rightBound - leftBound;
        return leftBound + Math.max(0, (availableWidth - titleView.getMeasuredWidth()) / 2f);
    }

    public static float getCollapsedTitleRightPadding(ActionBar actionBar, View titleView, float defaultRightPadding) {
        if (actionBar == null || titleView == null) {
            return defaultRightPadding;
        }
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_DEFAULT || actionBar.getMeasuredWidth() <= 0 || titleView.getMeasuredWidth() <= 0) {
            return defaultRightPadding;
        }

        int[] bounds = calculateBounds(actionBar, mode, null);
        int leftBound = bounds[0];
        int rightBound = bounds[1];
        if (rightBound <= leftBound) {
            return defaultRightPadding;
        }
        return Math.max(0, actionBar.getMeasuredWidth() - rightBound);
    }

    public static float getCollapsedContentLeft(ActionBar actionBar, float contentWidth, float defaultLeft) {
        if (actionBar == null || contentWidth <= 0) {
            return defaultLeft;
        }
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_DEFAULT || actionBar.getMeasuredWidth() <= 0) {
            return defaultLeft;
        }

        int[] bounds = calculateBounds(actionBar, mode, null);
        int leftBound = bounds[0];
        int rightBound = bounds[1];
        if (rightBound <= leftBound) {
            return defaultLeft;
        }
        float availableWidth = rightBound - leftBound;
        return leftBound + Math.max(0, (availableWidth - contentWidth) / 2f);
    }

    public static float getCollapsedContentRightPadding(ActionBar actionBar, float contentLeft, float containerWidth, float defaultRightPadding) {
        if (actionBar == null || containerWidth <= 0) {
            return defaultRightPadding;
        }
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_DEFAULT || actionBar.getMeasuredWidth() <= 0) {
            return defaultRightPadding;
        }

        int[] bounds = calculateBounds(actionBar, mode, null);
        int rightBound = bounds[1];
        if (rightBound <= bounds[0]) {
            return defaultRightPadding;
        }
        return Math.max(0, contentLeft + containerWidth - rightBound);
    }

    private static void centerTitleViews(ActionBar actionBar) {
        View customTitleView = findCustomTitleView(actionBar);
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_DEFAULT) {
            applyGravity(actionBar.getTitleTextView(), false);
            applyGravity(actionBar.getTitleTextView2(), false);
            resetTitleView(actionBar.getTitleTextView());
            resetTitleView(actionBar.getTitleTextView2());
            resetTitleView(customTitleView);
            return;
        }

        if (isTitleTransitionInProgress(actionBar)) {
            resetTitleView(actionBar.getTitleTextView());
            resetTitleView(actionBar.getTitleTextView2());
            resetTitleView(customTitleView);
            return;
        }

        applyGravity(actionBar.getTitleTextView(), true);
        applyGravity(actionBar.getTitleTextView2(), true);

        if (actionBar.getMeasuredWidth() <= 0) {
            return;
        }

        int[] bounds = calculateBounds(actionBar, mode, customTitleView);
        int leftBound = bounds[0];
        int rightBound = bounds[1];

        if (rightBound <= leftBound) {
            return;
        }

        centerTitleView(actionBar.getTitleTextView(), leftBound, rightBound);
        centerTitleView(actionBar.getTitleTextView2(), leftBound, rightBound);
        centerCustomTitleView(customTitleView, leftBound, rightBound);
    }

    private static int[] calculateBounds(ActionBar actionBar, int mode, View excludedView) {
        int leftBound = 0;
        int rightBound = actionBar.getMeasuredWidth();
        float centerX = actionBar.getMeasuredWidth() / 2.0f;

        for (int i = 0; i < actionBar.getChildCount(); i++) {
            View child = actionBar.getChildAt(i);
            if (!shouldAffectBounds(actionBar, child, mode, excludedView)) {
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

    private static boolean shouldAffectBounds(ActionBar actionBar, View child, int mode, View excludedView) {
        if (child == null || child.getVisibility() != View.VISIBLE || child.getMeasuredWidth() <= 0 || child.getAlpha() <= 0f) {
            return false;
        }
        if (child == actionBar.getTitleTextView()
                || child == actionBar.getTitleTextView2()
                || child == actionBar.getTitlesContainer()
                || child == excludedView) {
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

        centerView(titleView, leftBound, rightBound);
    }

    private static void centerCustomTitleView(View customTitleView, int leftBound, int rightBound) {
        if (customTitleView == null || customTitleView.getVisibility() != View.VISIBLE || customTitleView.getMeasuredWidth() <= 0) {
            return;
        }

        centerView(customTitleView, leftBound, rightBound);
    }

    private static void centerView(View view, int leftBound, int rightBound) {
        View parent = view.getParent() instanceof View ? (View) view.getParent() : null;
        int parentLeft = parent != null ? parent.getLeft() : 0;
        int availableWidth = rightBound - leftBound;
        int desiredLeft = leftBound + Math.max(0, (availableWidth - view.getMeasuredWidth()) / 2);
        float translationX = desiredLeft - parentLeft - view.getLeft();
        if (translationX != view.getTranslationX()) {
            view.setTranslationX(translationX);
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
        resetTitleView((View) titleView);
    }

    private static void invalidateTitleView(SimpleTextView titleView) {
        if (titleView != null) {
            titleView.requestLayout();
            titleView.invalidate();
        }
    }

    private static void resetTitleView(View titleView) {
        if (titleView != null && titleView.getTranslationX() != 0f) {
            titleView.setTranslationX(0f);
        }
    }

    private static View findCustomTitleView(ActionBar actionBar) {
        for (int i = 0; i < actionBar.getChildCount(); i++) {
            View child = actionBar.getChildAt(i);
            if (child instanceof ChatAvatarContainer) {
                return child;
            }
        }
        return null;
    }

    private static boolean isTitleTransitionInProgress(ActionBar actionBar) {
        SimpleTextView secondaryTitle = actionBar.getTitleTextView2();
        return secondaryTitle != null
                && secondaryTitle.getVisibility() == View.VISIBLE
                && secondaryTitle.getAlpha() > 0.01f
                && !TextUtils.isEmpty(secondaryTitle.getText());
    }

    private static void applyGravity(SimpleTextView titleView, boolean centered) {
        if (titleView == null) {
            return;
        }
        int gravity = centered ? Gravity.CENTER : Gravity.LEFT | Gravity.CENTER_VERTICAL;
        titleView.setGravity(gravity);
    }
}
