package org.ushastoe.fluffy.helpers;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.ColoredImageSpan;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import org.telegram.ui.ActionBar.Theme;

public class MessageHelper {
    public static String getPathToMessage(MessageObject messageObject) {
        String path = messageObject.messageOwner.attachPath;
        if (!TextUtils.isEmpty(path)) {
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                return null;
            }
        }
        return path;
    }

    public static String getTextOrBase64(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return Base64.encodeToString(data, Base64.NO_PADDING | Base64.NO_WRAP);
        }
    }

    private static final SpannableStringBuilder[] spannedStrings = new SpannableStringBuilder[5];

    public static CharSequence createNewString(MessageObject messageObject) {
        var spannableStringBuilder = new SpannableStringBuilder();

        if (messageObject.messageOwner.silent) {
            if (spannedStrings[2] == null) {
                spannedStrings[2] = new SpannableStringBuilder("s");
            }
            spannableStringBuilder
                    .append(spannedStrings[2])
                    .append(' ');

        }

        if (messageObject.messageOwner.from_scheduled) {
            if (spannedStrings[2] == null) {
                spannedStrings[2] = new SpannableStringBuilder("t");
            }
            spannableStringBuilder
                    .append(spannedStrings[2])
                    .append(' ');

        }

        if (messageObject.isEdited()) {
            if (spannedStrings[1] == null) {
                spannedStrings[1] = new SpannableStringBuilder("\u200B");
                spannedStrings[1].setSpan(new ColoredImageSpan(Theme.chat_editDrawable), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            spannableStringBuilder
                    .append(spannedStrings[1])
                    .append(' ');
        }

        spannableStringBuilder.append(LocaleController.getInstance().getFormatterDay().format((long) (messageObject.messageOwner.date) * 1000));
        return spannableStringBuilder;
    }
}
