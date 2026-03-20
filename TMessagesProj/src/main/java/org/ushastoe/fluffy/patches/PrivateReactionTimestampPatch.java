package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.view.View;

import java.util.Calendar;
import java.util.Date;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.ui.components.PrivateReactionTimestampRowView;

public final class PrivateReactionTimestampPatch {

    private PrivateReactionTimestampPatch() {
    }

    public static boolean addPrivateReactionRow(ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout,
            Context context, MessageObject messageObject, Theme.ResourcesProvider resourcesProvider) {
        if (popupLayout == null || context == null) {
            return false;
        }
        int reactionDate = getLatestExternalReactionDate(messageObject);
        if (reactionDate <= 0) {
            return false;
        }
        View view = new PrivateReactionTimestampRowView(context, formatReactionDate(reactionDate), resourcesProvider);
        popupLayout.addView(view, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
        return true;
    }

    private static int getLatestExternalReactionDate(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null || messageObject.messageOwner.reactions == null
                || messageObject.messageOwner.reactions.recent_reactions == null) {
            return 0;
        }
        long selfUserId = UserConfig.getInstance(messageObject.currentAccount).getClientUserId();
        int latestDate = 0;
        for (TLRPC.MessagePeerReaction reaction : messageObject.messageOwner.reactions.recent_reactions) {
            if (reaction == null || reaction.date <= 0) {
                continue;
            }
            if (MessageObject.getPeerId(reaction.peer_id) == selfUserId) {
                continue;
            }
            if (reaction.date > latestDate) {
                latestDate = reaction.date;
            }
        }
        return latestDate;
    }

    private static String formatReactionDate(long date) {
        try {
            long timestamp = date * 1000L;
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);

            rightNow.setTimeInMillis(timestamp);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return LocaleController.formatString(R.string.FluffyReactionTodayAt,
                        LocaleController.getInstance().getFormatterDay().format(new Date(timestamp)));
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString(R.string.FluffyReactionYesterdayAt,
                        LocaleController.getInstance().getFormatterDay().format(new Date(timestamp)));
            } else if (Math.abs(System.currentTimeMillis() - timestamp) < 31536000000L) {
                return LocaleController.formatString(R.string.FluffyReactionDateTimeAt,
                        LocaleController.getInstance().getFormatterDayMonth().format(new Date(timestamp)),
                        LocaleController.getInstance().getFormatterDay().format(new Date(timestamp)));
            } else {
                return LocaleController.formatString(R.string.FluffyReactionDateTimeAt,
                        LocaleController.getInstance().getFormatterYear().format(new Date(timestamp)),
                        LocaleController.getInstance().getFormatterDay().format(new Date(timestamp)));
            }
        } catch (Exception e) {
            return LocaleController.formatString(R.string.FluffyReactionTodayAt,
                    LocaleController.getInstance().getFormatterDay().format(new Date(System.currentTimeMillis())));
        }
    }
}
