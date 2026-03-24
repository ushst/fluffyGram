package org.ushastoe.fluffy.patches;

import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

public final class InlineCallbackDataPatch {

    private static final int TEXT_PREVIEW_LIMIT = 180;
    private static final int HEX_PREVIEW_LIMIT = 96;

    private InlineCallbackDataPatch() {
    }

    public static boolean showMenu(ChatActivity fragment, ChatMessageCell cell, TLRPC.KeyboardButton button) {
        if (fragment == null || cell == null || !(button instanceof TLRPC.TL_keyboardButtonCallback)) {
            return false;
        }

        MessageObject messageObject = cell.getMessageObject();
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }

        byte[] data = button.data != null ? button.data : new byte[0];
        String hexData = Utilities.bytesToHex(data);
        final String hexDataValue = TextUtils.isEmpty(hexData)
                ? LocaleController.getString(R.string.FluffyInlineCallbackDataEmpty)
                : hexData;
        String utf8Text = decodeUtf8Text(data);

        View anchorView = createAnchorView(fragment, cell, button);
        if (anchorView == null) {
            anchorView = cell;
        }

        final View dismissAnchorView = anchorView;
        ItemOptions options = ItemOptions.makeOptions(fragment, anchorView)
                .setOnDismiss(() -> removeAnchor(dismissAnchorView));
        if (!TextUtils.isEmpty(button.text)) {
            options.add(
                    LocaleController.getString(R.string.FluffyInlineCallbackButtonText),
                    trimPreview(button.text, TEXT_PREVIEW_LIMIT),
                    null
            ).makeMultiline(true);
        }
        options.add(
                LocaleController.getString(R.string.FluffyInlineCallbackDataSize),
                String.format(Locale.US, LocaleController.getString(R.string.FluffyInlineCallbackDataSizeValue), data.length),
                null
        );
        options.add(
                LocaleController.getString(R.string.FluffyInlineCallbackDataText),
                trimPreview(TextUtils.isEmpty(utf8Text) ? LocaleController.getString(R.string.FluffyInlineCallbackDataBinary) : utf8Text, TEXT_PREVIEW_LIMIT),
                null
        ).makeMultiline(true);
        options.add(
                LocaleController.getString(R.string.FluffyInlineCallbackDataHex),
                trimPreview(hexDataValue, HEX_PREVIEW_LIMIT),
                null
        ).makeMultiline(true);
        options.addGap();
        if (!TextUtils.isEmpty(utf8Text)) {
            options.add(R.drawable.msg_copy, LocaleController.getString(R.string.FluffyInlineCallbackCopyText), () -> copyAndNotify(fragment, utf8Text));
        }
        options.add(R.drawable.msg_copy, LocaleController.getString(R.string.FluffyInlineCallbackCopyHex), () -> copyAndNotify(fragment, hexDataValue));
        options.show();
        return true;
    }

    private static void copyAndNotify(ChatActivity fragment, String value) {
        if (fragment == null || TextUtils.isEmpty(value) || !AndroidUtilities.addToClipboard(value)) {
            return;
        }
        BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
    }

    private static View createAnchorView(ChatActivity fragment, ChatMessageCell cell, TLRPC.KeyboardButton button) {
        ViewGroup container = fragment.getLayoutContainer();
        if (container == null) {
            return null;
        }

        Rect buttonRect = new Rect();
        if (!cell.getBotButtonBounds(button, buttonRect)) {
            return null;
        }

        int[] cellLocation = new int[2];
        int[] containerLocation = new int[2];
        cell.getLocationInWindow(cellLocation);
        container.getLocationInWindow(containerLocation);

        int left = cellLocation[0] - containerLocation[0] + buttonRect.left;
        int top = cellLocation[1] - containerLocation[1] + buttonRect.top;

        View anchor = new View(container.getContext());
        anchor.setAlpha(0f);
        anchor.setClickable(false);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(Math.max(1, buttonRect.width()), Math.max(1, buttonRect.height()));
        container.addView(anchor, params);
        anchor.setX(left);
        anchor.setY(top);
        return anchor;
    }

    private static void removeAnchor(View anchorView) {
        if (anchorView == null || anchorView instanceof ChatMessageCell) {
            return;
        }
        ViewParent parent = anchorView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(anchorView);
        }
    }

    private static String decodeUtf8Text(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        String decoded = new String(data, StandardCharsets.UTF_8);
        if (!Arrays.equals(decoded.getBytes(StandardCharsets.UTF_8), data)) {
            return null;
        }
        if (!isPrintable(decoded)) {
            return null;
        }
        return decoded;
    }

    private static boolean isPrintable(String value) {
        if (TextUtils.isEmpty(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t') {
                return false;
            }
        }
        return true;
    }

    private static String trimPreview(String value, int limit) {
        if (TextUtils.isEmpty(value) || value.length() <= limit) {
            return value;
        }
        return value.substring(0, Math.max(0, limit - 3)) + "...";
    }
}
