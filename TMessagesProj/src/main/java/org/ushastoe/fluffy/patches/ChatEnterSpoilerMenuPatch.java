package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.text.Editable;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.URLSpanReplacement;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;

import java.util.WeakHashMap;

public final class ChatEnterSpoilerMenuPatch {

    private static final int ITEM_HEIGHT_DP = 40;
    private static final int ITEM_MIN_WIDTH_DP = 196;
    private static final WeakHashMap<Object, ActionBarPopupWindow> POPUPS = new WeakHashMap<>();

    private ChatEnterSpoilerMenuPatch() {
    }

    public static boolean onEmojiButtonLongClick(Object owner, View anchor, Context context, Theme.ResourcesProvider resourcesProvider, EditTextCaption editText, ChatActivity parentFragment) {
        if (owner == null || anchor == null || context == null || editText == null) {
            return false;
        }
        if (!AppearanceSettingsHook.isChatEnterSpoilerMenuEnabled()) {
            return false;
        }

        dismissPopup(owner);
        ActionBarPopupWindow.ActionBarPopupWindowLayout layout = createLayout(owner, context, resourcesProvider, editText, parentFragment);
        if (layout.getChildCount() == 0) {
            return false;
        }
        ActionBarPopupWindow popupWindow = createPopup(layout);

        layout.measure(
                View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST)
        );

        int[] location = new int[2];
        anchor.getLocationInWindow(location);
        int popupX = location[0] + (anchor.getMeasuredWidth() - layout.getMeasuredWidth()) / 2;
        int popupY = location[1] - layout.getMeasuredHeight() - AndroidUtilities.dp(2);
        popupX = Math.max(popupX, AndroidUtilities.dp(4));
        popupY = Math.max(popupY, AndroidUtilities.dp(4));

