package org.ushastoe.fluffy.helpers;

import static org.telegram.messenger.LocaleController.getString;

import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.collection.LongSparseArray;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.ColoredImageSpan;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.telegram.ui.ActionBar.Theme;

public class MessageHelper {
    public static String getFormattedTime(long unixTimeSeconds) {
        if (unixTimeSeconds == 0) {
            return getString(R.string.ShortNow);
        }
        long unixTimeMillis = unixTimeSeconds * 1000;
        Date date = new Date(unixTimeMillis);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        formatter.setTimeZone(TimeZone.getDefault());
        return formatter.format(date);
    }

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

    public static  MessageObject.GroupedMessages getValidGroupedMessage(MessageObject message) {
        LongSparseArray<MessageObject.GroupedMessages> groupedMessagesMap = new LongSparseArray<>();

        MessageObject.GroupedMessages groupedMessages = null;
        if (message.getGroupId() != 0) {
            groupedMessages = groupedMessagesMap.get(message.getGroupId());
            if (groupedMessages != null && (groupedMessages.messages.size() <= 1 || groupedMessages.getPosition(message) == null)) {
                groupedMessages = null;
            }
        }
        return groupedMessages;
    }

    public static String getTextOrBase64(byte[] data) {
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return Base64.encodeToString(data, Base64.NO_PADDING | Base64.NO_WRAP);
        }
    }
    public static String encodeBase64(String text) {
        if (text == null) {
            return null;
        }
        return Base64.encodeToString(text.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
    }
    public static String decodeBase64(String encodedString) throws IllegalArgumentException {
        byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    public static class SpannableResult {
        public final CharSequence text;
        public final int width;

        public SpannableResult(CharSequence text, int width) {
            this.text = text;
            this.width = width;
        }
    }

    public static SpannableResult createNewString(MessageObject messageObject) {
        var spannableStringBuilder = new SpannableStringBuilder();
        final SpannableStringBuilder[] spannedStrings = new SpannableStringBuilder[4];
        final Drawable[] icons = {
                messageObject.messageOwner.isDeleted() ? Theme.chat_deleteDrawable : null,
                messageObject.isEdited() ? Theme.chat_editDrawable : null,
                messageObject.messageOwner.silent ? Theme.chat_silentDrawable : null,
                messageObject.messageOwner.from_scheduled ? Theme.chat_sheduleDrawable : null
        };

        int totalWidth = 0;

        for (int i = 0; i < icons.length; i++) {
            if (icons[i] != null) {
                spannedStrings[i] = new SpannableStringBuilder("\u200B");
                ColoredImageSpan iconSpan = new ColoredImageSpan(icons[i]);
                iconSpan.setSize(30);
                spannedStrings[i].setSpan(iconSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                totalWidth += 30;
            }
        }

        for (SpannableStringBuilder spannedString : spannedStrings) {
            if (spannedString != null) {
                spannableStringBuilder.append(spannedString).append(" ");
            }
        }

        spannableStringBuilder.append(LocaleController.getInstance().getFormatterDay().format((long) (messageObject.messageOwner.date) * 1000));
        Log.d("fluffy", String.valueOf(totalWidth));
        return new SpannableResult(spannableStringBuilder, totalWidth);
    }

}