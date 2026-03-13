package org.ushastoe.fluffy.patches;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Stories.DialogStoriesCell;

public final class DialogsAppTitlePatch {

    private DialogsAppTitlePatch() {
    }

    public static void applyDialogsActionBarTitle(ActionBar actionBar, int currentAccount, Drawable rightDrawable) {
        if (actionBar == null) {
            return;
        }
        actionBar.setTitle(getDialogsAppTitle(currentAccount), rightDrawable);
    }

    public static CharSequence getDialogsStoryTitle(CharSequence currentTitle, int currentAccount) {
        if (!TextUtils.isEmpty(currentTitle)) {
            return currentTitle;
        }
        return getDialogsAppTitle(currentAccount);
    }

    public static void refreshDialogsStoryStatus(DialogStoriesCell dialogStoriesCell, int currentAccount) {
        if (dialogStoriesCell == null) {
            return;
        }
        dialogStoriesCell.updateStatus(UserConfig.getInstance(currentAccount).getCurrentUser(), false);
    }

    public static void refreshDialogsActionBarTitleLayout(ActionBar actionBar) {
        if (actionBar == null) {
            return;
        }
        int[] delays = {0, 16, 64, 180};
        for (int delay : delays) {
            actionBar.postDelayed(() -> {
                syncActionBarRightDrawable(actionBar, true);
                invalidateDialogsActionBarTitle(actionBar);
            }, delay);
        }
    }

    public static void onActionBarTitleUpdated(ActionBar actionBar) {
        syncActionBarRightDrawable(actionBar, true);
        refreshDialogsActionBarTitleLayout(actionBar);
    }

    public static void onDialogsEmojiLoaded(ActionBar actionBar) {
        if (actionBar == null) {
            return;
        }
        syncActionBarRightDrawable(actionBar, true);
        invalidateDialogsActionBarTitle(actionBar);
        actionBar.post(() -> {
            syncActionBarRightDrawable(actionBar, true);
            invalidateDialogsActionBarTitle(actionBar);
        });
    }

    public static void onActionBarResume(ActionBar actionBar) {
        syncActionBarRightDrawable(actionBar, true);
        refreshDialogsActionBarTitleLayout(actionBar);
    }

    public static void onActionBarPause(ActionBar actionBar) {
        syncActionBarRightDrawable(actionBar, false);
    }

    public static void onActionBarAttached(ActionBar actionBar) {
        syncActionBarRightDrawable(actionBar, true);
        refreshDialogsActionBarTitleLayout(actionBar);
    }

    public static void onActionBarDetached(ActionBar actionBar) {
        syncActionBarRightDrawable(actionBar, false);
    }

    public static void onDialogsStatusUpdated(ActionBar actionBar, int currentAccount, Drawable statusDrawable) {
        if (actionBar == null) {
            return;
        }
        Drawable desiredDrawable = statusDrawable;
        if (statusDrawable instanceof AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable) {
            if (((AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable) statusDrawable).getDrawable() == null) {
                desiredDrawable = null;
            }
        }
        SimpleTextView titleTextView = actionBar.getTitleTextView();
        if (titleTextView != null && titleTextView.getRightDrawable() == desiredDrawable) {
            return;
        }
        actionBar.setTitle(getDialogsAppTitle(currentAccount), desiredDrawable);
    }

    public static boolean shouldShowDialogsStoryStatusWithTitle(CharSequence currentTitle) {
        return TextUtils.isEmpty(currentTitle);
    }

    public static CharSequence getDialogsAppTitle(int currentAccount) {
        int mode = AppearanceSettingsPatch.getDialogsAppTitleMode();
        CharSequence title;
        switch (mode) {
            case AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_FLUFFY:
                title = "fluffy";
                break;
            case AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_TELEGRAM:
                title = "Telegram";
                break;
            case AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_USERNAME:
                title = getCurrentUserUsername(currentAccount);
                break;
            case AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_FIRST_NAME:
                title = getCurrentUserFirstName(currentAccount);
                break;
            case AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_CUSTOM:
                title = AppearanceSettingsPatch.getDialogsAppTitleCustom();
                break;
            case AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_FLUFFY_GRAM:
            default:
                title = LocaleController.getString(R.string.AppName);
                break;
        }
        if (TextUtils.isEmpty(title)) {
            return LocaleController.getString(R.string.AppName);
        }
        return title;
    }

