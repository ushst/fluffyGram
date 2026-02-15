package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.activities.elements.FluffyDialogUtils;
import org.ushastoe.fluffy.activities.elements.FluffySettingsScaffold;
import org.ushastoe.fluffy.fluffyConfig;
import org.ushastoe.fluffy.quickreplies.FluffyQuickRepliesManager;
import org.ushastoe.fluffy.helpers.WhisperHelper;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import android.content.SharedPreferences;

public class generalActivitySettings extends BaseFragment {
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private Parcelable recyclerViewState = null;

    private List<Row> rows = new ArrayList<>();

    private static final int REQUEST_CODE_IMPORT_FLUFFY_CONFIG = 1003;

    private enum RowType {
        HERO_CARD,
        CARD_SECTION,
        INFO_BLOCK,
        TEXT_CHECK,
        TEXT_CELL,
        TEXT_INFO_PRIVACY
    }

    private enum RowIdentifier {
        GENERAL_HERO,
        GENERAL_INFO,
        DEV_MODE,
        SHOW_FAKE_EDIT_INDICATOR,
        DOWNLOAD_SPEED_BOOST,
        SAVE_EDITED,
        SAVE_DELETED,
        SORT_CHATS_BY_UNREAD,
        UNMUTE_WITH_VOLUME,
        PAUSE_MUSIC_ON_MEDIA,
        EDGE_TO_EDGE_MODE,
        OPEN_LINKS_IN_SYSTEM_BROWSER,
        BIG_PHOTO_SEND,
        TRANSCRIBE_DISABLE_LISTEN_SIGNAL,
        VOICE_PROVIDER_SELECTOR,
        VOICE_PROVIDER_CREDENTIALS,
        EXPORT_FLUFFY_CONFIG,
        IMPORT_FLUFFY_CONFIG,
        ALLOW_ATTACH_ANY_BOT,
        USER_STATUS_LOG_VIEWER,
        HIDE_PINNED_SMALL_SCREEN,
        CUSTOM_QUICK_REPLIES_TOGGLE,
        CUSTOM_QUICK_REPLIES_MANAGE,
        GENERAL_PRODUCTIVITY_HEADER,
        GENERAL_HISTORY_HEADER,
        GENERAL_MEDIA_HEADER,
        GENERAL_DISPLAY_HEADER,
        GENERAL_DEVELOPER_HEADER,
        GENERAL_QUICK_REPLY_HEADER,
        VOICE_PROVIDER_DETAILS_HEADER,
        VOICE_ALERTS_HEADER,
        CONFIG_MANAGEMENT_HEADER,
        EXPERIMENTS_BOTS_HEADER,
        EXPERIMENTS_UTILITIES_HEADER
    }

    private static class Row {
        RowType type;
        RowIdentifier id;
        int textResId;
        int iconResId;
        int subtitleResId;
        CharSequence customText;
        List<Row> children;

        Row(RowIdentifier id, RowType type, int textResId, int iconResId) {
            this(id, type, textResId, iconResId, null);
        }

        Row(RowIdentifier id, RowType type, int textResId, int iconResId, CharSequence customText) {
            this.id = id;
            this.type = type;
            this.textResId = textResId;
            this.iconResId = iconResId;
            this.customText = customText;
        }

        Row(RowIdentifier id, RowType type, int textResId) {
            this(id, type, textResId, 0);
        }

        Row(RowIdentifier id, RowType type) {
            this(id, type, 0, 0);
        }

        Row(RowIdentifier id, RowType type, CharSequence customText) {
            this(id, type, 0, 0, customText);
        }

        static Row createSection(RowIdentifier id, CharSequence title, List<Row> children) {
            Row row = new Row(id, RowType.CARD_SECTION, 0, 0, title);
            row.children = children;
            return row;
        }

        static Row createHero(RowIdentifier id, CharSequence subtitle, int iconResId) {
            Row row = new Row(id, RowType.HERO_CARD, R.string.General, iconResId, subtitle);
            return row;
        }

        static Row createInfo(RowIdentifier id, CharSequence text) {
            Row row = new Row(id, RowType.INFO_BLOCK, 0, 0, text);
            return row;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
        updateRows();
        return true;
    }

    private void updateRows() {
        recyclerViewState = layoutManager != null ? layoutManager.onSaveInstanceState() : null;

        rows.clear();

        rows.add(Row.createHero(RowIdentifier.GENERAL_HERO,
                LocaleController.getString("GeneralHeroSubtitle", R.string.GeneralHeroSubtitle),
                R.drawable.msg_settings));

        addGeneralSubcategories();

        addVoiceSubcategories();

        addConfigurationCategory();

        addExperimentsCategories();

        rows.add(Row.createInfo(RowIdentifier.GENERAL_INFO,
                LocaleController.getString("GeneralInfo", R.string.GeneralInfo)));

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
            if (recyclerViewState != null) {
                layoutManager.onRestoreInstanceState(recyclerViewState);
            }
        }
    }

