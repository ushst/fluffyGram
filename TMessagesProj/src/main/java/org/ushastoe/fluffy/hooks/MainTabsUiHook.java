package org.ushastoe.fluffy.hooks;

import org.telegram.ui.Components.glass.GlassTabView;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.MainTabsLayout;
import org.ushastoe.fluffy.patches.MainTabsUiHost;
import org.ushastoe.fluffy.patches.MainTabsUiPatch;
import org.ushastoe.fluffy.patches.MainTabsUiState;

public final class MainTabsUiHook {

    private MainTabsUiHook() {
    }

    public static GlassTabView getTabViewForType(MainTabsActivity activity, GlassTabView[] tabs, int type) {
        return MainTabsUiPatch.getTabViewForType(activity, tabs, type);
    }

    public static int getTabTypeForIndex(int index) {
        return MainTabsUiPatch.getTabTypeForIndex(index);
    }

    public static MainTabsUiState createState() {
        return MainTabsUiPatch.createState();
    }

    public static boolean isTabIndexActive(MainTabsActivity activity, MainTabsUiState state, int index) {
        return MainTabsUiPatch.isTabIndexActive(activity, state, index);
    }

    public static boolean isTabSelected(MainTabsActivity activity, MainTabsUiState state, int index, int position) {
        return MainTabsUiPatch.isTabSelected(activity, state, index, position);
    }

    public static int getTabTypeAtPosition(MainTabsUiState state, int position) {
        return MainTabsUiPatch.getTabTypeAtPosition(state, position);
    }

    public static int getPositionForTabType(MainTabsUiState state, int type) {
        return MainTabsUiPatch.getPositionForTabType(state, type);
    }

    public static void rebuildTabsBar(MainTabsUiHost host, MainTabsUiState state) {
        MainTabsUiPatch.rebuildTabsBar(host, state);
    }

    public static void updateQuickDialogTabs(MainTabsUiHost host, MainTabsUiState state) {
        MainTabsUiPatch.updateQuickDialogTabs(host, state);
    }

    public static void updateQuickDialogCounters(MainTabsUiHost host, MainTabsUiState state, boolean animated) {
        MainTabsUiPatch.updateQuickDialogCounters(host, state, animated);
    }

    public static void applyMainTabsConfigIfNeeded(MainTabsUiHost host, MainTabsUiState state, boolean force) {
        MainTabsUiPatch.applyMainTabsConfigIfNeeded(host, state, force);
    }

    public static void onTabClicked(MainTabsUiHost host, MainTabsUiState state, int tabType, android.view.View anchor) {
        MainTabsUiPatch.onTabClicked(host, state, tabType, anchor);
    }

    public static boolean onTabLongClicked(MainTabsUiHost host, MainTabsUiState state, int tabType, android.view.View anchor) {
        return MainTabsUiPatch.onTabLongClicked(host, state, tabType, anchor);
    }

    public static boolean showNavbarSettingsMenu(MainTabsUiHost host, android.view.View anchor) {
        return MainTabsUiPatch.showNavbarSettingsMenu(host, anchor);
    }

    public static void applySelection(MainTabsActivity activity, MainTabsUiState state, GlassTabView[] tabs, int position, boolean animated) {
        MainTabsUiPatch.applySelection(activity, state, tabs, position, animated);
    }

    public static void applyGestureSelection(MainTabsActivity activity, MainTabsUiState state, GlassTabView[] tabs, MainTabsLayout tabsView, float animatedPosition, boolean allow) {
        MainTabsUiPatch.applyGestureSelection(activity, state, tabs, tabsView, animatedPosition, allow);
    }
}
