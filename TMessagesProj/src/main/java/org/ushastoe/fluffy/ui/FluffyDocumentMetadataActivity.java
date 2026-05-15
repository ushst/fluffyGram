package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.patches.DocumentMetadataPatch;

import java.util.ArrayList;

public class FluffyDocumentMetadataActivity extends BaseFragment {

    private static final int VIEW_TYPE_ROW = 0;
    private static final int VIEW_TYPE_INFO = 1;

    private final MessageObject messageObject;
    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<DocumentMetadataPatch.MetadataRow> rows = new ArrayList<>();
    private boolean loading = true;
    private String infoText;
    private int loadGeneration;

    public FluffyDocumentMetadataActivity(MessageObject messageObject) {
        this.messageObject = messageObject;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyDocumentMetadataTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < rows.size()) {
                AndroidUtilities.addToClipboard(rows.get(position).value);
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = frameLayout;
        startLoading();
        return fragmentView;
    }

    private void startLoading() {
        loading = true;
        rows.clear();
        infoText = LocaleController.getString(R.string.FluffyDocumentMetadataLoading);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        final int requestGeneration = ++loadGeneration;
        DocumentMetadataPatch.loadMetadata(messageObject, state -> {
            if (requestGeneration != loadGeneration) {
                return;
            }
            loading = false;
            rows.clear();
            rows.addAll(state.rows);
            infoText = resolveInfoText(state.status);
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private String resolveInfoText(String status) {
        if ("file_missing".equals(status)) {
            return LocaleController.getString(R.string.FluffyDocumentMetadataFileMissing);
        }
        if ("parse_failed".equals(status)) {
            return LocaleController.getString(R.string.FluffyDocumentMetadataParseFailed);
        }
        if ("empty".equals(status)) {
            return LocaleController.getString(R.string.FluffyDocumentMetadataEmpty);
        }
        return null;
    }

    private String getLabelForKey(String key) {
        switch (key) {
            case "file_name":
                return LocaleController.getString(R.string.FluffyDocumentMetadataFileName);
            case "format":
                return LocaleController.getString(R.string.FluffyDocumentMetadataFormat);
            case "file_path":
                return LocaleController.getString(R.string.filePathRow);
            case "title":
                return LocaleController.getString(R.string.FluffyDocumentMetadataTitleField);
            case "subject":
                return LocaleController.getString(R.string.FluffyDocumentMetadataSubject);
            case "author":
                return LocaleController.getString(R.string.FluffyDocumentMetadataAuthor);
            case "keywords":
                return LocaleController.getString(R.string.FluffyDocumentMetadataKeywords);
            case "description":
                return LocaleController.getString(R.string.FluffyDocumentMetadataDescription);
            case "creator":
                return LocaleController.getString(R.string.FluffyDocumentMetadataCreator);
            case "producer":
                return LocaleController.getString(R.string.FluffyDocumentMetadataProducer);
            case "created":
                return LocaleController.getString(R.string.FluffyDocumentMetadataCreated);
            case "modified":
                return LocaleController.getString(R.string.FluffyDocumentMetadataModified);
            case "last_modified_by":
                return LocaleController.getString(R.string.FluffyDocumentMetadataLastModifiedBy);
            case "application":
                return LocaleController.getString(R.string.FluffyDocumentMetadataApplication);
            case "application_version":
                return LocaleController.getString(R.string.FluffyDocumentMetadataApplicationVersion);
            case "company":
                return LocaleController.getString(R.string.FluffyDocumentMetadataCompany);
            case "manager":
                return LocaleController.getString(R.string.FluffyDocumentMetadataManager);
            case "pages":
                return LocaleController.getString(R.string.FluffyDocumentMetadataPages);
            case "words":
                return LocaleController.getString(R.string.FluffyDocumentMetadataWords);
            case "characters":
                return LocaleController.getString(R.string.FluffyDocumentMetadataCharacters);
            case "slides":
                return LocaleController.getString(R.string.FluffyDocumentMetadataSlides);
            case "notes":
                return LocaleController.getString(R.string.FluffyDocumentMetadataNotes);
            case "total_time":
                return LocaleController.getString(R.string.FluffyDocumentMetadataTotalTime);
            case "presentation_format":
                return LocaleController.getString(R.string.FluffyDocumentMetadataPresentationFormat);
            case "image_width":
                return LocaleController.getString(R.string.FluffyDocumentMetadataImageWidth);
            case "image_height":
                return LocaleController.getString(R.string.FluffyDocumentMetadataImageHeight);
            case "make":
                return LocaleController.getString(R.string.FluffyDocumentMetadataCameraMake);
            case "model":
                return LocaleController.getString(R.string.FluffyDocumentMetadataCameraModel);
            case "software":
                return LocaleController.getString(R.string.FluffyDocumentMetadataSoftware);
            case "orientation":
                return LocaleController.getString(R.string.FluffyDocumentMetadataOrientation);
            case "date_time_original":
                return LocaleController.getString(R.string.FluffyDocumentMetadataDateTimeOriginal);
            case "date_time_digitized":
                return LocaleController.getString(R.string.FluffyDocumentMetadataDateTimeDigitized);
            case "artist":
                return LocaleController.getString(R.string.FluffyDocumentMetadataArtist);
            case "copyright":
                return LocaleController.getString(R.string.FluffyDocumentMetadataCopyright);
            case "exposure_time":
                return LocaleController.getString(R.string.FluffyDocumentMetadataExposureTime);
            case "f_number":
                return LocaleController.getString(R.string.FluffyDocumentMetadataFNumber);
            case "iso":
                return LocaleController.getString(R.string.FluffyDocumentMetadataIso);
            case "focal_length":
                return LocaleController.getString(R.string.FluffyDocumentMetadataFocalLength);
            case "lens_model":
                return LocaleController.getString(R.string.FluffyDocumentMetadataLensModel);
            case "flash":
                return LocaleController.getString(R.string.FluffyDocumentMetadataFlash);
            case "white_balance":
                return LocaleController.getString(R.string.FluffyDocumentMetadataWhiteBalance);
            case "gps":
                return LocaleController.getString(R.string.FluffyDocumentMetadataGps);
            case "gps_altitude":
                return LocaleController.getString(R.string.FluffyDocumentMetadataGpsAltitude);
            default:
                return key;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            return rows.size() + (loading || infoText != null ? 1 : 0);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == VIEW_TYPE_ROW;
        }

        @Override
        public int getItemViewType(int position) {
            return position < rows.size() ? VIEW_TYPE_ROW : VIEW_TYPE_INFO;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ROW) {
                TextDetailSettingsCell cell = new TextDetailSettingsCell(parent.getContext());
                cell.setMultilineDetail(true);
                return new RecyclerListView.Holder(cell);
            }
            TextInfoPrivacyCell cell = new TextInfoPrivacyCell(parent.getContext());
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_ROW) {
                DocumentMetadataPatch.MetadataRow row = rows.get(position);
                ((TextDetailSettingsCell) holder.itemView).setTextAndValue(getLabelForKey(row.key), row.value, false);
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(loading ? LocaleController.getString(R.string.FluffyDocumentMetadataLoading) : infoText);
            }
        }
    }
}
