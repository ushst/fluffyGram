package org.ushastoe.fluffy.activities.elements;

import android.content.Context;
import android.view.View;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EditTextCell;
import org.telegram.ui.Cells.PollEditTextCell;
import org.telegram.ui.Components.LayoutHelper;

public abstract class BaseFluffySettingsActivity extends BaseFragment {

    protected ScrollView createSettingsScrollView(Context context) {
        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        return scrollView;
    }

    protected LinearLayout createSettingsContent(Context context, ScrollView parent) {
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        FluffySettingsScaffold.applyPageContentPadding(content);
        parent.addView(content, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        return content;
    }

    protected FrameLayout createSettingsCard(Context context) {
        FrameLayout card = new FrameLayout(context);
        FluffySettingsScaffold.styleCardContainer(card);
        return card;
    }

    protected TextView createSettingsSectionTitle(Context context, CharSequence text) {
        TextView title = new TextView(context);
        title.setText(text);
        FluffySettingsScaffold.styleSectionTitle(title);
        return title;
    }

    protected TextView createSettingsInfoBlock(Context context, CharSequence text) {
        TextView info = new TextView(context);
        info.setText(text);
        FluffySettingsScaffold.styleInfoBlock(info);
        return info;
    }

    protected int getSettingsSideInset() {
        return FluffySettingsScaffold.getCardHorizontalInset();
    }

    protected int dp(int value) {
        return AndroidUtilities.dp(value);
    }

    public static void styleInputCell(EditTextCell cell, boolean topRounded, boolean bottomRounded, boolean divider) {
        int radius = FluffySettingsScaffold.getCardRadius();
        int top = topRounded ? radius : 0;
        int bottom = bottomRounded ? radius : 0;
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        cell.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                top,
                top,
                bottom,
                bottom,
                background,
                background,
                background
        ));
        cell.setDivider(divider);
    }

    public static void styleInputContainer(View container, boolean topRounded, boolean bottomRounded) {
        int radius = FluffySettingsScaffold.getCardRadius();
        int top = topRounded ? radius : 0;
        int bottom = bottomRounded ? radius : 0;
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        container.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                top,
                top,
                bottom,
                bottom,
                background,
                background,
                background
        ));
    }

    public static void stylePollInputCell(PollEditTextCell cell, boolean topRounded, boolean bottomRounded) {
        int radius = FluffySettingsScaffold.getCardRadius();
        int top = topRounded ? radius : 0;
        int bottom = bottomRounded ? radius : 0;
        int background = Theme.getColor(Theme.key_windowBackgroundWhite);
        cell.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                top,
                top,
                bottom,
                bottom,
                background,
                background,
                background
        ));
    }
}
