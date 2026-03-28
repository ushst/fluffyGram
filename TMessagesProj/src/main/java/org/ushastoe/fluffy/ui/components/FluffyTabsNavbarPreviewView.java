package org.ushastoe.fluffy.ui.components;

import android.content.Context;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;
import org.telegram.ui.Components.glass.GlassTabView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.MainTabsLayout;
import org.telegram.ui.MainTabsActivity;
import org.ushastoe.fluffy.patches.MainTabsConfigPatch;
import org.ushastoe.fluffy.patches.MainTabsUiHost;
import org.ushastoe.fluffy.patches.MainTabsUiPatch;
import org.ushastoe.fluffy.patches.MainTabsUiState;

import java.util.ArrayList;

public class FluffyTabsNavbarPreviewView extends FrameLayout {
    private static final int DEFAULT_PREVIEW_WIDTH_DP = 328;
    private static final int DEFAULT_PREVIEW_SLOTS = 5;

    private final MainTabsLayout tabsView;
    private final Theme.ResourcesProvider resourcesProvider;
    private final int currentAccount;
    private final MainTabsUiState previewState;
    private final GlassTabView[] previewTabs;
    private final MainTabsUiHost previewHost = new PreviewHost();
    private final View topGlassView;
    private final BlurredBackgroundSourceColor backgroundSourceColor;
    private final BlurredBackgroundDrawable tabsViewBackground;
    private final BlurredBackgroundDrawable topGlassBackground;

    public FluffyTabsNavbarPreviewView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.currentAccount = currentAccount;
        this.resourcesProvider = resourcesProvider;
        this.previewState = MainTabsUiPatch.createState();

        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setFocusable(false);
        setOnTouchListener((v, event) -> true);