    private void addGeneralSubcategories() {
        addCategory(RowIdentifier.GENERAL_PRODUCTIVITY_HEADER, "Productivity",
                asList(
                        new Row(RowIdentifier.DOWNLOAD_SPEED_BOOST, RowType.TEXT_CHECK, R.string.downloadSpeedBoost, R.drawable.msg_download),
                        new Row(RowIdentifier.SORT_CHATS_BY_UNREAD, RowType.TEXT_CHECK, R.string.FG_SortByUnread, R.drawable.msg_markread)
                ), true);

        addCategory(RowIdentifier.GENERAL_MEDIA_HEADER, "Media",
                asList(
                        new Row(RowIdentifier.UNMUTE_WITH_VOLUME, RowType.TEXT_CHECK, R.string.unmuteVideoWithVolume, R.drawable.media_unmute),
                        new Row(RowIdentifier.PAUSE_MUSIC_ON_MEDIA, RowType.TEXT_CHECK, R.string.PauseMusicOnMedia, R.drawable.msg_filled_data_music),
                        new Row(RowIdentifier.BIG_PHOTO_SEND, RowType.TEXT_CHECK, R.string.SendLargePhoto, R.drawable.msg_filled_data_photos_solar)
                ), true);

        addCategory(RowIdentifier.GENERAL_DISPLAY_HEADER, "Display",
                asList(
                        new Row(RowIdentifier.EDGE_TO_EDGE_MODE, RowType.TEXT_CELL, R.string.EdgeToEdgeMode, R.drawable.msg_theme),
                        new Row(RowIdentifier.OPEN_LINKS_IN_SYSTEM_BROWSER, RowType.TEXT_CHECK, R.string.DisableDefaultInAppBrowser, R.drawable.msg_link)
                ), true);

        addCategory(RowIdentifier.GENERAL_HISTORY_HEADER, "History",
                asList(
                        new Row(RowIdentifier.SAVE_EDITED, RowType.TEXT_CHECK, R.string.saveEditRow, R.drawable.msg_edit),
                        new Row(RowIdentifier.SAVE_DELETED, RowType.TEXT_CHECK, R.string.saveDelRow, R.drawable.msg_delete)
                ), true);

        addCategory(RowIdentifier.GENERAL_DEVELOPER_HEADER, "Developer Tools",
                asList(
                        new Row(RowIdentifier.DEV_MODE, RowType.TEXT_CHECK, R.string.FG_DevMode, R.drawable.msg_settings),
                        new Row(RowIdentifier.SHOW_FAKE_EDIT_INDICATOR, RowType.TEXT_CHECK, R.string.FG_ShowFakeEditIndicator, R.drawable.msg_edit)
                ), true);

        List<Row> quickRows = new ArrayList<>();
        quickRows.add(new Row(RowIdentifier.CUSTOM_QUICK_REPLIES_TOGGLE, RowType.TEXT_CHECK, R.string.FG_EnableCustomQuickReplies, R.drawable.msg_bot));
        if (fluffyConfig.enableCustomQuickReplies) {
            quickRows.add(new Row(RowIdentifier.CUSTOM_QUICK_REPLIES_MANAGE, RowType.TEXT_CELL, R.string.FG_ManageCustomQuickReplies, R.drawable.msg_settings));
        }
        addCategory(RowIdentifier.GENERAL_QUICK_REPLY_HEADER, "Quick Replies", quickRows, false);
    }

    private void addVoiceSubcategories() {
        addCategory(RowIdentifier.VOICE_PROVIDER_DETAILS_HEADER, "Provider",
                asList(
                        new Row(RowIdentifier.VOICE_PROVIDER_SELECTOR, RowType.TEXT_CELL, R.string.UseCloudflare, R.drawable.voicechat_muted),
                        new Row(RowIdentifier.VOICE_PROVIDER_CREDENTIALS, RowType.TEXT_CELL, R.string.CloudflareCredentials, R.drawable.msg_voicechat_solar)
                ), false);

        addCategory(RowIdentifier.VOICE_ALERTS_HEADER, "Alerts",
                asList(
                        new Row(RowIdentifier.TRANSCRIBE_DISABLE_LISTEN_SIGNAL, RowType.TEXT_CHECK, R.string.FG_TranscribeDisableListenSignal, R.drawable.msg_voicechat)
                ), false);
    }

