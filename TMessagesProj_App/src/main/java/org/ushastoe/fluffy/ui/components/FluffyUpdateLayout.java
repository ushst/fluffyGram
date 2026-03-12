package org.ushastoe.fluffy.ui.components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BetaUpdate;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MediaActionDrawable;
import org.telegram.ui.Components.RadialProgress2;
import org.telegram.ui.IUpdateLayout;

public class FluffyUpdateLayout extends IUpdateLayout {

    private FrameLayout updateLayout;
    private RadialProgress2 updateLayoutIcon;
    private AnimatedTextView updateTextView;

    private final Activity activity;
    private final ViewGroup sideMenuContainer;

    public FluffyUpdateLayout(Activity activity, ViewGroup sideMenuContainer) {
        super(activity, sideMenuContainer);
        this.activity = activity;
        this.sideMenuContainer = sideMenuContainer;
    }

    @Override
    public void updateFileProgress(Object[] args) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            AndroidUtilities.runOnUIThread(() -> updateFileProgress(args));
            return;
        }
        if (updateTextView == null || updateLayoutIcon == null) {
            return;
        }
        if (!ApplicationLoader.applicationLoaderInstance.isCustomUpdate()) {
            return;
        }
        if (ApplicationLoader.applicationLoaderInstance.isDownloadingUpdate()) {
            float progress = ApplicationLoader.applicationLoaderInstance.getDownloadingUpdateProgress();
            updateLayoutIcon.setProgress(progress, true);
            setUpdateText(LocaleController.formatString(R.string.AppUpdateDownloading, (int) (progress * 100)), true);
        }
    }

    @Override
    public void createUpdateUI(int currentAccount) {
        if (sideMenuContainer == null || updateLayout != null) {
            return;
        }
        updateLayout = new FrameLayout(activity);
        updateLayout.setVisibility(View.INVISIBLE);
        updateLayout.setTranslationY(dp(44));
        updateLayout.setBackground(Theme.getSelectorDrawable(0x40ffffff, false));
        sideMenuContainer.addView(updateLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 44, Gravity.LEFT | Gravity.BOTTOM));
        updateLayout.setOnClickListener(v -> onUpdateClicked());

        updateTextView = new AnimatedTextView(activity, true, true, true) {
            @Override
            protected void onDraw(Canvas canvas) {
                canvas.save();
                canvas.translate(dp(15), 0);
                super.onDraw(canvas);
                canvas.translate((getMeasuredWidth() - width()) / 2f - dp(30), dp(11));
                updateLayoutIcon.draw(canvas);
                canvas.restore();
            }
        };
        updateTextView.setTextSize(dp(15));
        updateTextView.setTypeface(AndroidUtilities.bold());
        updateTextView.setTextColor(0xffffffff);
        updateTextView.setGravity(Gravity.CENTER);
        updateLayout.addView(updateTextView, LayoutHelper.createFrameMatchParent());
        updateTextView.setText(LocaleController.getString(R.string.AppUpdate), false);

        updateLayoutIcon = new RadialProgress2(updateTextView);
        updateLayoutIcon.setColors(0xffffffff, 0xffffffff, Theme.getColor(Theme.key_featuredStickers_addButton), Theme.getColor(Theme.key_featuredStickers_addButton));
        updateLayoutIcon.setProgressRect(0, 0, dp(22), dp(22));
        updateLayoutIcon.setCircleRadius(dp(11));
        updateLayoutIcon.setAsMini();
    }

    @Override
    public void updateAppUpdateViews(int currentAccount, boolean animated) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            AndroidUtilities.runOnUIThread(() -> updateAppUpdateViews(currentAccount, animated));
            return;
        }
        if (sideMenuContainer == null) {
            return;
        }
        BetaUpdate update = ApplicationLoader.applicationLoaderInstance.getUpdate();
        boolean visible = update != null && update.higherThan(null) && ApplicationLoader.applicationLoaderInstance.isCustomUpdate();
        if (visible) {
            createUpdateUI(currentAccount);
            if (ApplicationLoader.applicationLoaderInstance.getDownloadedUpdateFile() != null) {
                updateLayoutIcon.setIcon(MediaActionDrawable.ICON_UPDATE, true, animated);
                setUpdateText(LocaleController.getString(R.string.AppUpdateNow), animated);
            } else if (ApplicationLoader.applicationLoaderInstance.isDownloadingUpdate()) {
                float progress = ApplicationLoader.applicationLoaderInstance.getDownloadingUpdateProgress();
                updateLayoutIcon.setIcon(MediaActionDrawable.ICON_CANCEL, true, animated);
                updateLayoutIcon.setProgress(progress, false);
                setUpdateText(LocaleController.formatString(R.string.AppUpdateDownloading, (int) (progress * 100)), animated);
            } else {
                updateLayoutIcon.setIcon(MediaActionDrawable.ICON_DOWNLOAD, true, animated);
                setUpdateText(LocaleController.getString(R.string.AppUpdate), animated);
            }

            if (updateLayout.getTag() != null) {
                return;
            }
            updateLayout.setVisibility(View.VISIBLE);
            updateLayout.setTag(Boolean.TRUE);
            if (animated) {
                updateLayout.animate().translationY(0).setInterpolator(CubicBezierInterpolator.EASE_OUT).setListener(null).setDuration(180).start();
            } else {
                updateLayout.setTranslationY(0);
            }
        } else {
            if (updateLayout == null || updateLayout.getTag() == null) {
                return;
            }
            updateLayout.setTag(null);
            if (animated) {
                updateLayout.animate().translationY(dp(44)).setInterpolator(CubicBezierInterpolator.EASE_OUT).setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (updateLayout.getTag() == null) {
                            updateLayout.setVisibility(View.INVISIBLE);
                        }
                    }
                }).setDuration(180).start();
            } else {
                updateLayout.setTranslationY(dp(44));
                updateLayout.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void onUpdateClicked() {
        if (!ApplicationLoader.applicationLoaderInstance.isCustomUpdate()) {
            return;
        }
        if (ApplicationLoader.applicationLoaderInstance.getDownloadedUpdateFile() != null) {
            ApplicationLoader.applicationLoaderInstance.openApkInstall(activity, null);
        } else if (ApplicationLoader.applicationLoaderInstance.isDownloadingUpdate()) {
            ApplicationLoader.applicationLoaderInstance.cancelDownloadingUpdate();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateLoading);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
        } else {
            ApplicationLoader.applicationLoaderInstance.downloadUpdate();
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateLoading);
        }
    }

    private void setUpdateText(String text, boolean animate) {
        updateTextView.setText(text, animate);
    }
}
