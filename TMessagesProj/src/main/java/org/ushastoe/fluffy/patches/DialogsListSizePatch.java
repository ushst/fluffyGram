package org.ushastoe.fluffy.patches;

import static org.telegram.messenger.AndroidUtilities.dp;

import org.telegram.ui.ActionBar.Theme;

public final class DialogsListSizePatch {

    private DialogsListSizePatch() {
    }

    public static void applyThemePaintSizes() {
        if (Theme.dialogs_countTextPaint != null) {
            Theme.dialogs_countTextPaint.setTextSize(dp(scaleText(12)));
        }
        if (Theme.dialogs_countTextPaint2 != null) {
            Theme.dialogs_countTextPaint2.setTextSize(dp(scaleText(13)));
        }
        if (Theme.dialogs_messageNamePaint != null) {
            Theme.dialogs_messageNamePaint.setTextSize(dp(scaleText(14)));
        }
        if (Theme.dialogs_timePaint != null) {
            Theme.dialogs_timePaint.setTextSize(dp(scaleText(12)));
        }
        if (Theme.dialogs_timePaintBold != null) {
            Theme.dialogs_timePaintBold.setTextSize(dp(scaleText(12)));
        }
        if (Theme.dialogs_timePaintBoldAccent != null) {
            Theme.dialogs_timePaintBoldAccent.setTextSize(dp(scaleText(12)));
        }
        if (Theme.dialogs_archiveTextPaint != null) {
            Theme.dialogs_archiveTextPaint.setTextSize(dp(scaleText(13)));
        }
        if (Theme.dialogs_archiveTextPaintSmall != null) {
            Theme.dialogs_archiveTextPaintSmall.setTextSize(dp(scaleText(11)));
        }
        if (Theme.dialogs_onlinePaint != null) {
            Theme.dialogs_onlinePaint.setTextSize(dp(scaleText(15)));
        }
        if (Theme.dialogs_offlinePaint != null) {
            Theme.dialogs_offlinePaint.setTextSize(dp(scaleText(15)));
        }
        if (Theme.dialogs_searchNamePaint != null) {
            Theme.dialogs_searchNamePaint.setTextSize(dp(scaleText(16)));
        }
        if (Theme.dialogs_searchNameEncryptedPaint != null) {
            Theme.dialogs_searchNameEncryptedPaint.setTextSize(dp(scaleText(16)));
        }
        if (Theme.dialogs_tagTextPaint != null) {
            Theme.dialogs_tagTextPaint.setTextSize(dp(Math.max(9, scaleText(10))));
        }
    }

    public static void applyDialogCellTextSizes(boolean threeLines) {
        if (Theme.dialogs_namePaint == null || Theme.dialogs_messagePaint == null || Theme.dialogs_messagePrintingPaint == null || Theme.dialogs_nameEncryptedPaint == null) {
            return;
        }
        if (threeLines) {
            Theme.dialogs_namePaint[0].setTextSize(dp(scaleText(17)));
            Theme.dialogs_nameEncryptedPaint[0].setTextSize(dp(scaleText(17)));
            Theme.dialogs_messagePaint[0].setTextSize(dp(scaleText(16)));
            Theme.dialogs_messagePrintingPaint[0].setTextSize(dp(scaleText(16)));

            Theme.dialogs_namePaint[1].setTextSize(dp(scaleText(16)));
            Theme.dialogs_nameEncryptedPaint[1].setTextSize(dp(scaleText(16)));
            Theme.dialogs_messagePaint[1].setTextSize(dp(scaleText(15)));
            Theme.dialogs_messagePrintingPaint[1].setTextSize(dp(scaleText(15)));
        } else {
            Theme.dialogs_namePaint[0].setTextSize(dp(scaleText(17)));
            Theme.dialogs_nameEncryptedPaint[0].setTextSize(dp(scaleText(17)));
            Theme.dialogs_messagePaint[0].setTextSize(dp(scaleText(16)));
            Theme.dialogs_messagePrintingPaint[0].setTextSize(dp(scaleText(16)));
        }
    }

    public static int adjustDialogCellHeight(int defaultDp) {
        float scale = getHeightScale();
        return Math.max(52, Math.round(defaultDp * scale));
    }

    public static int adjustDialogThumbSize(int defaultDp) {
        return Math.max(16, Math.round(defaultDp * getThumbScale()));
    }

    public static int getDialogAvatarSize(int defaultDp) {
        return defaultDp;
    }

    public static int getDialogAvatarTop(int defaultTopDp, int defaultSizeDp, int scaledSizeDp) {
        return defaultTopDp;
    }

    public static int getDialogAvatarRoundRadius(int defaultDp) {
        return defaultDp;
    }

    private static int scaleText(int base) {
        return Math.max(9, Math.round(base * getTextScale()));
    }

    private static float getTextScale() {
        return AppearanceSettingsPatch.getDialogsListScale() / 100f;
    }

    private static float getHeightScale() {
        return 1f + ((AppearanceSettingsPatch.getDialogsListScale() - 100) * 0.006f);
    }

    private static float getThumbScale() {
        return 1f + ((AppearanceSettingsPatch.getDialogsListScale() - 100) * 0.005f);
    }

}
