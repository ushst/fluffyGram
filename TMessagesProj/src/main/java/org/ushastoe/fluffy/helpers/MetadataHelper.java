package org.ushastoe.fluffy.helpers;

import android.content.Context;
import android.text.TextUtils;
import android.util.Xml;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ChatActivity;
import org.ushastoe.fluffy.activities.elements.FluffyDialogUtils;
import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MetadataHelper {
    private static final String[] OFFICE_EXTENSIONS = {
            ".docx", ".docm", ".dotx", ".dotm",
            ".xlsx", ".xlsm", ".xltx", ".xltm",
            ".pptx", ".pptm", ".potx", ".potm"
    };

    private static final Map<String, String> CORE_TAGS;
    private static final Map<String, String> APP_TAGS;

    static {
        LinkedHashMap<String, String> core = new LinkedHashMap<>();
        core.put("title", "Title");
        core.put("subject", "Subject");
        core.put("creator", "Author");
        core.put("lastModifiedBy", "Last modified by");
        core.put("description", "Description");
        core.put("keywords", "Keywords");
        core.put("category", "Category");
        core.put("created", "Created");
        core.put("modified", "Modified");
        core.put("revision", "Revision");
        core.put("language", "Language");
        CORE_TAGS = Collections.unmodifiableMap(core);

        LinkedHashMap<String, String> app = new LinkedHashMap<>();
        app.put("Application", "Application");
        app.put("AppVersion", "App version");
        app.put("Company", "Company");
        app.put("Manager", "Manager");
        app.put("TotalTime", "Total time");
        app.put("Pages", "Pages");
        app.put("Words", "Words");
        app.put("Characters", "Characters");
        app.put("Lines", "Lines");
        app.put("Paragraphs", "Paragraphs");
        app.put("Slides", "Slides");
        app.put("Notes", "Notes");
        app.put("HiddenSlides", "Hidden slides");
        app.put("MMClips", "Media clips");
        app.put("Template", "Template");
        app.put("DocSecurity", "Document security");
        app.put("LinksUpToDate", "Links up to date");
        app.put("HyperlinksChanged", "Hyperlinks changed");
        app.put("SharedDoc", "Shared document");
        APP_TAGS = Collections.unmodifiableMap(app);
    }

    private MetadataHelper() {
    }

    public static boolean shouldShowMetadata(MessageObject messageObject) {
        return isOfficeDocument(messageObject) || isImageMessage(messageObject);
    }

    public static void showMetadata(BaseFragment fragment, MessageObject messageObject, Theme.ResourcesProvider resourcesProvider) {
        if (fragment == null || fragment.getParentActivity() == null || messageObject == null) {
            return;
        }
        String path = ChatActivity.getPathToMessage(messageObject);
        if (TextUtils.isEmpty(path)) {
            AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.MetadataUnavailable));
            return;
        }
        File file = new File(path);
        if (!file.isFile()) {
            AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.MetadataUnavailable));
            return;
        }
        Map<String, String> metadata;
        if (isImageMessage(messageObject)) {
            metadata = readImageMetadata(file);
        } else {
            metadata = readOfficeMetadata(file);
        }
        if (metadata.isEmpty()) {
            AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.MetadataEmpty));
            return;
        }
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(entry.getKey()).append(": ").append(entry.getValue());
        }
        showMetadataDialog(fragment, text.toString(), resourcesProvider);
    }

    private static boolean isOfficeDocument(MessageObject messageObject) {
        if (messageObject == null || messageObject.getDocument() == null) {
            return false;
        }
        String name = messageObject.getDocumentName();
        if (TextUtils.isEmpty(name)) {
            name = FileLoader.getDocumentFileName(messageObject.getDocument());
        }
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        for (String extension : OFFICE_EXTENSIONS) {
            if (lower.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isImageMessage(MessageObject messageObject) {
        if (messageObject == null) {
            return false;
        }
        if (messageObject.isSticker() || messageObject.isAnimatedSticker()) {
            return false;
        }
        if (messageObject.isPhoto()) {
            return true;
        }
        if (messageObject.getDocument() != null) {
            String mime = messageObject.getDocument().mime_type;
            if (!TextUtils.isEmpty(mime) && mime.startsWith("image/")) {
                return true;
            }
            String name = messageObject.getDocumentName();
            if (TextUtils.isEmpty(name)) {
                name = FileLoader.getDocumentFileName(messageObject.getDocument());
            }
            if (!TextUtils.isEmpty(name)) {
                String lower = name.toLowerCase(Locale.ROOT);
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".heic");
            }
        }
        return false;
    }

    private static void showMetadataDialog(BaseFragment fragment, String message, Theme.ResourcesProvider resourcesProvider) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        TextView textView = new TextView(context);
        textView.setText(message);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        textView.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        textView.setTextIsSelectable(true);

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(textView, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT));

        AlertDialog dialog = FluffyDialogUtils.themedBuilder(context, resourcesProvider)
                .setTitle(LocaleController.getString(R.string.Metadata))
                .setView(FluffyDialogUtils.wrapWithStandardPadding(scrollView))
                .setPositiveButton(LocaleController.getString(R.string.Close), null)
                .create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        fragment.showDialog(dialog);
    }

    private static Map<String, String> readOfficeMetadata(File file) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(file);
            ZipEntry coreEntry = zipFile.getEntry("docProps/core.xml");
            if (coreEntry != null) {
                try (InputStream inputStream = zipFile.getInputStream(coreEntry)) {
                    parseXml(inputStream, CORE_TAGS, result);
                }
            }
            ZipEntry appEntry = zipFile.getEntry("docProps/app.xml");
            if (appEntry != null) {
                try (InputStream inputStream = zipFile.getInputStream(appEntry)) {
                    parseXml(inputStream, APP_TAGS, result);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
        return result;
    }

    private static void parseXml(InputStream inputStream, Map<String, String> tags, Map<String, String> out) {
        if (inputStream == null) {
            return;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream, "UTF-8");
            int event = parser.getEventType();
            String captureTag = null;
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    String name = parser.getName();
                    String lookup = name;
                    if (!tags.containsKey(lookup) && name.contains(":")) {
                        lookup = name.substring(name.indexOf(':') + 1);
                    }
                    if (tags.containsKey(lookup)) {
                        captureTag = lookup;
                    } else {
                        captureTag = null;
                    }
                } else if (event == XmlPullParser.TEXT && captureTag != null) {
                    String value = parser.getText();
                    if (!TextUtils.isEmpty(value)) {
                        String label = tags.get(captureTag);
                        if (!TextUtils.isEmpty(label) && !out.containsKey(label)) {
                            out.put(label, value.trim());
                        }
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    captureTag = null;
                }
                event = parser.next();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static Map<String, String> readImageMetadata(File file) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        try {
            androidx.exifinterface.media.ExifInterface exif =
                    new androidx.exifinterface.media.ExifInterface(file.getAbsolutePath());
            putIfPresent(result, "Make", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MAKE));
            putIfPresent(result, "Model", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_MODEL));
            putIfPresent(result, "Date", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL));
            putIfPresent(result, "Software", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE));
            putIfPresent(result, "Width", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_WIDTH));
            putIfPresent(result, "Height", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_LENGTH));
            putIfPresent(result, "Orientation", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION));
            putIfPresent(result, "Exposure", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME));
            putIfPresent(result, "FNumber", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_F_NUMBER));
            putIfPresent(result, "ISO", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ISO_SPEED_RATINGS));
            putIfPresent(result, "FocalLength", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH));
            putIfPresent(result, "Flash", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_FLASH));
            putIfPresent(result, "WhiteBalance", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE));
            putIfPresent(result, "LensMake", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MAKE));
            putIfPresent(result, "LensModel", exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL));
            float[] latLong = new float[2];
            if (exif.getLatLong(latLong)) {
                result.put("GPS", latLong[0] + ", " + latLong[1]);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    private static void putIfPresent(Map<String, String> out, String label, String value) {
        if (!TextUtils.isEmpty(value) && !out.containsKey(label)) {
            out.put(label, value.trim());
        }
    }
}