    private void addConfigurationCategory() {
        addCategory(RowIdentifier.CONFIG_MANAGEMENT_HEADER, "Backup",
                asList(
                        new Row(RowIdentifier.EXPORT_FLUFFY_CONFIG, RowType.TEXT_CELL, R.string.ExportFluffyConfig, R.drawable.msg_download),
                        new Row(RowIdentifier.IMPORT_FLUFFY_CONFIG, RowType.TEXT_CELL, R.string.ImportFluffyConfig, R.drawable.msg_saved)
                ), true);
    }

    private void addExperimentsCategories() {
        addCategory(RowIdentifier.EXPERIMENTS_BOTS_HEADER, "Bots",
                asList(
                        new Row(RowIdentifier.ALLOW_ATTACH_ANY_BOT, RowType.TEXT_CHECK, R.string.AllowAttachAnyBot, R.drawable.msg_bot)
                ), false);

        addCategory(RowIdentifier.EXPERIMENTS_UTILITIES_HEADER, "Utilities",
                asList(
                        new Row(RowIdentifier.USER_STATUS_LOG_VIEWER, RowType.TEXT_CELL, R.string.UserStatusLogTitle, R.drawable.menu_feature_status),
                        new Row(RowIdentifier.HIDE_PINNED_SMALL_SCREEN, RowType.TEXT_CHECK, R.string.HidePinnedOnSmallScreen, R.drawable.msg_pin)
                ), true);
    }

    private void addCategory(RowIdentifier headerId, CharSequence title, List<Row> entries, boolean sortEntries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        if (sortEntries) {
            sortRows(entries);
        }
        rows.add(Row.createSection(headerId, title, entries));
    }

    private void sortRows(List<Row> entries) {
        Collections.sort(entries, (row1, row2) ->
                resolveRowTitle(row1).toString().compareToIgnoreCase(resolveRowTitle(row2).toString()));
    }

    private CharSequence resolveRowTitle(Row row) {
        if (row.customText != null) {
            return row.customText;
        }
        if (row.textResId != 0) {
            return getString(row.textResId);
        }
        return "";
    }

    private ArrayList<Row> asList(Row... rowItems) {
        ArrayList<Row> list = new ArrayList<>(rowItems.length);
        Collections.addAll(list, rowItems);
        return list;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        CacheControlActivity.canceled = true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(getString(R.string.General));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.createMenu().addItem(1000, (Drawable) null).setVisibility(View.INVISIBLE);

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context) {
            @Override
            public Integer getSelectorColor(int position) {
                return getThemedColor(Theme.key_listSelector);
            }
        };
        listView.setSelectorType(9);
        listView.setSelectorDrawableColor(0);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        int padding = FluffySettingsScaffold.getListOuterPadding();
        listView.setPadding(padding, padding, padding, padding);
        listView.setClipToPadding(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);

