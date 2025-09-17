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
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.fluffyConfig;
import org.ushastoe.fluffy.helpers.WhisperHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class generalActivitySettings extends BaseFragment {
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;
    private Parcelable recyclerViewState = null;

    private List<Row> rows = new ArrayList<>();

    private static final int REQUEST_CODE_IMPORT_FLUFFY_CONFIG = 1003;

    private enum RowType {
        HEADER,
        TEXT_CHECK,
        TEXT_CELL,
        SHADOW_SECTION
    }

    private enum RowIdentifier {
        GENERAL_HEADER,
        DOWNLOAD_SPEED_BOOST,
        SAVE_EDITED,
        SAVE_DELETED,
        UNMUTE_WITH_VOLUME,
        PAUSE_MUSIC_ON_MEDIA,
        BIG_PHOTO_SEND,
        EXPORT_FLUFFY_CONFIG,
        IMPORT_FLUFFY_CONFIG,
        ALLOW_ATTACH_ANY_BOT,
        DIVIDER_1,
        VOICE_RECOGNITION_HEADER,
        VOICE_PROVIDER_SELECTOR,
        VOICE_PROVIDER_CREDENTIALS,
        EXPERIMENTAL_SETTINGS_HEADER
    }

    private static class Row {
        RowType type;
        RowIdentifier id;
        int textResId;
        int iconResId;

        Row(RowIdentifier id, RowType type, int textResId, int iconResId) {
            this.id = id;
            this.type = type;
            this.textResId = textResId;
            this.iconResId = iconResId;
        }

        Row(RowIdentifier id, RowType type, int textResId) {
            this(id, type, textResId, 0);
        }

        Row(RowIdentifier id, RowType type) {
            this(id, type, 0, 0);
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

        rows.add(new Row(RowIdentifier.GENERAL_HEADER, RowType.HEADER, R.string.General));
        rows.add(new Row(RowIdentifier.DOWNLOAD_SPEED_BOOST, RowType.TEXT_CHECK, R.string.downloadSpeedBoost, R.drawable.msg_download));
        rows.add(new Row(RowIdentifier.SAVE_EDITED, RowType.TEXT_CHECK, R.string.saveEditRow, R.drawable.msg_edit));
        rows.add(new Row(RowIdentifier.SAVE_DELETED, RowType.TEXT_CHECK, R.string.saveDelRow, R.drawable.msg_delete));
        rows.add(new Row(RowIdentifier.UNMUTE_WITH_VOLUME, RowType.TEXT_CHECK, R.string.unmuteVideoWithVolume, R.drawable.media_unmute));
        rows.add(new Row(RowIdentifier.PAUSE_MUSIC_ON_MEDIA, RowType.TEXT_CHECK, R.string.PauseMusicOnMedia, R.drawable.msg_filled_data_music));
        rows.add(new Row(RowIdentifier.BIG_PHOTO_SEND, RowType.TEXT_CHECK, R.string.SendLargePhoto, R.drawable.msg_filled_data_photos_solar));
        rows.add(new Row(RowIdentifier.EXPORT_FLUFFY_CONFIG, RowType.TEXT_CELL, R.string.ExportFluffyConfig, R.drawable.msg_download));
        rows.add(new Row(RowIdentifier.IMPORT_FLUFFY_CONFIG, RowType.TEXT_CELL, R.string.ImportFluffyConfig, R.drawable.msg_saved));

        rows.add(new Row(RowIdentifier.DIVIDER_1, RowType.SHADOW_SECTION));
        rows.add(new Row(RowIdentifier.VOICE_RECOGNITION_HEADER, RowType.HEADER, R.string.Voip));
        rows.add(new Row(RowIdentifier.VOICE_PROVIDER_SELECTOR, RowType.TEXT_CELL, R.string.UseCloudflare, R.drawable.voicechat_muted));
        rows.add(new Row(RowIdentifier.VOICE_PROVIDER_CREDENTIALS, RowType.TEXT_CELL, R.string.CloudflareCredentials, R.drawable.msg_voicechat_solar));

        rows.add(new Row(RowIdentifier.EXPERIMENTAL_SETTINGS_HEADER, RowType.HEADER, R.string.Other));
        rows.add(new Row(RowIdentifier.ALLOW_ATTACH_ANY_BOT, RowType.TEXT_CHECK, R.string.AllowAttachAnyBot, R.drawable.msg_bot));


        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
            if (recyclerViewState != null) {
                layoutManager.onRestoreInstanceState(recyclerViewState);
            }
        }
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
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setAdapter(listAdapter);

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);

        // Упрощенный обработчик нажатий
        listView.setOnItemClickListener((view, position) -> {
            Row row = rows.get(position);
            handleItemClick(row.id, view, context);
        });

        return fragmentView;
    }

    private void handleItemClick(RowIdentifier rowId, View view, Context context) {
        if (view instanceof TextCell) {
            TextCell textCell = (TextCell) view;
            switch (rowId) {
                case DOWNLOAD_SPEED_BOOST:
                    fluffyConfig.toggleDownloadSpeedBoost();
                    textCell.setChecked(fluffyConfig.downloadSpeedBoost);
                    break;
                case SAVE_EDITED:
                    fluffyConfig.toggleSaveEditedMessages();
                    textCell.setChecked(fluffyConfig.saveEditedMessages);
                    break;
                case SAVE_DELETED:
                    fluffyConfig.toggleSaveDeletedMessages();
                    textCell.setChecked(fluffyConfig.saveDeletedMessages);
                    break;
                case UNMUTE_WITH_VOLUME:
                    fluffyConfig.toggleUnmuteVideoWithVolume();
                    textCell.setChecked(fluffyConfig.unmuteVideoWithVolume);
                    break;
                case PAUSE_MUSIC_ON_MEDIA:
                    fluffyConfig.togglePauseMusicOnMedia();
                    textCell.setChecked(fluffyConfig.pauseMusicOnMedia);
                    break;
                case BIG_PHOTO_SEND:
                    fluffyConfig.toggleLargePhoto();
                    textCell.setChecked(fluffyConfig.largePhoto);
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
                case VOICE_PROVIDER_CREDENTIALS:
                    WhisperHelper.showCfCredentialsDialog(this);
                    break;
                case VOICE_PROVIDER_SELECTOR:
                    selectProvider(context);
                    break;
            }
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
            fluffyConfig.reloadFromDisk();
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

    private int getRowPositionById(RowIdentifier id) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    private void selectProvider(Context context) {
        if (getParentActivity() == null) {
            return;
        }
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        CharSequence[] items = new CharSequence[]{
                "Telegram",
                "Cloudflare"
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
                int position = getRowPositionById(RowIdentifier.VOICE_PROVIDER_SELECTOR);
                if (position != -1) {
                    listAdapter.notifyItemChanged(position);
                }
                dialogRef.get().dismiss();
            });
        }

        Dialog dialog = new AlertDialog.Builder(getParentActivity())
                .setTitle(getString(R.string.UseCloudflare))
                .setView(linearLayout)
                .setNegativeButton(getString("Cancel", R.string.Cancel), null)
                .create();
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

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
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
                case SHADOW_SECTION:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(getString(row.textResId));
                    break;
                case TEXT_CELL:
                    TextCell textCell = (TextCell) holder.itemView;
                    if (row.id == RowIdentifier.VOICE_PROVIDER_SELECTOR) {
                        String value = fluffyConfig.useCloudFlare() ? "Cloudflare" : "Telegram";
                        textCell.setTextAndValueAndIcon(getString(row.textResId), value, row.iconResId, true);
                    } else {
                        textCell.setTextAndIcon(getString(row.textResId), row.iconResId, true);
                    }
                    break;
                case TEXT_CHECK:
                    TextCell textCheckCell = (TextCell) holder.itemView;
                    textCheckCell.setEnabled(true);
                    boolean checked = false;
                    switch (row.id) {
                        case DOWNLOAD_SPEED_BOOST:
                            checked = fluffyConfig.downloadSpeedBoost;
                            break;
                        case BIG_PHOTO_SEND:
                            checked = fluffyConfig.largePhoto;
                            break;
                        case ALLOW_ATTACH_ANY_BOT:
                            checked = fluffyConfig.allowAttachAnyBot;
                            break;
                        case SAVE_EDITED:
                            checked = fluffyConfig.saveEditedMessages;
                            break;
                        case SAVE_DELETED:
                            checked = fluffyConfig.saveDeletedMessages;
                            break;
                        case UNMUTE_WITH_VOLUME:
                            checked = fluffyConfig.unmuteVideoWithVolume;
                            break;
                        case PAUSE_MUSIC_ON_MEDIA:
                            checked = fluffyConfig.pauseMusicOnMedia;
                            break;
                    }
                    textCheckCell.setTextAndCheckAndIcon(getString(row.textResId), checked, row.iconResId, true);
                    break;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position < 0 || position >= rows.size()) {
                return false;
            }
            Row row = rows.get(position);
            return row.type != RowType.SHADOW_SECTION && row.type != RowType.HEADER;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0: // HEADER
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1: // TEXT_CHECK
                    view = new TextCell(mContext, 0, false, true, null);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2: // TEXT_CELL
                    view = new TextCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3: // SHADOW_SECTION
                default:
                    view = new ShadowSectionCell(mContext);
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }


        @Override
        public int getItemViewType(int position) {
            Row row = rows.get(position);
            switch (row.type) {
                case HEADER:
                    return 0;
                case TEXT_CHECK:
                    return 1;
                case TEXT_CELL:
                    return 2;
                case SHADOW_SECTION:
                    return 3;
                default:
                    return 3;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, NotificationsCheckCell.class, TextCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        return themeDescriptions;
    }
}