package org.ushastoe.fluffy.helpers;

import static org.telegram.messenger.LocaleController.getString;

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

    public static String decodeBase64(String encodedString) throws IllegalArgumentException {
        byte[] decodedBytes = Base64.decode(encodedString, Base64.DEFAULT);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }


    private static final SpannableStringBuilder[] spannedStrings = new SpannableStringBuilder[5];

    public static CharSequence createNewString(MessageObject messageObject) {
        var spannableStringBuilder = new SpannableStringBuilder();

        Log.d("messageObject", "nStr: " + messageObject.messageOwner.dialog_id);

        if (messageObject.messageOwner.silent) {
            if (spannedStrings[0] == null) {
                spannedStrings[0] = new SpannableStringBuilder("s");
            }
            spannableStringBuilder
                    .append(spannedStrings[0])
                    .append(' ');
        }

        if (messageObject.messageOwner.from_scheduled) {
            if (spannedStrings[1] == null) {
                spannedStrings[1] = new SpannableStringBuilder("t");
            }
            spannableStringBuilder
                    .append(spannedStrings[1])
                    .append(' ');
        }

        if (messageObject.messageOwner.isDeleted() ) {
            if (spannedStrings[2] == null) {
                spannedStrings[2] = new SpannableStringBuilder("\u200B");
                ColoredImageSpan icon = new ColoredImageSpan(Theme.chat_deleteDrawable);
                icon.setSize(40);
                spannedStrings[2].setSpan(icon, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            spannableStringBuilder
                    .append(spannedStrings[2])
                    .append(' ');
        }

        if (messageObject.isEdited() && !messageObject.messageOwner.isDeleted()) {
            if (spannedStrings[3] == null) {
                spannedStrings[3] = new SpannableStringBuilder("\u200B");
                ColoredImageSpan edit_icon = new ColoredImageSpan(Theme.chat_editDrawable);
                edit_icon.setSize(30);
                spannedStrings[3].setSpan(edit_icon, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            spannableStringBuilder
                    .append(spannedStrings[3])
                    .append(' ');
        }


        spannableStringBuilder.append(LocaleController.getInstance().getFormatterDay().format((long) (messageObject.messageOwner.date) * 1000));
        return spannableStringBuilder;
    }
}