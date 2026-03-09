package org.ushastoe.fluffy.patches;

import android.util.LongSparseArray;

import org.telegram.ui.Components.glass.GlassTabView;

public final class MainTabsUiState {
    public int[] visibleTabTypes;
    public long[] quickDialogIds;
    public String appliedTabsSignature;
    public boolean appliedShowCallsTab;
    public final LongSparseArray<GlassTabView> quickDialogTabs = new LongSparseArray<>();
}
