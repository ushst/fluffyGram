package org.ushastoe.fluffy.patches;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterTopView;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;
import org.ushastoe.fluffy.smartreply.SmartReplyLog;
import org.ushastoe.fluffy.smartreply.SmartReplyPredictor;
import org.ushastoe.fluffy.ui.components.SmartReplyBarView;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

/**
 * Smart replies expand the input island upward via {@code topView}
 * (same rectangular growth as the stock reply panel).
 */
public final class SmartReplyPredictorPatch {

    private static final WeakHashMap<ChatActivity, SmartReplyBarView> BARS = new WeakHashMap<>();
    private static final WeakHashMap<ChatActivity, String> LAST_INCOMING = new WeakHashMap<>();
    private static final WeakHashMap<ChatActivity, Boolean> SMART_REPLY_HOLDING_TOP = new WeakHashMap<>();

    private enum HideReason {
        NO_MESSAGES,
        LAST_IS_MINE,
        LAST_NOT_TEXT,
        NOT_ELIGIBLE
    }

    private static HideReason lastHideReason = HideReason.NO_MESSAGES;

    private SmartReplyPredictorPatch() {
    }

    public static void attach(ChatActivity activity,
                              android.view.ViewGroup contentView,
                              BlurredBackgroundDrawableViewFactory glassFactory) {
        if (activity == null || contentView == null) {
            SmartReplyLog.w("attach skipped: activity or contentView null");
            return;
        }
        if (!AppearanceSettingsHook.isSmartReplyEnabled()) {
            SmartReplyLog.d("attach skipped: smart reply disabled in settings");
            return;
        }
        if (BARS.containsKey(activity)) {
            SmartReplyLog.d("attach skipped: bar already attached");
            return;
        }
        if (activity.getCurrentEncryptedChat() != null) {
            SmartReplyLog.d("attach skipped: encrypted chat");
            return;
        }
        if (!DialogObject.isUserDialog(activity.getDialogId()) || activity.getCurrentChat() != null) {
            SmartReplyLog.d("attach skipped: not a private user chat");
            return;
        }
        ChatActivityEnterTopView topView = activity.fluffyGetEnterTopView();
        if (topView == null || activity.getChatActivityEnterView() == null) {
            SmartReplyLog.w("attach skipped: enter topView null");
            return;
        }

        SmartReplyPredictor.getInstance().preload(activity.getContext());
        SmartReplyPredictor.getInstance().whenReady(() ->
                AndroidUtilities.runOnUIThread(() -> {
                    if (BARS.get(activity) != null) {
                        refreshFromLastIncoming(activity);
                    }
                }));

        SmartReplyBarView bar = new SmartReplyBarView(activity.getContext(), activity.getResourceProvider());
        bar.setListener(text -> onChipClick(activity, text));
        bar.setVisibilityListener(visible -> onBarVisibilityChanged(activity, visible));

        // Sit at the bottom of the island topView — same surface as "В ответ …".
        topView.addView(bar, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, SmartReplyBarView.ROW_HEIGHT_DP,
                Gravity.BOTTOM | Gravity.FILL_HORIZONTAL, 0, 0, 0, 0));
        BARS.put(activity, bar);
        SMART_REPLY_HOLDING_TOP.put(activity, false);
        SmartReplyLog.d("attach ok inside enter topView");

