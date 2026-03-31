package org.ushastoe.fluffy.hooks;

import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.telegram.ui.Cells.AboutLinkCell;
import org.ushastoe.fluffy.patches.AboutLinkCellPatch;

public final class AboutLinkCellHook {

    private AboutLinkCellHook() {
    }

    public static boolean handleShowMoreTouch(AboutLinkCell cell, MotionEvent event, int x, int y, View backgroundView, TextView textView) {
        return AboutLinkCellPatch.handleShowMoreTouch(cell, event, x, y, backgroundView, textView);
    }

    public static boolean handleShowMoreDispatchTouch(AboutLinkCell cell, MotionEvent event, View backgroundView, TextView textView) {
        return AboutLinkCellPatch.handleShowMoreDispatchTouch(cell, event, backgroundView, textView);
    }

    public static void setupShowMoreControl(AboutLinkCell cell, View backgroundView, TextView textView) {
        AboutLinkCellPatch.setupShowMoreControl(cell, backgroundView, textView);
    }

    public static boolean shouldDisableMoreButtonInProfile(int position, int bioRow) {
        return AboutLinkCellPatch.shouldDisableMoreButtonInProfile(position, bioRow);
    }

    public static void updateShowMoreText(AboutLinkCell cell, TextView textView, boolean expanded) {
        AboutLinkCellPatch.updateShowMoreText(cell, textView, expanded);
    }
}
