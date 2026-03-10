package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.EmojiTabsSelectionPatch;

public final class EmojiTabsSelectionHook {

    private EmojiTabsSelectionHook() {
    }

    public static boolean isAnimatedPackSelected(int selectedChildIndex, int packsIndexStart, int packIndex) {
        return EmojiTabsSelectionPatch.isAnimatedPackSelected(selectedChildIndex, packsIndexStart, packIndex);
    }

    public static int resolveVisiblePosition(int type, int firstVisiblePosition, int firstCompletelyVisiblePosition) {
        return EmojiTabsSelectionPatch.resolveVisiblePosition(type, firstVisiblePosition, firstCompletelyVisiblePosition);
    }

    public static int getPackSectionIndexFromRawTabIndex(int rawTabIndex, boolean hasToggleEmojiTab, boolean hasRecentTab, boolean hasGiftsTab) {
        return EmojiTabsSelectionPatch.getPackSectionIndexFromRawTabIndex(rawTabIndex, hasToggleEmojiTab, hasRecentTab, hasGiftsTab);
    }

    public static int getRawTabIndexForSection(int sectionIndex, boolean hasToggleEmojiTab, boolean hasRecentTab, boolean hasGiftsTab) {
        return EmojiTabsSelectionPatch.getRawTabIndexForSection(sectionIndex, hasToggleEmojiTab, hasRecentTab, hasGiftsTab);
    }

    public static int getGiftsRawTabIndex(boolean hasToggleEmojiTab, boolean hasRecentTab) {
        return EmojiTabsSelectionPatch.getGiftsRawTabIndex(hasToggleEmojiTab, hasRecentTab);
    }

    public static int getPackScrollPosition(int sectionStartPosition) {
        return EmojiTabsSelectionPatch.getPackScrollPosition(sectionStartPosition);
    }

    public static boolean isPositionInsidePack(int position, int startPosition, int count) {
        return EmojiTabsSelectionPatch.isPositionInsidePack(position, startPosition, count);
    }
}