        return fragmentView;
    }

    private void handleItemClick(Row row, TextCell textCell, Context context) {
        switch (row.id) {
                case DOWNLOAD_SPEED_BOOST:
                    fluffyConfig.toggleDownloadSpeedBoost();
                    textCell.setChecked(fluffyConfig.downloadSpeedBoost);
                    break;
                case DEV_MODE:
                    fluffyConfig.toggleDevModeEnabled();
                    textCell.setChecked(fluffyConfig.devModeEnabled);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    break;
                case SHOW_FAKE_EDIT_INDICATOR:
                    if (fluffyConfig.devModeEnabled) {
                        fluffyConfig.toggleShowFakeEditIcon();
                        textCell.setChecked(fluffyConfig.showFakeEditIcon);
                    }
                    break;
                case SAVE_EDITED:
                    fluffyConfig.toggleSaveEditedMessages();
                    textCell.setChecked(fluffyConfig.saveEditedMessages);
                    break;
                case SAVE_DELETED:
                    fluffyConfig.toggleSaveDeletedMessages();
                    textCell.setChecked(fluffyConfig.saveDeletedMessages);
                    break;
                case SORT_CHATS_BY_UNREAD:
                    fluffyConfig.toggleSortChatsByUnread();
                    textCell.setChecked(fluffyConfig.sortChatsByUnread);
                    break;
                case UNMUTE_WITH_VOLUME:
                    fluffyConfig.toggleUnmuteVideoWithVolume();
                    textCell.setChecked(fluffyConfig.unmuteVideoWithVolume);
                    break;
                case PAUSE_MUSIC_ON_MEDIA:
                    fluffyConfig.togglePauseMusicOnMedia();
                    textCell.setChecked(fluffyConfig.pauseMusicOnMedia);
                    break;
                case EDGE_TO_EDGE_MODE:
                    showEdgeToEdgeModeDialog();
                    break;
                case OPEN_LINKS_IN_SYSTEM_BROWSER: {
                    SharedPreferences preferences = MessagesController.getGlobalMainSettings();
                    boolean currentValue = preferences.getBoolean("disableDefaultInAppBrowser", BuildConfig.SKIP_INTERNAL_BROWSER_BY_DEFAULT);
                    boolean newValue = !currentValue;
                    preferences.edit().putBoolean("disableDefaultInAppBrowser", newValue).apply();
                    textCell.setChecked(newValue);
                    break;
                }
                case BIG_PHOTO_SEND:
                    fluffyConfig.toggleLargePhoto();
                    textCell.setChecked(fluffyConfig.largePhoto);
                    break;
                case TRANSCRIBE_DISABLE_LISTEN_SIGNAL:
                    fluffyConfig.toggleTranscribeDisableListenSignal();
                    textCell.setChecked(fluffyConfig.transcribeDisableListenSignal);
                    break;
                case CUSTOM_QUICK_REPLIES_TOGGLE:
                    fluffyConfig.toggleCustomQuickReplies();
                    textCell.setChecked(fluffyConfig.enableCustomQuickReplies);
                    updateRows();
                    break;
                case CUSTOM_QUICK_REPLIES_MANAGE:
                    presentFragment(new FluffyQuickRepliesActivity());
                    break;
                case EXPORT_FLUFFY_CONFIG:
                    exportFluffyConfig(context);
                    break;
                case IMPORT_FLUFFY_CONFIG:
                    startImportFluffyConfig();
                    break;
                case ALLOW_ATTACH_ANY_BOT:
                    fluffyConfig.toggleAllowAttachAnyBot();
                    textCell.setChecked(fluffyConfig.allowAttachAnyBot);
                    break;
                case USER_STATUS_LOG_VIEWER:
                    presentFragment(new UserStatusLogActivity());
                    break;
                case HIDE_PINNED_SMALL_SCREEN:
                    fluffyConfig.toggleHidePinnedInSmallMode();
                    textCell.setChecked(fluffyConfig.hidePinnedInSmallMode);
                    break;
                case VOICE_PROVIDER_CREDENTIALS:
                    WhisperHelper.showCfCredentialsDialog(this);
                    break;
                case VOICE_PROVIDER_SELECTOR:
                    selectProvider(context);
                    break;
        }
    }

    private void exportFluffyConfig(Context context) {
        if (context == null) {
            return;
        }
        File sourceFile = fluffyConfig.getPreferencesFile();
        if (sourceFile == null || !sourceFile.exists()) {
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ExportFluffyConfigError), getString(R.string.FluffyConfigFileMissing));
            return;
        }
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir == null) {
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ExportFluffyConfigError), null);
            return;
        }
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ExportFluffyConfigError), downloadsDir.getAbsolutePath());
            return;
        }
        File destinationFile = new File(downloadsDir, fluffyConfig.getPreferencesFileName());
        try {
            if (!AndroidUtilities.copyFile(sourceFile, destinationFile)) {
                throw new IOException("Failed to copy file");
            }
            MediaScannerConnection.scanFile(context.getApplicationContext(), new String[]{destinationFile.getAbsolutePath()}, null, null);
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ExportFluffyConfigSuccess), destinationFile.getAbsolutePath());
        } catch (Exception e) {
            FileLog.e(e);
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ExportFluffyConfigError), e.getLocalizedMessage());
        }
    }

    private void startImportFluffyConfig() {
        if (getParentActivity() == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/xml", "application/xml"});
        startActivityForResult(Intent.createChooser(intent, getString(R.string.ImportFluffyConfig)), REQUEST_CODE_IMPORT_FLUFFY_CONFIG);
    }

    private void importFluffyConfigFromUri(Uri uri) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        DocumentFile documentFile = DocumentFile.fromSingleUri(activity, uri);
        String expectedName = fluffyConfig.getPreferencesFileName();
        if (documentFile == null || documentFile.getName() == null || !expectedName.equals(documentFile.getName())) {
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ImportFluffyConfigError), getString(R.string.ImportFluffyConfigWrongFile));
            return;
        }
        File destinationFile = fluffyConfig.getPreferencesFile();
        if (destinationFile == null) {
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ImportFluffyConfigError), getString(R.string.FluffyConfigFileMissing));
            return;
        }
        File parent = destinationFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ImportFluffyConfigError), parent.getAbsolutePath());
            return;
        }
        File tempFile = null;
        try (InputStream inputStream = activity.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                BulletinHelper.showSimpleBulletin(this, getString(R.string.ImportFluffyConfigError), getString(R.string.ImportFluffyConfigWrongFile));
                return;
            }
            tempFile = File.createTempFile("fluffyConfig", ".xml", activity.getCacheDir());
            if (!AndroidUtilities.copyFile(inputStream, tempFile)) {
                throw new IOException("Failed to read selected file");
            }
            if (!AndroidUtilities.copyFile(tempFile, destinationFile)) {
                throw new IOException("Failed to replace preferences");
            }
            // Reload preferences by parsing the XML file manually
            reloadPreferencesFromFile(destinationFile);
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ImportFluffyConfigSuccess), null);
        } catch (Exception e) {
            FileLog.e(e);
            BulletinHelper.showSimpleBulletin(this, getString(R.string.ImportFluffyConfigError), e.getLocalizedMessage());
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private void reloadPreferencesFromFile(File file) throws IOException, XmlPullParserException {
        SharedPreferences preferences = fluffyConfig.getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        FileInputStream fis = new FileInputStream(file);
        parser.setInput(fis, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if ("string".equals(tagName)) {
                    String key = parser.getAttributeValue(null, "name");
                    String value = parser.nextText();
                    editor.putString(key, value);
                } else if ("boolean".equals(tagName)) {
                    String key = parser.getAttributeValue(null, "name");
                    boolean value = Boolean.parseBoolean(parser.getAttributeValue(null, "value"));
                    editor.putBoolean(key, value);
                } else if ("int".equals(tagName)) {
                    String key = parser.getAttributeValue(null, "name");
                    int value = Integer.parseInt(parser.getAttributeValue(null, "value"));
                    editor.putInt(key, value);
                } else if ("long".equals(tagName)) {
                    String key = parser.getAttributeValue(null, "name");
                    long value = Long.parseLong(parser.getAttributeValue(null, "value"));
                    editor.putLong(key, value);
                } else if ("float".equals(tagName)) {
                    String key = parser.getAttributeValue(null, "name");
                    float value = Float.parseFloat(parser.getAttributeValue(null, "value"));
                    editor.putFloat(key, value);
                }
            }
            eventType = parser.next();
        }
        fis.close();
        editor.apply();

        fluffyConfig.reloadFromDisk();
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT_FLUFFY_CONFIG && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importFluffyConfigFromUri(uri);
            } else {
                BulletinHelper.showSimpleBulletin(this, getString(R.string.ImportFluffyConfigError), getString(R.string.ImportFluffyConfigWrongFile));
            }
        }
    }

    private String getProviderLabel(int provider) {
        switch (provider) {
            case fluffyConfig.TRANSCRIBE_PROVIDER_CLOUDFLARE:
                return getString(R.string.FG_TranscribeProviderCloudflare);
            case fluffyConfig.TRANSCRIBE_PROVIDER_LOCAL:
                return getString(R.string.FG_TranscribeProviderLocal);
            case fluffyConfig.TRANSCRIBE_PROVIDER_TELEGRAM:
            default:
                return getString(R.string.FG_TranscribeProviderTelegram);
        }
    }

    private String getEdgeToEdgeModeLabel(int mode) {
        switch (mode) {
            case fluffyConfig.EDGE_MODE_ENABLE:
                return getString(R.string.EdgeToEdgeForce);
            case fluffyConfig.EDGE_MODE_DISABLE:
                return getString(R.string.EdgeToEdgeDisable);
            case fluffyConfig.EDGE_MODE_AUTO:
            default:
                return getString(R.string.EdgeToEdgeAuto);
        }
    }

    private int edgeModeToIndex(int mode) {
        switch (mode) {
            case fluffyConfig.EDGE_MODE_ENABLE:
                return 1;
            case fluffyConfig.EDGE_MODE_DISABLE:
                return 2;
            default:
                return 0;
        }
    }

    private int indexToEdgeMode(int index) {
        switch (index) {
            case 1:
                return fluffyConfig.EDGE_MODE_ENABLE;
            case 2:
                return fluffyConfig.EDGE_MODE_DISABLE;
            default:
                return fluffyConfig.EDGE_MODE_AUTO;
        }
    }

    private void showEdgeToEdgeModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();
        LinearLayout linearLayout = new LinearLayout(getParentActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        CharSequence[] items = new CharSequence[]{
                getString(R.string.EdgeToEdgeAuto),
                getString(R.string.EdgeToEdgeForce),
                getString(R.string.EdgeToEdgeDisable)
        };

        final int currentIndex = edgeModeToIndex(fluffyConfig.edgeToEdgeMode);
        for (int i = 0; i < items.length; ++i) {
            final int index = i;
            RadioColorCell cell = new RadioColorCell(getParentActivity());
            cell.setPadding(dp(4), 0, dp(4), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(items[index], index == currentIndex);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                int newMode = indexToEdgeMode(index);
                if (newMode != fluffyConfig.edgeToEdgeMode) {
                    fluffyConfig.setEdgeToEdgeMode(newMode);
                    if (listAdapter != null) {
                        listAdapter.notifyDataSetChanged();
                    }
                    BulletinHelper.showRestartNotification(this);
                }
                Dialog dlg = dialogRef.get();
                if (dlg != null) {
                    dlg.dismiss();
                }
            });
        }

        AlertDialog dialog = FluffyDialogUtils.themedBuilder(getParentActivity())
                .setTitle(getString(R.string.EdgeToEdgeMode))
                .setView(FluffyDialogUtils.wrapWithStandardPadding(linearLayout))
                .setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null)
                .create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        dialogRef.set(dialog);
        showDialog(dialog);
    }

    private void selectProvider(Context context) {
        if (getParentActivity() == null) {
            return;
        }
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        CharSequence[] items = new CharSequence[]{
                getString(R.string.FG_TranscribeProviderTelegram),
                getString(R.string.FG_TranscribeProviderCloudflare),
                getString(R.string.FG_TranscribeProviderLocal)
        };

        for (int i = 0; i < items.length; ++i) {
            final int index = i;
            RadioColorCell cell = new RadioColorCell(getParentActivity());
            cell.setPadding(dp(4), 0, dp(4), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(items[index], index == fluffyConfig.voiceUseCloudflare);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                fluffyConfig.setProviderVoice(index);
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
                dialogRef.get().dismiss();
            });
        }

        AlertDialog dialog = FluffyDialogUtils.themedBuilder(getParentActivity())
                .setTitle(getString(R.string.UseCloudflare))
                .setView(FluffyDialogUtils.wrapWithStandardPadding(linearLayout))
                .setNegativeButton(getString("Cancel", R.string.Cancel), null)
                .create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        dialogRef.set(dialog);
        showDialog(dialog);
    }


    @Override
    public void onResume() {
        super.onResume();
        // Просто обновляем строки без полного пересоздания адаптера
        if(listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private static class HeroCardView extends FrameLayout {
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;

        HeroCardView(Context context) {
            super(context);
            int radius = AndroidUtilities.dp(22);
            setBackground(Theme.createRoundRectDrawable(radius, Theme.getColor(Theme.key_windowBackgroundWhite)));
            setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(20), AndroidUtilities.dp(18));
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = AndroidUtilities.dp(16);
            setLayoutParams(params);

            iconView = new ImageView(context);
            iconView.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon));
            addView(iconView, LayoutHelper.createFrame(52, 52, Gravity.START | Gravity.TOP));

            titleView = new TextView(context);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, 64, 0, 0, 0));

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            subtitleView.setLineSpacing(AndroidUtilities.dp(2), 1.1f);
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            addView(subtitleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.TOP, 64, 30, 0, 0));
        }

        void bind(int iconRes, CharSequence title, CharSequence subtitle) {
            iconView.setImageResource(iconRes);
            titleView.setText(title);
            subtitleView.setText(subtitle);
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            Row row = rows.get(position);
            switch (row.type) {
                case HERO_CARD:
                    ((HeroCardHolder) holder).bind(row);
                    break;
                case CARD_SECTION:
                    ((CardSectionHolder) holder).bind(row);
                    break;
                case INFO_BLOCK:
                    ((InfoBlockHolder) holder).bind(row);
                    break;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new HeroCardHolder(new HeroCardView(mContext));
            } else if (viewType == 1) {
                return new CardSectionHolder(mContext);
            } else {
                return new InfoBlockHolder(mContext);
            }
        }

        @Override
        public int getItemViewType(int position) {
            Row row = rows.get(position);
            if (row.type == RowType.HERO_CARD) {
                return 0;
            } else if (row.type == RowType.CARD_SECTION) {
                return 1;
            }
            return 2;
        }

        private class HeroCardHolder extends RecyclerView.ViewHolder {
            private final HeroCardView heroCardView;

            HeroCardHolder(HeroCardView view) {
                super(view);
                heroCardView = view;
            }

            void bind(Row row) {
                CharSequence title = row.textResId != 0 ? getString(row.textResId) : row.customText;
                CharSequence subtitle = row.customText;
                heroCardView.bind(row.iconResId, title, subtitle);
            }
        }

        private class CardSectionHolder extends RecyclerView.ViewHolder {
            private final TextView titleView;
            private final LinearLayout contentLayout;

            CardSectionHolder(Context context) {
                super(new LinearLayout(context));
                LinearLayout root = (LinearLayout) itemView;
                root.setOrientation(LinearLayout.VERTICAL);
                RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = AndroidUtilities.dp(14);
                int sideInset = FluffySettingsScaffold.getCardHorizontalInset();
                params.leftMargin = sideInset;
                params.rightMargin = sideInset;
                root.setLayoutParams(params);
                root.setPadding(0, 0, 0, 0);

                titleView = new TextView(context);
                FluffySettingsScaffold.styleSectionTitle(titleView);
                titleView.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), AndroidUtilities.dp(6));
                root.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                FrameLayout card = new FrameLayout(context);
                card.setBackground(FluffySettingsScaffold.createCardBackground());
                card.setPadding(0, 0, 0, 0);
                root.addView(card, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                contentLayout = new LinearLayout(context);
                contentLayout.setOrientation(LinearLayout.VERTICAL);
                contentLayout.setDividerDrawable(null);
                card.addView(contentLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            void bind(Row row) {
                CharSequence title = row.customText != null ? row.customText : (row.textResId != 0 ? getString(row.textResId) : "");
                titleView.setText(title);
                contentLayout.removeAllViews();
                if (row.children == null) {
                    return;
                }
                for (int i = 0; i < row.children.size(); i++) {
                    Row child = row.children.get(i);
                    View childView = createChildView(
                            child,
                            shouldDrawDivider(row.children, i),
                            isTopInteractiveRow(row.children, i),
                            isBottomInteractiveRow(row.children, i));
                    if (childView != null) {
                        contentLayout.addView(childView);
                    }
                }
            }

            private boolean shouldDrawDivider(List<Row> children, int index) {
                for (int i = index + 1; i < children.size(); i++) {
                    Row next = children.get(i);
                    if (next.type == RowType.TEXT_INFO_PRIVACY) {
                        continue;
                    }
                    return true;
                }
                return false;
            }

            private View createChildView(Row child, boolean needDivider, boolean isTop, boolean isBottom) {
                switch (child.type) {
                    case TEXT_CELL: {
                        TextCell cell = new TextCell(mContext);
                        applyInteractiveBackground(cell, isTop, isBottom);
                        bindTextCell(cell, child, needDivider);
                        cell.setOnClickListener(v -> handleItemClick(child, cell, mContext));
                        return cell;
                    }
                    case TEXT_CHECK: {
                        TextCell cell = new TextCell(mContext, 0, false, true, null);
                        applyInteractiveBackground(cell, isTop, isBottom);
                        bindCheckCell(cell, child, needDivider);
                        cell.setOnClickListener(v -> handleItemClick(child, cell, mContext));
                        return cell;
                    }
                    case TEXT_INFO_PRIVACY: {
                        TextInfoPrivacyCell infoCell = new TextInfoPrivacyCell(mContext);
                        infoCell.setBackground(null);
                        infoCell.setText(child.customText != null ? child.customText : (child.textResId != 0 ? getString(child.textResId) : ""));
                        LinearLayout.LayoutParams params = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
                        params.topMargin = AndroidUtilities.dp(6);
                        infoCell.setLayoutParams(params);
                        return infoCell;
                    }
                    default:
                        return null;
                }
            }

            private void bindTextCell(TextCell cell, Row child, boolean needDivider) {
                String title = child.textResId != 0 ? getString(child.textResId) : (child.customText != null ? child.customText.toString() : "");
                if (child.id == RowIdentifier.VOICE_PROVIDER_SELECTOR) {
                    String value = getProviderLabel(fluffyConfig.voiceUseCloudflare);
                    cell.setTextAndValueAndIcon(title, value, child.iconResId, needDivider);
                } else if (child.id == RowIdentifier.EDGE_TO_EDGE_MODE) {
                    String value = getEdgeToEdgeModeLabel(fluffyConfig.edgeToEdgeMode);
                    cell.setTextAndValueAndIcon(title, value, child.iconResId, needDivider);
                } else if (child.id == RowIdentifier.CUSTOM_QUICK_REPLIES_MANAGE) {
                    int count = FluffyQuickRepliesManager.getInstance().getRepliesCount();
                    String value = LocaleController.formatPluralString("FG_CustomQuickRepliesCount", count);
                    cell.setTextAndValueAndIcon(title, value, child.iconResId, needDivider);
                } else {
                    cell.setTextAndIcon(title, child.iconResId, needDivider);
                }
            }

            private void bindCheckCell(TextCell cell, Row child, boolean needDivider) {
                boolean enabled = true;
                boolean checked = false;
                switch (child.id) {
                    case DEV_MODE:
                        checked = fluffyConfig.devModeEnabled;
                        break;
                    case SHOW_FAKE_EDIT_INDICATOR:
                        checked = fluffyConfig.showFakeEditIcon;
                        enabled = fluffyConfig.devModeEnabled;
                        break;
                    case DOWNLOAD_SPEED_BOOST:
                        checked = fluffyConfig.downloadSpeedBoost;
                        break;
                    case BIG_PHOTO_SEND:
                        checked = fluffyConfig.largePhoto;
                        break;
                    case ALLOW_ATTACH_ANY_BOT:
                        checked = fluffyConfig.allowAttachAnyBot;
                        break;
                    case HIDE_PINNED_SMALL_SCREEN:
                        checked = fluffyConfig.hidePinnedInSmallMode;
                        break;
                    case SAVE_EDITED:
                        checked = fluffyConfig.saveEditedMessages;
                        break;
                    case SAVE_DELETED:
                        checked = fluffyConfig.saveDeletedMessages;
                        break;
                    case SORT_CHATS_BY_UNREAD:
                        checked = fluffyConfig.sortChatsByUnread;
                        break;
                    case UNMUTE_WITH_VOLUME:
                        checked = fluffyConfig.unmuteVideoWithVolume;
                        break;
                    case PAUSE_MUSIC_ON_MEDIA:
                        checked = fluffyConfig.pauseMusicOnMedia;
                        break;
                    case OPEN_LINKS_IN_SYSTEM_BROWSER:
                        checked = MessagesController.getGlobalMainSettings()
                                .getBoolean("disableDefaultInAppBrowser", BuildConfig.SKIP_INTERNAL_BROWSER_BY_DEFAULT);
                        break;
                    case TRANSCRIBE_DISABLE_LISTEN_SIGNAL:
                        checked = fluffyConfig.transcribeDisableListenSignal;
                        break;
                    case CUSTOM_QUICK_REPLIES_TOGGLE:
                        checked = fluffyConfig.enableCustomQuickReplies;
                        break;
                }
                cell.setEnabled(enabled);
                cell.setAlpha(enabled ? 1f : 0.5f);
                cell.setTextAndCheckAndIcon(getString(child.textResId), checked, child.iconResId, needDivider);
            }

            private boolean isInteractiveRow(Row row) {
                return row.type == RowType.TEXT_CELL || row.type == RowType.TEXT_CHECK;
            }

            private boolean isTopInteractiveRow(List<Row> children, int index) {
                if (!isInteractiveRow(children.get(index))) {
                    return false;
                }
                for (int i = index - 1; i >= 0; i--) {
                    if (isInteractiveRow(children.get(i))) {
                        return false;
                    }
                }
                return true;
            }

            private boolean isBottomInteractiveRow(List<Row> children, int index) {
                if (!isInteractiveRow(children.get(index))) {
                    return false;
                }
                for (int i = index + 1; i < children.size(); i++) {
                    if (isInteractiveRow(children.get(i))) {
                        return false;
                    }
                }
                return true;
            }

            private void applyInteractiveBackground(View view, boolean top, boolean bottom) {
                int radius = FluffySettingsScaffold.getCardRadius();
                int topRadius = top ? radius : 0;
                int bottomRadius = bottom ? radius : 0;
                int selectorColor = Theme.getColor(Theme.key_listSelector);
                view.setBackground(Theme.createSimpleSelectorRoundRectDrawable(
                        topRadius,
                        topRadius,
                        bottomRadius,
                        bottomRadius,
                        0,
                        selectorColor,
                        selectorColor));
            }
        }

        private class InfoBlockHolder extends RecyclerView.ViewHolder {
            private final TextView textView;

            InfoBlockHolder(Context context) {
                super(new TextView(context));
                textView = (TextView) itemView;
                RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
                params.bottomMargin = AndroidUtilities.dp(16);
                textView.setLayoutParams(params);
                FluffySettingsScaffold.styleInfoBlock(textView);
            }

            void bind(Row row) {
                textView.setText(row.customText != null ? row.customText : (row.textResId != 0 ? getString(row.textResId) : ""));
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextCell.class, TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        return themeDescriptions;
    }
}