    private static CharSequence getCurrentUserUsername(int currentAccount) {
        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        String username = UserObject.getPublicUsername(user);
        if (TextUtils.isEmpty(username)) {
            return null;
        }
        return "@" + username;
    }

    private static CharSequence getCurrentUserFirstName(int currentAccount) {
        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (user == null) {
            return null;
        }
        String firstName = UserObject.getFirstName(user);
        return TextUtils.isEmpty(firstName) ? null : firstName;
    }

    private static void invalidateDialogsActionBarTitle(ActionBar actionBar) {
        actionBar.requestLayout();
        actionBar.invalidate();
        if (actionBar.getTitlesContainer() != null) {
            actionBar.getTitlesContainer().requestLayout();
            actionBar.getTitlesContainer().invalidate();
        }
        SimpleTextView titleTextView = actionBar.getTitleTextView();
        if (titleTextView != null) {
            titleTextView.requestLayout();
            titleTextView.invalidate();
        }
        SimpleTextView titleTextView2 = actionBar.getTitleTextView2();
        if (titleTextView2 != null) {
            titleTextView2.requestLayout();
            titleTextView2.invalidate();
        }
    }

    private static void syncActionBarRightDrawable(ActionBar actionBar, boolean attach) {
        if (actionBar == null) {
            return;
        }
        SimpleTextView titleTextView = actionBar.getTitleTextView();
        SimpleTextView titleTextView2 = actionBar.getTitleTextView2();
        syncSimpleTextViewRightDrawable(titleTextView, titleTextView2, attach);
        syncSimpleTextViewRightDrawable(titleTextView2, titleTextView, attach);
        if (attach) {
            normalizeActionBarRightDrawableOwner(actionBar);
        }
    }

    private static void syncSimpleTextViewRightDrawable(SimpleTextView titleTextView, SimpleTextView secondParent, boolean attach) {
        if (titleTextView == null) {
            return;
        }
        Drawable rightDrawable = titleTextView.getRightDrawable();
        if (!(rightDrawable instanceof AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable)) {
            return;
        }
        AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable swapDrawable = (AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable) rightDrawable;
        if (attach) {
            // Force a real rebind for the same drawable instance: SimpleTextView skips work
            // when the reference did not change, which breaks cold-start status rendering.
            titleTextView.setRightDrawable(null);
            titleTextView.setRightDrawable(swapDrawable);
            swapDrawable.setParentView(titleTextView);
            swapDrawable.setSecondParent(secondParent);
            swapDrawable.detach();
            swapDrawable.attach();
            swapDrawable.play();
            titleTextView.requestLayout();
            titleTextView.invalidate();
        } else {
            swapDrawable.detach();
            swapDrawable.setSecondParent(null);
        }
    }

    private static void normalizeActionBarRightDrawableOwner(ActionBar actionBar) {
        SimpleTextView titleTextView = actionBar.getTitleTextView();
        SimpleTextView titleTextView2 = actionBar.getTitleTextView2();
        if (titleTextView == null || titleTextView2 == null) {
            return;
        }
        Drawable primaryDrawable = titleTextView.getRightDrawable();
        Drawable secondaryDrawable = titleTextView2.getRightDrawable();
        if (!(primaryDrawable instanceof AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable) || primaryDrawable != secondaryDrawable) {
            return;
        }
        titleTextView2.setRightDrawable(null);
        AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable swapDrawable = (AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable) primaryDrawable;
        swapDrawable.setParentView(titleTextView);
        swapDrawable.setSecondParent(null);
        titleTextView.invalidate();
        titleTextView2.invalidate();
    }
}
