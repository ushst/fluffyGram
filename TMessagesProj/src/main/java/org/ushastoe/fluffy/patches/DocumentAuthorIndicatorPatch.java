package org.ushastoe.fluffy.patches;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.Components.ColoredImageSpan;
import org.ushastoe.fluffy.utils.DocumentMetadataReader;
import org.ushastoe.fluffy.utils.MessageTimeIconSpanFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DocumentAuthorIndicatorPatch {

    private static final String AUTHOR_ICON_PLACEHOLDER = "a";
    private static final ConcurrentHashMap<String, String> authorCache = new ConcurrentHashMap<>();
    private static final HashSet<String> loadingKeys = new HashSet<>();

    private DocumentAuthorIndicatorPatch() {
    }

    public static CharSequence buildTimeLabel(MessageObject messageObject, boolean edited) {
        if (messageObject == null
                || messageObject.messageOwner == null
                || !MessageActionsPatch.isDocumentMetadataEnabled()
                || !DocumentMetadataPatch.canShowForMessage(messageObject)) {
            return "";
        }
        String cacheKey = buildCacheKey(messageObject);
        if (TextUtils.isEmpty(cacheKey)) {
            return "";
        }
        String cachedAuthor = authorCache.get(cacheKey);
        if (cachedAuthor == null) {
            scheduleAuthorLoad(messageObject, cacheKey);
            return "";
        }
        if (cachedAuthor.isEmpty()) {
            return "";
        }

        String time = LocaleController.getInstance().getFormatterDay().format((long) messageObject.messageOwner.date * 1000);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (messageObject.messageOwner.silent) {
            MessageTimeLabelPatch.appendSilentMarker(builder);
        }
        if (edited) {
            MessageTimeLabelPatch.appendEditedMarker(builder);
        }
        appendAuthorMarker(builder);
        if (builder.length() > 0) {
            builder.append(" ");
        }
        builder.append(time);
        return builder;
    }

    private static String buildCacheKey(MessageObject messageObject) {
        File file = DocumentMetadataPatch.resolveLocalFile(messageObject);
        if (file == null || !file.exists()) {
            return null;
        }
        return file.getAbsolutePath() + "|" + file.length() + "|" + file.lastModified();
    }

    private static void scheduleAuthorLoad(MessageObject messageObject, String cacheKey) {
        synchronized (loadingKeys) {
            if (!loadingKeys.add(cacheKey)) {
                return;
            }
        }
        final MessageObject target = messageObject;
        Utilities.globalQueue.postRunnable(() -> {
            String author = "";
            try {
                File file = DocumentMetadataPatch.resolveLocalFile(target);
                String fileName = DocumentMetadataPatch.getDocumentFileName(target);
                if (file != null && file.exists()) {
                    DocumentMetadataReader.MetadataResult result = DocumentMetadataReader.read(file, fileName);
                    author = findAuthor(result);
                    if (author == null) {
                        author = "";
                    }
                }
            } catch (Throwable ignore) {
                author = "";
            }
            authorCache.put(cacheKey, author);
            synchronized (loadingKeys) {
                loadingKeys.remove(cacheKey);
            }
            AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(target.currentAccount)
                    .postNotificationName(NotificationCenter.updateInterfaces, MessagesController.UPDATE_MASK_MESSAGE_TEXT));
        });
    }

    private static String findAuthor(DocumentMetadataReader.MetadataResult result) {
        if (result == null || result.entries.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, String> entry : result.entries.entrySet()) {
            if ("author".equals(entry.getKey()) && !TextUtils.isEmpty(entry.getValue())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static void appendAuthorMarker(SpannableStringBuilder builder) {
        int mode = PremiumSettingsPatch.getDocumentAuthorMarkerMode();
        if (builder.length() > 0) {
            builder.append(" ");
        }
        if (mode == PremiumSettingsPatch.DOCUMENT_AUTHOR_MARKER_MODE_ICON) {
            int start = builder.length();
            builder.append(AUTHOR_ICON_PLACEHOLDER);
            ColoredImageSpan span = MessageTimeIconSpanFactory.create(R.drawable.msg_contact);
            builder.setSpan(span, start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return;
        }
        if (mode == PremiumSettingsPatch.DOCUMENT_AUTHOR_MARKER_MODE_SHORT_TEXT) {
            builder.append(LocaleController.getString(R.string.FluffyDocumentAuthorMarkerShortText));
            return;
        }
        builder.append(LocaleController.getString(R.string.FluffyDocumentAuthorMarkerText));
    }
}