        updateIslandPlacement(activity);
    }

    public static void detach(ChatActivity activity) {
        SmartReplyBarView bar = BARS.remove(activity);
        LAST_INCOMING.remove(activity);
        SMART_REPLY_HOLDING_TOP.remove(activity);
        if (activity != null) {
            ChatActivityEnterView enterView = activity.getChatActivityEnterView();
            if (enterView != null) {
                enterView.fluffyHideTopViewForSmartReply(false);
            }
            restoreReplyRowLayout(activity, false);
        }
        if (bar != null && bar.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) bar.getParent()).removeView(bar);
        }
        SmartReplyLog.d("detach");
    }

    public static void updatePosition(ChatActivity activity, float animatedMaxBottomInset) {
        // Insets are handled by the input island; do not re-layout chips every animation frame.
    }

    public static float getExtraChatListBottomPadding(ChatActivity activity) {
        return 0;
    }

    public static void onNewMessages(ChatActivity activity, ArrayList<MessageObject> messages) {
        if (activity == null || messages == null || messages.isEmpty()) {
            return;
        }
        if (!AppearanceSettingsHook.isSmartReplyEnabled()) {
            hide(activity);
            return;
        }
        if (BARS.get(activity) == null) {
            return;
        }
        refreshFromLastIncoming(activity);
    }

    public static void onFieldPanelChanged(ChatActivity activity) {
        if (activity == null || !AppearanceSettingsHook.isSmartReplyEnabled()) {
            return;
        }
        if (BARS.get(activity) == null) {
            return;
        }
        boolean reply = getActiveReplyTarget(activity) != null;
        SmartReplyLog.d("field panel changed reply=" + reply);
        if (!reply) {
            // Reply/edit panel closed — drop reply-scoped chips and re-evaluate last message.
            LAST_INCOMING.remove(activity);
            SmartReplyBarView bar = BARS.get(activity);
            if (bar != null) {
                bar.clear();
            }
            SMART_REPLY_HOLDING_TOP.put(activity, false);
        }
        refreshFromLastIncoming(activity);
    }

    public static void refreshFromLastIncoming(ChatActivity activity) {
        if (activity == null || !AppearanceSettingsHook.isSmartReplyEnabled()) {
            return;
        }
        SmartReplyBarView bar = BARS.get(activity);
        if (bar == null) {
            return;
        }
        if (!isPrivateUserChat(activity)) {
            hide(activity);
            return;
        }

        MessageObject replyTarget = getActiveReplyTarget(activity);
        if (replyTarget != null) {
            String replyText = extractText(replyTarget);
            if (TextUtils.isEmpty(replyText)) {
                SmartReplyLog.d("refresh: reply target has no text — keep chips=" + bar.hasSuggestions());
                updateIslandPlacement(activity);
                return;
            }
            String key = "reply:" + replyTarget.getId() + ":" + replyText;
            String prev = LAST_INCOMING.get(activity);
            // Same text already shown as last-incoming or previous reply — just expand island.
            if (TextUtils.equals(key, prev)
                    || TextUtils.equals("last:" + replyText, prev)
                    || (bar.isVisibleForLayout() && prev != null && prev.endsWith(":" + replyText))) {
                LAST_INCOMING.put(activity, key);
                updateIslandPlacement(activity);
                SmartReplyLog.d("refresh reply reuse chips for \"" + preview(replyText) + "\"");
                return;
            }
            SmartReplyLog.d("refresh reply=\"" + preview(replyText) + "\" id=" + replyTarget.getId());
            applySuggestions(activity, bar, replyText, key);
            return;
        }

        String text = findSuggestionTextFromLastMessage(activity.messages);
        if (text == null) {
            SmartReplyLog.d("refresh: hide (" + lastHideReason + ")");
            forceHide(activity);
            return;
        }
        String key = "last:" + text;
        if (TextUtils.equals(key, LAST_INCOMING.get(activity))) {
            updateIslandPlacement(activity);
            return;
        }
        SmartReplyLog.d("refresh incoming=\"" + preview(text) + "\"");
        applySuggestions(activity, bar, text, key);
    }

    public static void hide(ChatActivity activity) {
        // Don't clear suggestions while a reply target is still selected.
        if (getActiveReplyTarget(activity) != null) {
            SmartReplyLog.d("hide ignored — reply still active");
            updateIslandPlacement(activity);
            return;
        }
        forceHide(activity);
    }

    private static void forceHide(ChatActivity activity) {
        SmartReplyBarView bar = BARS.get(activity);
        if (bar != null) {
            bar.clear();
        }
        LAST_INCOMING.remove(activity);
        updateIslandPlacement(activity);
    }

    public static void onPause(ChatActivity activity) {
        SmartReplyBarView bar = BARS.get(activity);
        if (bar != null) {
            bar.setVisibility(View.GONE);
            bar.setAlpha(0f);
        }
    }

    public static void onResume(ChatActivity activity) {
        SmartReplyBarView bar = BARS.get(activity);
        if (bar == null) {
            return;
        }
        if (!isPrivateUserChat(activity)) {
            hide(activity);
            return;
        }
        LAST_INCOMING.remove(activity);
        refreshFromLastIncoming(activity);
    }

    public static void onSettingChanged() {
        AndroidUtilities.runOnUIThread(() -> {
            ArrayList<ChatActivity> activities = new ArrayList<>(BARS.keySet());
            if (!AppearanceSettingsHook.isSmartReplyEnabled()) {
                for (ChatActivity activity : activities) {
                    forceHide(activity);
                }
                SmartReplyLog.d("setting off — hid " + activities.size() + " bars");
                return;
            }
            for (ChatActivity activity : activities) {
                LAST_INCOMING.remove(activity);
                refreshFromLastIncoming(activity);
            }
            SmartReplyLog.d("setting on — refreshed " + activities.size() + " bars");
        });
    }

    private static boolean isPrivateUserChat(ChatActivity activity) {
        return activity != null
                && DialogObject.isUserDialog(activity.getDialogId())
                && activity.getCurrentChat() == null
                && activity.getCurrentEncryptedChat() == null;
    }

    private static MessageObject getActiveReplyTarget(ChatActivity activity) {
        MessageObject reply = activity.fluffyGetReplyingMessageObject();
        if (reply == null) {
            ChatActivityEnterView enterView = activity.getChatActivityEnterView();
            if (enterView != null) {
                reply = enterView.getReplyingMessageObject();
            }
        }
        if (reply == null || !isRealChatMessage(reply)) {
            return null;
        }
        MessageObject thread = activity.getThreadMessage();
        if (thread != null && reply == thread) {
            return null;
        }
        if (reply.isTopicMainMessage) {
            return null;
        }
        return reply;
    }

    private static String findSuggestionTextFromLastMessage(ArrayList<MessageObject> messages) {
        lastHideReason = HideReason.NO_MESSAGES;
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        // ChatActivity.messages: index 0 = newest. Older history is toward size-1.
        for (int i = 0; i < messages.size(); i++) {
            MessageObject msg = messages.get(i);
            if (!isRealChatMessage(msg)) {
                continue;
            }
            if (msg.isOutOwner()) {
                lastHideReason = HideReason.LAST_IS_MINE;
                return null;
            }
            if (!isEligibleIncoming(msg)) {
                lastHideReason = HideReason.NOT_ELIGIBLE;
                return null;
            }
            String text = extractText(msg);
            if (TextUtils.isEmpty(text)) {
                lastHideReason = HideReason.LAST_NOT_TEXT;
                return null;
            }
            return text;
        }
        return null;
    }

    private static boolean isRealChatMessage(MessageObject msg) {
        if (msg == null || msg.isSponsored()) {
            return false;
        }
        if (msg.isDateObject || msg.type == MessageObject.TYPE_DATE) {
            return false;
        }
        if (msg.messageOwner != null && msg.messageOwner.action != null) {
            return false;
        }
        return msg.messageOwner == null || msg.messageOwner.id > 0;
    }

    private static void applySuggestions(ChatActivity activity, SmartReplyBarView bar, String incoming, String cacheKey) {
        ChatActivityEnterView enterView = activity.getChatActivityEnterView();
        if (enterView != null && enterView.isStickersExpanded()) {
            SmartReplyLog.d("applySuggestions: stickers expanded");
            updateIslandPlacement(activity);
            return;
        }
        SmartReplyPredictor predictor = SmartReplyPredictor.getInstance();
        if (!predictor.isReady()) {
            SmartReplyLog.d("applySuggestions: db not ready, defer");
            predictor.preload(activity.getContext());
            predictor.whenReady(() -> AndroidUtilities.runOnUIThread(() -> {
                if (BARS.get(activity) == bar) {
                    LAST_INCOMING.remove(activity);
                    refreshFromLastIncoming(activity);
                }
            }));
            return;
        }
        List<String> chips = predictor.getSuggestions(incoming);
        if (chips.isEmpty()) {
            SmartReplyLog.d("applySuggestions chips=0 for \"" + preview(incoming) + "\"");
            // Keep already visible chips (e.g. opened reply on the same message).
            if (bar.isVisibleForLayout() || bar.hasSuggestions()) {
                LAST_INCOMING.put(activity, cacheKey);
                updateIslandPlacement(activity);
                return;
            }
            LAST_INCOMING.put(activity, cacheKey);
            bar.clear();
            updateIslandPlacement(activity);
            return;
        }
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < chips.size(); i++) {
            if (i > 0) {
                all.append(" | ");
            }
            all.append(preview(chips.get(i)));
        }
        SmartReplyLog.d("applySuggestions chips=" + chips.size() + " → [" + all + "]");
        LAST_INCOMING.put(activity, cacheKey);
        bar.setSuggestions(chips);
        updateIslandPlacement(activity);
    }

    private static void onBarVisibilityChanged(ChatActivity activity, boolean visible) {
        updateIslandPlacement(activity);
        activity.fluffyInvalidateSmartReplyLayout();
        SmartReplyLog.d("bar visibility=" + visible);
    }

    /**
     * Grow / shrink the input island topView around the chip row —
     * identical visual language to the stock reply rectangle.
     */
    private static void updateIslandPlacement(ChatActivity activity) {
        if (activity == null) {
            return;
        }
        SmartReplyBarView bar = BARS.get(activity);
        ChatActivityEnterView enterView = activity.getChatActivityEnterView();
        ChatActivityEnterTopView topView = activity.fluffyGetEnterTopView();
        if (bar == null || enterView == null || topView == null) {
            return;
        }

        boolean chips = bar.isVisibleForLayout();
        boolean reply = getActiveReplyTarget(activity) != null;
        // Editing/forward also use topView — keep their row if Telegram is showing it.
        boolean telegramPanel = enterView.isTopViewVisible() || reply;

        if (chips) {
            int heightDp = reply || telegramPanelHasReplyRow(activity)
                    ? SmartReplyBarView.REPLY_PANEL_BASE_DP + SmartReplyBarView.ROW_HEIGHT_DP
                    : SmartReplyBarView.ROW_HEIGHT_DP;
            layoutReplyRowForChips(activity, reply || telegramPanelHasReplyRow(activity));
            bar.setVisibility(View.VISIBLE);
            enterView.fluffyShowTopViewForSmartReply(AndroidUtilities.dp(heightDp), true);
            SMART_REPLY_HOLDING_TOP.put(activity, true);
            SmartReplyLog.d("island expand heightDp=" + heightDp + " reply=" + reply);
        } else {
            restoreReplyRowLayout(activity, reply);
            Boolean holding = SMART_REPLY_HOLDING_TOP.get(activity);
            if (holding != null && holding) {
                if (reply) {
                    enterView.fluffyUpdateTopViewHeight(AndroidUtilities.dp(SmartReplyBarView.REPLY_PANEL_BASE_DP));
                } else {
                    enterView.fluffyHideTopViewForSmartReply(true);
                }
                SMART_REPLY_HOLDING_TOP.put(activity, false);
            }
        }
    }

    private static boolean telegramPanelHasReplyRow(ChatActivity activity) {
        ChatActivityEnterTopView topView = activity.fluffyGetEnterTopView();
        if (topView == null) {
            return false;
        }
        View replyView = topView.getReplyView();
        return replyView != null && replyView.getVisibility() == View.VISIBLE
                && getActiveReplyTarget(activity) != null;
    }

    private static void layoutReplyRowForChips(ChatActivity activity, boolean showReplyRow) {
        ChatActivityEnterTopView topView = activity.fluffyGetEnterTopView();
        if (topView == null) {
            return;
        }
        View replyView = topView.getReplyView();
        ImageView close = activity.fluffyGetReplyCloseImageView();
        SmartReplyBarView bar = BARS.get(activity);
        int chipsH = AndroidUtilities.dp(SmartReplyBarView.ROW_HEIGHT_DP);
        int replyH = AndroidUtilities.dp(SmartReplyBarView.REPLY_PANEL_BASE_DP);

        if (showReplyRow) {
            // Stack: [chips higher] → [reply] → [input]
            if (bar != null && bar.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams blp = (FrameLayout.LayoutParams) bar.getLayoutParams();
                blp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
                blp.height = chipsH;
                blp.topMargin = 0;
                bar.setLayoutParams(blp);
                topView.bringChildToFront(bar);
            }
            if (replyView != null) {
                replyView.setVisibility(View.VISIBLE);
                if (replyView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams rlp = (FrameLayout.LayoutParams) replyView.getLayoutParams();
                    rlp.height = replyH;
                    rlp.gravity = Gravity.TOP | Gravity.FILL_HORIZONTAL;
                    rlp.topMargin = chipsH;
                    rlp.rightMargin = AndroidUtilities.dp(52);
                    replyView.setLayoutParams(rlp);
                }
            }
            if (close != null) {
                close.setVisibility(View.VISIBLE);
                if (close.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams clp = (FrameLayout.LayoutParams) close.getLayoutParams();
                    clp.gravity = Gravity.RIGHT | Gravity.TOP;
                    clp.topMargin = chipsH + AndroidUtilities.dp(0.5f);
                    close.setLayoutParams(clp);
                }
                topView.bringChildToFront(close);
            }
        } else {
            // Chips-only: suggestions are the whole upward rectangle.
            if (replyView != null) {
                replyView.setVisibility(View.INVISIBLE);
            }
            if (close != null) {
                close.setVisibility(View.INVISIBLE);
            }
            if (bar != null && bar.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams blp = (FrameLayout.LayoutParams) bar.getLayoutParams();
                blp.gravity = Gravity.FILL;
                blp.height = chipsH;
                blp.topMargin = 0;
                bar.setLayoutParams(blp);
                topView.bringChildToFront(bar);
            }
        }
    }

    private static void restoreReplyRowLayout(ChatActivity activity, boolean replyActive) {
        ChatActivityEnterTopView topView = activity.fluffyGetEnterTopView();
        if (topView == null) {
            return;
        }
        View replyView = topView.getReplyView();
        ImageView close = activity.fluffyGetReplyCloseImageView();
        if (replyView != null) {
            if (replyView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams rlp = (FrameLayout.LayoutParams) replyView.getLayoutParams();
                rlp.height = LayoutHelper.MATCH_PARENT;
                rlp.gravity = Gravity.NO_GRAVITY;
                rlp.topMargin = 0;
                rlp.rightMargin = AndroidUtilities.dp(52);
                replyView.setLayoutParams(rlp);
            }
            replyView.setVisibility(View.VISIBLE);
        }
        if (close != null) {
            if (close.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams clp = (FrameLayout.LayoutParams) close.getLayoutParams();
                clp.gravity = Gravity.RIGHT | Gravity.TOP;
                clp.topMargin = AndroidUtilities.dp(0.5f);
                close.setLayoutParams(clp);
            }
            if (replyActive) {
                close.setVisibility(View.VISIBLE);
            }
        }
    }

    private static void onChipClick(ChatActivity activity, String text) {
        ChatActivityEnterView enterView = activity.getChatActivityEnterView();
        if (enterView == null || TextUtils.isEmpty(text)) {
            return;
        }
        CharSequence current = enterView.getFieldText();
        if (current == null || current.length() == 0) {
            enterView.setFieldText(text);
        } else {
            enterView.setFieldText(current.toString() + " " + text);
        }
        if (enterView.getEditField() != null) {
            enterView.getEditField().requestFocus();
            AndroidUtilities.showKeyboard(enterView.getEditField());
        }
        hide(activity);
    }

    private static boolean isEligibleIncoming(MessageObject msg) {
        if (msg == null || msg.isOutOwner() || msg.isSponsored()) {
            return false;
        }
        if (msg.isDateObject || msg.type == MessageObject.TYPE_DATE) {
            return false;
        }
        if (msg.messageOwner != null) {
            if (msg.messageOwner.id <= 0) {
                return false;
            }
            if (msg.messageOwner.action != null) {
                return false;
            }
        }
        return true;
    }

    private static String extractText(MessageObject msg) {
        if (msg == null) {
            return null;
        }
        String rawMessage = msg.messageOwner != null ? msg.messageOwner.message : null;
        if (!TextUtils.isEmpty(rawMessage)) {
            return rawMessage;
        }
        if (msg.caption != null && !TextUtils.isEmpty(msg.caption)) {
            return msg.caption.toString();
        }
        if (!msg.isMediaEmpty() && !isWebpageMedia(msg)) {
            return null;
        }
        if (msg.messageText != null && !TextUtils.isEmpty(msg.messageText)) {
            return msg.messageText.toString();
        }
        return null;
    }

    private static boolean isWebpageMedia(MessageObject msg) {
        if (msg.messageOwner == null || msg.messageOwner.media == null) {
            return false;
        }
        return msg.messageOwner.media instanceof org.telegram.tgnet.TLRPC.TL_messageMediaWebPage;
    }

    private static String preview(String text) {
        if (text == null) {
            return "";
        }
        String oneLine = text.replace('\n', ' ').trim();
        if (oneLine.length() <= 48) {
            return oneLine;
        }
        return oneLine.substring(0, 48) + "…";
    }
}
