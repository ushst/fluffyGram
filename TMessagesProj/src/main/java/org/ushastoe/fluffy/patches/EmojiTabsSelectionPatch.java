package org.ushastoe.fluffy.patches;

public final class EmojiTabsSelectionPatch {

    private EmojiTabsSelectionPatch() {
    }

    public static boolean isAnimatedPackSelected(int selectedChildIndex, int packsIndexStart, int packIndex) {
        return selectedChildIndex >= packsIndexStart && selectedChildIndex - packsIndexStart == packIndex;
    }

    public static int resolveVisiblePosition(int type, int firstVisiblePosition, int firstCompletelyVisiblePosition) {
        if (type == org.telegram.ui.SelectAnimatedEmojiDialog.TYPE_SET_REPLY_ICON
                || type == org.telegram.ui.SelectAnimatedEmojiDialog.TYPE_SET_REPLY_ICON_BOTTOM) {
            return firstVisiblePosition;
        }
        return firstCompletelyVisiblePosition;
    }

    public static int getRawLeadingTabCount(boolean hasToggleEmojiTab, boolean hasRecentTab, boolean hasGiftsTab) {
        return (hasToggleEmojiTab ? 1 : 0) + (hasRecentTab ? 1 : 0) + (hasGiftsTab ? 1 : 0);
    }

    public static int getPackSectionIndexFromRawTabIndex(int rawTabIndex, boolean hasToggleEmojiTab, boolean hasRecentTab, boolean hasGiftsTab) {
        return rawTabIndex - getRawLeadingTabCount(hasToggleEmojiTab, hasRecentTab, hasGiftsTab);
    }

    public static int getRawTabIndexForSection(int sectionIndex, boolean hasToggleEmojiTab, boolean hasRecentTab, boolean hasGiftsTab) {
        return getRawLeadingTabCount(hasToggleEmojiTab, hasRecentTab, hasGiftsTab) + sectionIndex;
    }

    public static int getGiftsRawTabIndex(boolean hasToggleEmojiTab, boolean hasRecentTab) {
        return (hasToggleEmojiTab ? 1 : 0) + (hasRecentTab ? 1 : 0);
    }

    public static int getPackScrollPosition(int sectionStartPosition) {
        return Math.max(0, sectionStartPosition + 1);
    }

    public static boolean isPositionInsidePack(int position, int startPosition, int count) {
        return position >= startPosition && position <= startPosition + 1 + count;
    }
}
