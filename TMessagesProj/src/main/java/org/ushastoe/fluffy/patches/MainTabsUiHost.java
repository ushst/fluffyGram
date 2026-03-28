package org.ushastoe.fluffy.patches;

import android.content.Context;

import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.MainTabsLayout;
import org.telegram.ui.Components.glass.GlassTabView;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

public interface MainTabsUiHost {
    MainTabsActivity getActivity();

    Context getContext();

    int getCurrentAccount();

    boolean isCallsTabEnabled();

    MainTabsLayout getTabsView();

    GlassTabView[] getTabs();

    Theme.ResourcesProvider getResourceProvider();

    boolean hasViewPager();

    int getCurrentViewPagerPosition();

    void setViewPagerPosition(int position);

    void scrollViewPagerToPosition(int position);

    int getLastFragmentPosition();

    ArrayList<Integer> collectNonRootFragmentPositions();

    void dropFragmentAtPosition(int position);

    void checkFadeView();

    boolean canHandleTabClick();

    void scrollCurrentTabToTop();
}
