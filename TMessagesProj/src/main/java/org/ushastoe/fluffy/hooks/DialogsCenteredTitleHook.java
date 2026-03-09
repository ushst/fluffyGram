package org.ushastoe.fluffy.hooks;

import org.telegram.ui.ActionBar.ActionBar;
import android.view.View;
import org.ushastoe.fluffy.patches.DialogsCenteredTitlePatch;

public final class DialogsCenteredTitleHook {

    private DialogsCenteredTitleHook() {
    }

    public static void attach(ActionBar actionBar) {
        DialogsCenteredTitlePatch.attach(actionBar);
    }

    public static void onTitleChanged(ActionBar actionBar) {
        DialogsCenteredTitlePatch.onTitleChanged(actionBar);
    }

    public static float getCollapsedTitleLeft(ActionBar actionBar, View titleView, float defaultLeft) {
        return DialogsCenteredTitlePatch.getCollapsedTitleLeft(actionBar, titleView, defaultLeft);
    }

    public static float getCollapsedTitleRightPadding(ActionBar actionBar, View titleView, float defaultRightPadding) {
        return DialogsCenteredTitlePatch.getCollapsedTitleRightPadding(actionBar, titleView, defaultRightPadding);
    }

    public static float getCollapsedContentLeft(ActionBar actionBar, float contentWidth, float defaultLeft) {
        return DialogsCenteredTitlePatch.getCollapsedContentLeft(actionBar, contentWidth, defaultLeft);
    }

    public static float getCollapsedContentRightPadding(ActionBar actionBar, float contentLeft, float containerWidth, float defaultRightPadding) {
        return DialogsCenteredTitlePatch.getCollapsedContentRightPadding(actionBar, contentLeft, containerWidth, defaultRightPadding);
    }
}