        popupWindow.setFocusable(true);
        popupWindow.showAtLocation(anchor, Gravity.LEFT | Gravity.TOP, popupX, popupY);
        POPUPS.put(owner, popupWindow);
        return true;
    }

    private static ActionBarPopupWindow.ActionBarPopupWindowLayout createLayout(Object owner, Context context, Theme.ResourcesProvider resourcesProvider, EditTextCaption editText, ChatActivity parentFragment) {
        ActionBarPopupWindow.ActionBarPopupWindowLayout layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, resourcesProvider);
        layout.setAnimationEnabled(false);
        layout.setShownFromBottom(false);
        layout.setupRadialSelectors(Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider));
        Editable text = editText.getText();
        boolean hasText = text != null && text.length() > 0;

        ChatEnterGoogleAiGeneratePatch.maybeAddMenuItem(
                context,
                resourcesProvider,
                layout,
                ITEM_HEIGHT_DP,
                ITEM_MIN_WIDTH_DP,
                owner,
                () -> dismissPopup(owner),
                editText,
                parentFragment
        );
        ChatEnterTemplateFillPatch.maybeAddMenuItem(
                context,
                resourcesProvider,
                layout,
                ITEM_HEIGHT_DP,
                ITEM_MIN_WIDTH_DP,
                () -> dismissPopup(owner),
                editText
        );
        ChatEnterChannelSubscribePatch.maybeAddMenuItem(
                context,
                resourcesProvider,
                layout,
                ITEM_HEIGHT_DP,
                ITEM_MIN_WIDTH_DP,
                () -> dismissPopup(owner),
                editText,
                parentFragment
        );

        if (hasText) {
            ActionBarMenuSubItem spoilerItem = new ActionBarMenuSubItem(context, false, true, resourcesProvider);
            spoilerItem.setTextAndIcon(LocaleController.getString(R.string.Spoiler), R.drawable.msg_spoiler);
            spoilerItem.setMinimumWidth(AndroidUtilities.dp(ITEM_MIN_WIDTH_DP));
            spoilerItem.setOnClickListener(v -> {
                dismissPopup(owner);
                applySpoilerToWholeMessage(editText);
            });
            layout.addView(spoilerItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, ITEM_HEIGHT_DP));
        }

        if (hasText && hasSpoilerSpans(editText.getText())) {
            ActionBarMenuSubItem removeSpoilerItem = new ActionBarMenuSubItem(context, false, true, resourcesProvider);
            removeSpoilerItem.setTextAndIcon(LocaleController.getString(R.string.DisablePhotoSpoiler), R.drawable.msg_spoiler_off);
            removeSpoilerItem.setMinimumWidth(AndroidUtilities.dp(ITEM_MIN_WIDTH_DP));
            removeSpoilerItem.setOnClickListener(v -> {
                dismissPopup(owner);
                removeSpoilerFromWholeMessage(editText);
            });
            layout.addView(removeSpoilerItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, ITEM_HEIGHT_DP));
        }

        return layout;
    }

    private static ActionBarPopupWindow createPopup(ActionBarPopupWindow.ActionBarPopupWindowLayout layout) {
        ActionBarPopupWindow popupWindow = new ActionBarPopupWindow(layout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        popupWindow.setPauseNotifications(true);
        popupWindow.setDismissAnimationDuration(220);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setClippingEnabled(true);
        popupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        popupWindow.getContentView().setFocusableInTouchMode(true);
        return popupWindow;
    }

    private static void dismissPopup(Object owner) {
        ActionBarPopupWindow popupWindow = POPUPS.remove(owner);
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    private static void applySpoilerToWholeMessage(EditTextCaption editText) {
        Editable text = editText.getText();
        if (text == null || text.length() == 0) {
            return;
        }
        int length = text.length();
        editText.setSelection(0, length);
        editText.makeSelectedSpoiler();
        editText.setSelection(length);
    }

    private static void removeSpoilerFromWholeMessage(EditTextCaption editText) {
        Editable text = editText.getText();
        if (text == null || text.length() == 0) {
            return;
        }

        CharacterStyle[] spans = text.getSpans(0, text.length(), CharacterStyle.class);
        for (CharacterStyle span : spans) {
            if (span instanceof TextStyleSpan) {
                TextStyleSpan textStyleSpan = (TextStyleSpan) span;
                TextStyleSpan.TextStyleRun run = textStyleSpan.getTextStyleRun();
                if (run == null || (run.flags & (TextStyleSpan.FLAG_STYLE_SPOILER | TextStyleSpan.FLAG_STYLE_SPOILER_REVEALED)) == 0) {
                    continue;
                }
                int spanStart = text.getSpanStart(span);
                int spanEnd = text.getSpanEnd(span);
                int spanFlags = text.getSpanFlags(span);
                text.removeSpan(span);
                TextStyleSpan.TextStyleRun cleanedRun = new TextStyleSpan.TextStyleRun(run);
                cleanedRun.flags &= ~TextStyleSpan.FLAG_STYLE_SPOILER;
                cleanedRun.flags &= ~TextStyleSpan.FLAG_STYLE_SPOILER_REVEALED;
                if (cleanedRun.flags != 0) {
                    text.setSpan(new TextStyleSpan(cleanedRun), spanStart, spanEnd, spanFlags == 0 ? Spanned.SPAN_EXCLUSIVE_EXCLUSIVE : spanFlags);
                }
            } else if (span instanceof URLSpanReplacement) {
                URLSpanReplacement urlSpan = (URLSpanReplacement) span;
                TextStyleSpan.TextStyleRun run = urlSpan.getTextStyleRun();
                if (run == null || (run.flags & (TextStyleSpan.FLAG_STYLE_SPOILER | TextStyleSpan.FLAG_STYLE_SPOILER_REVEALED)) == 0) {
                    continue;
                }
                int spanStart = text.getSpanStart(span);
                int spanEnd = text.getSpanEnd(span);
                int spanFlags = text.getSpanFlags(span);
                text.removeSpan(span);
                TextStyleSpan.TextStyleRun cleanedRun = new TextStyleSpan.TextStyleRun(run);
                cleanedRun.flags &= ~TextStyleSpan.FLAG_STYLE_SPOILER;
                cleanedRun.flags &= ~TextStyleSpan.FLAG_STYLE_SPOILER_REVEALED;
                text.setSpan(new URLSpanReplacement(urlSpan.getURL(), cleanedRun), spanStart, spanEnd, spanFlags == 0 ? Spanned.SPAN_EXCLUSIVE_EXCLUSIVE : spanFlags);
            }
        }

        editText.setSelection(text.length());
        editText.invalidateEffects();
    }

    private static boolean hasSpoilerSpans(Editable text) {
        if (text == null || text.length() == 0) {
            return false;
        }

        CharacterStyle[] spans = text.getSpans(0, text.length(), CharacterStyle.class);
        for (CharacterStyle span : spans) {
            TextStyleSpan.TextStyleRun run = null;
            if (span instanceof TextStyleSpan) {
                run = ((TextStyleSpan) span).getTextStyleRun();
            } else if (span instanceof URLSpanReplacement) {
                run = ((URLSpanReplacement) span).getTextStyleRun();
            }
            if (run != null && (run.flags & (TextStyleSpan.FLAG_STYLE_SPOILER | TextStyleSpan.FLAG_STYLE_SPOILER_REVEALED)) != 0) {
                return true;
            }
        }
        return false;
    }
}
