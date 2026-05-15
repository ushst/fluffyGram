package org.ushastoe.fluffy.utils;

import android.graphics.BitmapFactory;
import android.text.TextUtils;

import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class DocumentMetadataReader {

    private static final long PDF_FULL_READ_LIMIT = 8L * 1024L * 1024L;
    private static final int PDF_HEAD_READ_LIMIT = 256 * 1024;
    private static final int PDF_TAIL_READ_LIMIT = 1024 * 1024;
    private static final int XML_ENTRY_READ_LIMIT = 1024 * 1024;
    private static final int METADATA_VALUE_LIMIT = 4096;

    private DocumentMetadataReader() {
    }

    public static MetadataResult read(File file, String fileName) throws IOException {
        String extension = getExtension(!TextUtils.isEmpty(fileName) ? fileName : file.getName());
        MetadataResult result = new MetadataResult(extension);
        if (isOpenXmlExtension(extension)) {
            result.entries.putAll(readOpenXml(file));
        } else if ("pdf".equals(extension)) {
            result.entries.putAll(readPdf(file));
        } else if (isImageExtension(extension)) {
            result.entries.putAll(readImage(file));
        }
        return result;
    }

    public static boolean isSupportedExtension(String extension) {
        if (TextUtils.isEmpty(extension)) {
            return false;
        }
        return "pdf".equals(extension) || isOpenXmlExtension(extension) || isImageExtension(extension);
    }

    private static boolean isOpenXmlExtension(String extension) {
        if (TextUtils.isEmpty(extension)) {
            return false;
        }
        switch (extension) {
            case "docx":
            case "docm":
            case "dotx":
            case "dotm":
            case "xlsx":
            case "xlsm":
            case "xltx":
            case "xltm":
            case "pptx":
            case "pptm":
            case "ppsx":
            case "ppsm":
            case "potx":
            case "potm":
                return true;
            default:
                return false;
        }
    }

    private static boolean isImageExtension(String extension) {
        if (TextUtils.isEmpty(extension)) {
            return false;
        }
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "webp":
            case "heic":
            case "heif":
            case "tif":
            case "tiff":
                return true;
            default:
                return false;
        }
    }

    private static LinkedHashMap<String, String> readOpenXml(File file) throws IOException {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        try (ZipFile zipFile = new ZipFile(file)) {
            String coreXml = readZipEntry(zipFile, "docProps/core.xml");
            String appXml = readZipEntry(zipFile, "docProps/app.xml");

            putIfNotEmpty(entries, "title", extractXmlValue(coreXml, "title"));
            putIfNotEmpty(entries, "subject", extractXmlValue(coreXml, "subject"));
            putIfNotEmpty(entries, "author", extractXmlValue(coreXml, "creator"));
            putIfNotEmpty(entries, "keywords", extractXmlValue(coreXml, "keywords"));
            putIfNotEmpty(entries, "description", extractXmlValue(coreXml, "description"));
            putIfNotEmpty(entries, "last_modified_by", extractXmlValue(coreXml, "lastModifiedBy"));
            putIfNotEmpty(entries, "created", normalizeDateValue(extractXmlValue(coreXml, "created")));
            putIfNotEmpty(entries, "modified", normalizeDateValue(extractXmlValue(coreXml, "modified")));

            putIfNotEmpty(entries, "application", extractXmlValue(appXml, "Application"));
            putIfNotEmpty(entries, "application_version", extractXmlValue(appXml, "AppVersion"));
            putIfNotEmpty(entries, "company", extractXmlValue(appXml, "Company"));
            putIfNotEmpty(entries, "manager", extractXmlValue(appXml, "Manager"));
            putIfNotEmpty(entries, "pages", extractXmlValue(appXml, "Pages"));
            putIfNotEmpty(entries, "words", extractXmlValue(appXml, "Words"));
            putIfNotEmpty(entries, "characters", extractXmlValue(appXml, "Characters"));
            putIfNotEmpty(entries, "slides", extractXmlValue(appXml, "Slides"));
            putIfNotEmpty(entries, "notes", extractXmlValue(appXml, "Notes"));
            putIfNotEmpty(entries, "total_time", extractXmlValue(appXml, "TotalTime"));
            putIfNotEmpty(entries, "presentation_format", extractXmlValue(appXml, "PresentationFormat"));
        }
        return entries;
    }

    private static LinkedHashMap<String, String> readPdf(File file) throws IOException {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        String pdfText = readPdfSearchText(file);

        putIfNotEmpty(entries, "title", firstNonEmpty(extractPdfInfoValue(pdfText, "Title"), extractPdfXmpValue(pdfText, "title")));
        putIfNotEmpty(entries, "author", firstNonEmpty(extractPdfInfoValue(pdfText, "Author"), extractPdfXmpValue(pdfText, "creator")));
        putIfNotEmpty(entries, "subject", firstNonEmpty(extractPdfInfoValue(pdfText, "Subject"), extractPdfXmpValue(pdfText, "description")));
        putIfNotEmpty(entries, "keywords", extractPdfInfoValue(pdfText, "Keywords"));
        putIfNotEmpty(entries, "creator", firstNonEmpty(extractPdfInfoValue(pdfText, "Creator"), extractPdfXmpValue(pdfText, "CreatorTool")));
        putIfNotEmpty(entries, "producer", firstNonEmpty(extractPdfInfoValue(pdfText, "Producer"), extractPdfXmpValue(pdfText, "Producer")));
        putIfNotEmpty(entries, "created", normalizeDateValue(firstNonEmpty(extractPdfInfoValue(pdfText, "CreationDate"), extractPdfXmpValue(pdfText, "CreateDate"))));
        putIfNotEmpty(entries, "modified", normalizeDateValue(firstNonEmpty(extractPdfInfoValue(pdfText, "ModDate"), extractPdfXmpValue(pdfText, "ModifyDate"))));

        return entries;
    }

    private static LinkedHashMap<String, String> readImage(File file) throws IOException {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        putIfPositive(entries, "image_width", options.outWidth);
        putIfPositive(entries, "image_height", options.outHeight);

        ExifInterface exifInterface;
        try {
            exifInterface = new ExifInterface(file.getAbsolutePath());
        } catch (Throwable ignore) {
            return entries;
        }
        putIfNotEmpty(entries, "make", getExifAttribute(exifInterface, ExifInterface.TAG_MAKE));
        putIfNotEmpty(entries, "model", getExifAttribute(exifInterface, ExifInterface.TAG_MODEL));
        putIfNotEmpty(entries, "software", getExifAttribute(exifInterface, ExifInterface.TAG_SOFTWARE));
        putIfNotEmpty(entries, "orientation", normalizeOrientation(getExifAttributeInt(exifInterface, ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)));
        putIfNotEmpty(entries, "created", normalizeExifDate(getExifAttribute(exifInterface, ExifInterface.TAG_DATETIME)));
        putIfNotEmpty(entries, "date_time_original", normalizeExifDate(getExifAttribute(exifInterface, ExifInterface.TAG_DATETIME_ORIGINAL)));
        putIfNotEmpty(entries, "date_time_digitized", normalizeExifDate(getExifAttribute(exifInterface, ExifInterface.TAG_DATETIME_DIGITIZED)));
        putIfNotEmpty(entries, "artist", getExifAttribute(exifInterface, ExifInterface.TAG_ARTIST));
        putIfNotEmpty(entries, "copyright", getExifAttribute(exifInterface, ExifInterface.TAG_COPYRIGHT));
        putIfNotEmpty(entries, "description", getExifAttribute(exifInterface, ExifInterface.TAG_IMAGE_DESCRIPTION));
        putIfNotEmpty(entries, "exposure_time", getExifAttribute(exifInterface, ExifInterface.TAG_EXPOSURE_TIME));
        putIfNotEmpty(entries, "f_number", getExifAttribute(exifInterface, ExifInterface.TAG_F_NUMBER));
        putIfNotEmpty(entries, "iso", firstNonEmpty(getExifAttribute(exifInterface, ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY), getExifAttribute(exifInterface, ExifInterface.TAG_ISO_SPEED_RATINGS)));
        putIfNotEmpty(entries, "focal_length", getExifAttribute(exifInterface, ExifInterface.TAG_FOCAL_LENGTH));
        putIfNotEmpty(entries, "lens_model", getExifAttribute(exifInterface, ExifInterface.TAG_LENS_MODEL));
        putIfNotEmpty(entries, "flash", getExifAttribute(exifInterface, ExifInterface.TAG_FLASH));
        putIfNotEmpty(entries, "white_balance", getExifAttribute(exifInterface, ExifInterface.TAG_WHITE_BALANCE));
        putIfNotEmpty(entries, "gps", getGpsValue(exifInterface));
        putIfNotEmpty(entries, "gps_altitude", getGpsAltitudeValue(exifInterface));
        return entries;
    }

    private static String readZipEntry(ZipFile zipFile, String entryName) throws IOException {
        ZipEntry entry = zipFile.getEntry(entryName);
        if (entry == null) {
            return null;
        }
        if (entry.getSize() > XML_ENTRY_READ_LIMIT) {
            return null;
        }
        try (InputStream inputStream = zipFile.getInputStream(entry);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                outputStream.write(buffer, 0, read);
                if (outputStream.size() > XML_ENTRY_READ_LIMIT) {
                    return null;
                }
            }
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String extractXmlValue(String xml, String tagName) {
        if (TextUtils.isEmpty(xml) || TextUtils.isEmpty(tagName)) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?is)<(?:\\w+:)?" + Pattern.quote(tagName) + "(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?" + Pattern.quote(tagName) + ">");
        Matcher matcher = pattern.matcher(xml);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        value = value.replaceAll("(?is)<[^>]+>", " ");
        value = decodeXmlEntities(value);
        return cleanValue(value);
    }

    private static String readPdfSearchText(File file) throws IOException {
        long length = file.length();
        if (length <= PDF_FULL_READ_LIMIT) {
            try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
                byte[] bytes = new byte[(int) length];
                randomAccessFile.readFully(bytes);
                return new String(bytes, StandardCharsets.ISO_8859_1);
            }
        }

        int headLength = (int) Math.min(PDF_HEAD_READ_LIMIT, length);
        int tailLength = (int) Math.min(PDF_TAIL_READ_LIMIT, Math.max(0, length - headLength));
        byte[] head = new byte[headLength];
        byte[] tail = new byte[tailLength];
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            randomAccessFile.readFully(head);
            if (tailLength > 0) {
                randomAccessFile.seek(length - tailLength);
                randomAccessFile.readFully(tail);
            }
        }
        return new String(head, StandardCharsets.ISO_8859_1) + "\n" + new String(tail, StandardCharsets.ISO_8859_1);
    }

    private static String extractPdfInfoValue(String text, String key) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        String token = "/" + key;
        int startIndex = 0;
        while (true) {
            int keyIndex = text.indexOf(token, startIndex);
            if (keyIndex < 0) {
                return null;
            }
            int valueIndex = keyIndex + token.length();
            while (valueIndex < text.length() && Character.isWhitespace(text.charAt(valueIndex))) {
                valueIndex++;
            }
            if (valueIndex >= text.length()) {
                return null;
            }
            char marker = text.charAt(valueIndex);
            if (marker == '(') {
                return cleanValue(decodePdfLiteralString(text, valueIndex));
            } else if (marker == '<' && valueIndex + 1 < text.length() && text.charAt(valueIndex + 1) != '<') {
                return cleanValue(decodePdfHexString(text, valueIndex));
            }
            startIndex = valueIndex;
        }
    }

    private static String decodePdfLiteralString(String text, int startIndex) {
        StringBuilder builder = new StringBuilder();
        int depth = 0;
        boolean escaped = false;
        for (int i = startIndex; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (i == startIndex) {
                depth = 1;
                continue;
            }
            if (escaped) {
                switch (ch) {
                    case 'n':
                        builder.append('\n');
                        break;
                    case 'r':
                        builder.append('\r');
                        break;
                    case 't':
                        builder.append('\t');
                        break;
                    case 'b':
                        builder.append('\b');
                        break;
                    case 'f':
                        builder.append('\f');
                        break;
                    default:
                        builder.append(ch);
                        break;
                }
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '(') {
                depth++;
                builder.append(ch);
                continue;
            }
            if (ch == ')') {
                depth--;
                if (depth == 0) {
                    break;
                }
                builder.append(ch);
                continue;
            }
            builder.append(ch);
        }
        return builder.toString();
    }

    private static String decodePdfHexString(String text, int startIndex) {
        int endIndex = text.indexOf('>', startIndex + 1);
        if (endIndex < 0) {
            return null;
        }
        String hex = text.substring(startIndex + 1, endIndex).replaceAll("\\s+", "");
        if (hex.length() % 2 != 0) {
            hex += "0";
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                return null;
            }
            bytes[i] = (byte) ((high << 4) + low);
        }
        if (bytes.length >= 2) {
            int bom = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);
            if (bom == 0xFEFF) {
                return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
            }
        }
        return new String(bytes, StandardCharsets.ISO_8859_1);
    }

    private static String extractPdfXmpValue(String text, String tagName) {
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        Pattern pattern = Pattern.compile("(?is)<(?:\\w+:)?" + Pattern.quote(tagName) + "(?:\\s[^>]*)?>(.*?)</(?:\\w+:)?" + Pattern.quote(tagName) + ">");
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).replaceAll("(?is)<[^>]+>", " ");
        return cleanValue(decodeXmlEntities(value));
    }

    private static void putIfNotEmpty(LinkedHashMap<String, String> entries, String key, String value) {
        String cleaned = cleanValue(value);
        if (!TextUtils.isEmpty(cleaned)) {
            entries.put(key, cleaned);
        }
    }

    private static void putIfPositive(LinkedHashMap<String, String> entries, String key, int value) {
        if (value > 0) {
            entries.put(key, String.valueOf(value));
        }
    }

    private static String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        value = value.replace('\u0000', ' ').replaceAll("[\\t\\x0B\\f\\r]+", " ");
        value = value.replaceAll("\\s*\\n\\s*", "\n").trim();
        if (value.length() > METADATA_VALUE_LIMIT) {
            value = value.substring(0, METADATA_VALUE_LIMIT).trim() + "...";
        }
        return value.isEmpty() ? null : value;
    }

    private static String decodeXmlEntities(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }

    private static String normalizeDateValue(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        if (value.startsWith("D:")) {
            return normalizePdfDate(value);
        }
        String normalized = value.trim().replace('T', ' ');
        normalized = normalized.replaceAll("Z$", " UTC");
        return cleanValue(normalized);
    }

    private static String normalizePdfDate(String value) {
        String digits = value.substring(2).replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return value;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(digits, 0, Math.min(4, digits.length()));
        if (digits.length() >= 6) {
            builder.append('-').append(digits, 4, 6);
        }
        if (digits.length() >= 8) {
            builder.append('-').append(digits, 6, 8);
        }
        if (digits.length() >= 10) {
            builder.append(' ').append(digits, 8, 10);
        }
        if (digits.length() >= 12) {
            builder.append(':').append(digits, 10, 12);
        }
        if (digits.length() >= 14) {
            builder.append(':').append(digits, 12, 14);
        }
        return builder.toString();
    }

    private static String normalizeExifDate(String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() >= 19 && normalized.charAt(4) == ':' && normalized.charAt(7) == ':') {
            normalized = normalized.substring(0, 4) + "-" + normalized.substring(5, 7) + "-" + normalized.substring(8);
        }
        return cleanValue(normalized);
    }

    private static String normalizeOrientation(int orientation) {
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return "Normal";
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return "Flip horizontal";
            case ExifInterface.ORIENTATION_ROTATE_180:
                return "Rotate 180";
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return "Flip vertical";
            case ExifInterface.ORIENTATION_TRANSPOSE:
                return "Transpose";
            case ExifInterface.ORIENTATION_ROTATE_90:
                return "Rotate 90";
            case ExifInterface.ORIENTATION_TRANSVERSE:
                return "Transverse";
            case ExifInterface.ORIENTATION_ROTATE_270:
                return "Rotate 270";
            default:
                return null;
        }
    }

    private static String getExifAttribute(ExifInterface exifInterface, String tag) {
        try {
            return exifInterface.getAttribute(tag);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static int getExifAttributeInt(ExifInterface exifInterface, String tag, int defaultValue) {
        try {
            return exifInterface.getAttributeInt(tag, defaultValue);
        } catch (Throwable ignore) {
            return defaultValue;
        }
    }

    private static String getGpsValue(ExifInterface exifInterface) {
        float[] latLong = new float[2];
        try {
            if (!exifInterface.getLatLong(latLong)) {
                return null;
            }
        } catch (Throwable ignore) {
            return null;
        }
        return String.format(Locale.US, "%.6f, %.6f", latLong[0], latLong[1]);
    }

    private static String getGpsAltitudeValue(ExifInterface exifInterface) {
        double altitude;
        try {
            altitude = exifInterface.getAltitude(Double.NaN);
        } catch (Throwable ignore) {
            return null;
        }
        if (Double.isNaN(altitude)) {
            return null;
        }
        return String.format(Locale.US, "%.2f m", altitude);
    }

    private static String getExtension(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return "";
        }
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(index + 1).toLowerCase(Locale.US);
    }

    private static String firstNonEmpty(String first, String second) {
        return TextUtils.isEmpty(first) ? second : first;
    }

    public static final class MetadataResult {
        public final String extension;
        public final LinkedHashMap<String, String> entries = new LinkedHashMap<>();

        public MetadataResult(String extension) {
            this.extension = extension;
        }
    }
}