        tabsView = new MainTabsLayout(context);
        previewTabs = createPreviewTabs(context);
        tabsView.setClipChildren(false);
        tabsView.setPadding(
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4),
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4),
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4),
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4)
        );

        backgroundSourceColor = new BlurredBackgroundSourceColor();
        backgroundSourceColor.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(backgroundSourceColor);
        tabsViewBackground = factory.create(tabsView, BlurredBackgroundProviderImpl.mainTabs(resourcesProvider));
        tabsViewBackground.setRadius(org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT / 2f));
        tabsViewBackground.setPadding(org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN - 0.334f));
        tabsView.setBackground(tabsViewBackground);
        tabsView.setClipToOutline(false);

        addView(tabsView, LayoutHelper.createFrame(
                getPreviewWidthForSlots(DEFAULT_PREVIEW_SLOTS),
                DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        ));

        topGlassView = new View(context);
        topGlassBackground = factory.create(topGlassView, BlurredBackgroundProviderImpl.mainTabs(resourcesProvider));
        topGlassBackground.setRadius(org.telegram.messenger.AndroidUtilities.dp(11));
        topGlassBackground.setPadding(org.telegram.messenger.AndroidUtilities.dp(1));
        topGlassView.setBackground(topGlassBackground);
        topGlassView.setAlpha(0.72f);
        topGlassView.setClickable(false);
        topGlassView.setFocusable(false);
        addView(topGlassView, LayoutHelper.createFrame(
                getTopGlassWidthForSlots(DEFAULT_PREVIEW_SLOTS),
                14,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0,
                8,
                0,
                0
        ));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void updatePreview() {
        backgroundSourceColor.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        topGlassView.invalidate();
        MainTabsUiPatch.rebuildTabsBar(previewHost, previewState);
        disableInteractionForPreviewTabs();

        if (tabsView.getChildCount() == 0) {
            GlassTabView fallbackTab = MainTabsUiPatch.getTabViewForType(isCallsTabEnabled(), previewTabs, MainTabsConfigPatch.TAB_CHATS);
            if (fallbackTab != null) {
                tabsView.addView(fallbackTab);
                tabsView.setViewVisible(fallbackTab, true, false);
                fallbackTab.setSelected(true, false);
                fallbackTab.setClickable(false);
                fallbackTab.setFocusable(false);
            }
        }

        updatePreviewWidth();
        tabsView.requestLayout();
        tabsView.invalidate();
    }

    private GlassTabView[] createPreviewTabs(Context context) {
        GlassTabView[] tabs = new GlassTabView[5];
        tabs[0] = GlassTabView.createMainTab(context, resourcesProvider, GlassTabView.TabAnimation.CHATS, R.string.MainTabsChats);
        tabs[1] = GlassTabView.createMainTab(context, resourcesProvider, GlassTabView.TabAnimation.CONTACTS, R.string.MainTabsContacts);
        tabs[2] = GlassTabView.createMainTab(context, resourcesProvider, GlassTabView.TabAnimation.SETTINGS, R.string.Settings);
        tabs[3] = GlassTabView.createMainTab(context, resourcesProvider, GlassTabView.TabAnimation.CALLS, R.string.MainTabsCalls);
        tabs[4] = GlassTabView.createAvatar(context, resourcesProvider, currentAccount, R.string.MainTabsProfile);
        for (GlassTabView tab : tabs) {
            if (tab != null) {
                tab.setCounterBelowIcon(true);
                tab.setClickable(false);
                tab.setFocusable(false);
            }
        }
        return tabs;
    }

    private void disableInteractionForPreviewTabs() {
        for (int i = 0; i < tabsView.getChildCount(); i++) {
            if (!(tabsView.getChildAt(i) instanceof GlassTabView)) {
                continue;
            }
            GlassTabView tabView = (GlassTabView) tabsView.getChildAt(i);
            tabView.setClickable(false);
            tabView.setFocusable(false);
            tabView.setSelected(i == 0, false);
        }
    }

    private boolean isCallsTabEnabled() {
        return UserConfig.getInstance(currentAccount).showCallsTab;
    }

    private int getPreviewWidthForSlots(int slots) {
        return org.telegram.messenger.AndroidUtilities.dp(
                Math.round(DEFAULT_PREVIEW_WIDTH_DP * (slots / (float) DEFAULT_PREVIEW_SLOTS))
                        + DialogsActivity.MAIN_TABS_MARGIN * 2
        );
    }

    private int getTopGlassWidthForSlots(int slots) {
        int previewWidth = getPreviewWidthForSlots(slots);
        return Math.max(org.telegram.messenger.AndroidUtilities.dp(44), previewWidth - org.telegram.messenger.AndroidUtilities.dp(60));
    }

    private void updatePreviewWidth() {
        LayoutParams tabsLayoutParams = (LayoutParams) tabsView.getLayoutParams();
        LayoutParams topGlassLayoutParams = (LayoutParams) topGlassView.getLayoutParams();
        if (tabsLayoutParams == null || topGlassLayoutParams == null) {
            return;
        }
        int childCount = Math.max(1, tabsView.getChildCount());
        int targetWidth = childCount < DEFAULT_PREVIEW_SLOTS
                ? getPreviewWidthForSlots(childCount)
                : getPreviewWidthForSlots(DEFAULT_PREVIEW_SLOTS);
        if (tabsLayoutParams.width != targetWidth) {
            tabsLayoutParams.width = targetWidth;
            tabsView.setLayoutParams(tabsLayoutParams);
        }
        int targetTopGlassWidth = getTopGlassWidthForSlots(childCount < DEFAULT_PREVIEW_SLOTS ? childCount : DEFAULT_PREVIEW_SLOTS);
        if (topGlassLayoutParams.width != targetTopGlassWidth) {
            topGlassLayoutParams.width = targetTopGlassWidth;
            topGlassView.setLayoutParams(topGlassLayoutParams);
        }
    }

    private final class PreviewHost implements MainTabsUiHost {
        @Override
        public MainTabsActivity getActivity() {
            return null;
        }

        @Override
        public Context getContext() {
            return FluffyTabsNavbarPreviewView.this.getContext();
        }

        @Override
        public int getCurrentAccount() {
            return currentAccount;
        }

        @Override
        public boolean isCallsTabEnabled() {
            return FluffyTabsNavbarPreviewView.this.isCallsTabEnabled();
        }

        @Override
        public MainTabsLayout getTabsView() {
            return tabsView;
        }

        @Override
        public GlassTabView[] getTabs() {
            return previewTabs;
        }

        @Override
        public Theme.ResourcesProvider getResourceProvider() {
            return resourcesProvider;
        }

        @Override
        public boolean hasViewPager() {
            return false;
        }

        @Override
        public int getCurrentViewPagerPosition() {
            return 0;
        }

        @Override
        public void setViewPagerPosition(int position) {
        }

        @Override
        public void scrollViewPagerToPosition(int position) {
        }

        @Override
        public int getLastFragmentPosition() {
            return Math.max(0, tabsView.getChildCount() - 1);
        }

        @Override
        public ArrayList<Integer> collectNonRootFragmentPositions() {
            return new ArrayList<>();
        }

        @Override
        public void dropFragmentAtPosition(int position) {
        }

        @Override
        public void checkFadeView() {
        }

        @Override
        public boolean canHandleTabClick() {
            return false;
        }

        @Override
        public void scrollCurrentTabToTop() {
        }
    }
}
