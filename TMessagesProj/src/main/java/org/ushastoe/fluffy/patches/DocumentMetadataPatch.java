package org.ushastoe.fluffy.patches;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.ushastoe.fluffy.utils.DocumentMetadataReader;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public final class DocumentMetadataPatch {

    private DocumentMetadataPatch() {
    }

    public static boolean canShowForMessage(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return false;
        }
        TLRPC.Document document = messageObject.getDocument();
        if (messageObject.isVoiceOnce() || messageObject.isRoundOnce() || messageObject.isAnyKindOfSticker()) {
            return false;
        }
        if (document == null && !messageObject.isPhoto()) {
            return false;
        }
        return DocumentMetadataReader.isSupportedExtension(getMessageExtension(messageObject));
    }

    public static void loadMetadata(MessageObject messageObject, MetadataCallback callback) {
        if (callback == null) {
            return;
        }
        if (messageObject == null || messageObject.messageOwner == null) {
            callback.onLoaded(MetadataState.error("missing_message", null));
            return;
        }
        final TLRPC.Document document = messageObject.getDocument();
        final String fileName = getDocumentFileName(messageObject);
        final File file = resolveLocalFile(messageObject);
        if (file == null || !file.exists()) {
            callback.onLoaded(MetadataState.error("file_missing", buildBasicRows(fileName, null, getMessageExtension(messageObject))));
            return;
        }

        Utilities.globalQueue.postRunnable(() -> {
            MetadataState state;
            try {
                DocumentMetadataReader.MetadataResult result = DocumentMetadataReader.read(file, fileName);
                ArrayList<MetadataRow> rows = buildBasicRows(fileName, file.getAbsolutePath(), result.extension);
                for (Map.Entry<String, String> entry : result.entries.entrySet()) {
                    rows.add(new MetadataRow(entry.getKey(), entry.getValue()));
                }
                state = rows.size() > 3 ? MetadataState.ready(rows) : MetadataState.empty(rows);
            } catch (Throwable throwable) {
                state = MetadataState.error("parse_failed", buildBasicRows(fileName, file.getAbsolutePath(), getMessageExtension(messageObject)));
            }
            final MetadataState resolvedState = state;
            AndroidUtilities.runOnUIThread(() -> callback.onLoaded(resolvedState));
        });
    }

    private static ArrayList<MetadataRow> buildBasicRows(String fileName, String filePath, String extension) {
        ArrayList<MetadataRow> rows = new ArrayList<>();
        if (!TextUtils.isEmpty(fileName)) {
            rows.add(new MetadataRow("file_name", fileName));
        }
        if (!TextUtils.isEmpty(extension)) {
            rows.add(new MetadataRow("format", extension.toUpperCase(Locale.US)));
        }
        if (!TextUtils.isEmpty(filePath)) {
            rows.add(new MetadataRow("file_path", filePath));
        }
        return rows;
    }

    public static File resolveLocalFile(MessageObject object) {
        String attachPath = object.messageOwner.attachPath;
        if (!TextUtils.isEmpty(attachPath)) {
            File file = new File(attachPath);
            if (file.exists()) {
                return file;
            }
        }

        File fromMessage = FileLoader.getInstance(object.currentAccount).getPathToMessage(object.messageOwner);
        if (fromMessage != null && fromMessage.exists()) {
            return fromMessage;
        }

        TLRPC.Document document = object.getDocument();
        if (document != null) {
            File fromAttach = FileLoader.getInstance(object.currentAccount).getPathToAttach(document, true);
            if (fromAttach != null && fromAttach.exists()) {
                return fromAttach;
            }
        }
        return null;
    }

    public static String getDocumentFileName(MessageObject messageObject) {
        if (messageObject == null) {
            return null;
        }
        TLRPC.Document document = messageObject.getDocument();
        if (document != null) {
            String fileName = FileLoader.getDocumentFileName(document);
            if (!TextUtils.isEmpty(fileName)) {
                return fileName;
            }
        }
        String messageFileName = FileLoader.getMessageFileName(messageObject.messageOwner);
        return !TextUtils.isEmpty(messageFileName) ? messageFileName : null;
    }

    private static String getMessageExtension(MessageObject messageObject) {
        if (messageObject == null) {
            return "";
        }
        TLRPC.Document document = messageObject.getDocument();
        String extension = getFileExtension(getDocumentFileName(messageObject));
        if (!TextUtils.isEmpty(extension)) {
            return extension;
        }
        if (document != null && !TextUtils.isEmpty(document.mime_type)) {
            return getExtensionForMimeType(document.mime_type);
        }
        if (messageObject.isPhoto()) {
            return "jpg";
        }
        return "";
    }

    private static String getFileExtension(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.US);
    }

    private static String getExtensionForMimeType(String mimeType) {
        switch (mimeType.toLowerCase(Locale.US)) {
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            case "image/heic":
                return "heic";
            case "image/heif":
                return "heif";
            case "image/tiff":
                return "tiff";
            default:
                return "";
        }
    }

    public interface MetadataCallback {
        void onLoaded(MetadataState state);
    }

    public static final class MetadataState {
        public final ArrayList<MetadataRow> rows;
        public final String status;

        private MetadataState(ArrayList<MetadataRow> rows, String status) {
            this.rows = rows == null ? new ArrayList<>() : rows;
            this.status = status;
        }

        public static MetadataState ready(ArrayList<MetadataRow> rows) {
            return new MetadataState(rows, "ready");
        }

        public static MetadataState empty(ArrayList<MetadataRow> rows) {
            return new MetadataState(rows, "empty");
        }

        public static MetadataState error(String status, ArrayList<MetadataRow> rows) {
            return new MetadataState(rows, status);
        }
    }

    public static final class MetadataRow {
        public final String key;
        public final String value;

        public MetadataRow(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
