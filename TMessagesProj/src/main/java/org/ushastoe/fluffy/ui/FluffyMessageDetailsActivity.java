package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FluffyMessageDetailsActivity extends BaseFragment {

    private final MessageObject messageObject;
    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<RowData> rows = new ArrayList<>();

    public FluffyMessageDetailsActivity(MessageObject messageObject) {
        this.messageObject = messageObject;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.MessageDetails));
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
        rebuildRows();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        rebuildRows();
    }

    private void rebuildRows() {
        rows.clear();
        if (messageObject == null || messageObject.messageOwner == null) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return;
        }

        TLRPC.Message message = messageObject.messageOwner;
        addRow(LocaleController.getString(R.string.idRow), String.valueOf(message.id));
        addRow(LocaleController.getString(R.string.dateRow), formatTime(message.date));

        if (!TextUtils.isEmpty(messageObject.messageText)) {
            addRow(LocaleController.getString(R.string.textRow), messageObject.messageText.toString());
        }
        if (!TextUtils.isEmpty(messageObject.caption)) {
            addRow(LocaleController.getString(R.string.captionRow), messageObject.caption.toString());
        }
        if (message.edit_date != 0) {
            addRow(LocaleController.getString(R.string.editDateRow), formatTime(message.edit_date));
        }
        if (message.from_scheduled) {
            addRow(LocaleController.getString(R.string.scheduleRow), String.valueOf(true));
        }
        if (message.silent) {
            addRow(LocaleController.getString(R.string.silenceRow), String.valueOf(true));
        }

        if (message.from_id != null) {
            if (message.from_id.user_id != 0) {
                addRow(LocaleController.getString(R.string.ownerIdRow), String.valueOf(message.from_id.user_id));
            } else if (message.from_id.channel_id != 0) {
                addRow(LocaleController.getString(R.string.ownerIdRow), String.valueOf(-message.from_id.channel_id));
            } else if (message.from_id.chat_id != 0) {
                addRow(LocaleController.getString(R.string.ownerIdRow), String.valueOf(-message.from_id.chat_id));
            }
        }

        long docId = 0;
        TLRPC.MessageMedia media = MessageObject.getMedia(message);
        if (media != null) {
            if (media.document != null) {
                docId = media.document.id;
            } else if (media.photo != null) {
                docId = media.photo.id;
            }
        }
        if (docId != 0) {
            addRow(LocaleController.getString(R.string.docIdRow), String.valueOf(docId));
        }

        String filePath = resolveFilePath(messageObject);
        if (!TextUtils.isEmpty(filePath)) {
            addRow(LocaleController.getString(R.string.filePathRow), filePath);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private String resolveFilePath(MessageObject object) {
        String path = object.messageOwner.attachPath;
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                return path;
            }
        }

        File fromMessage = FileLoader.getInstance(currentAccount).getPathToMessage(object.messageOwner);
        if (fromMessage != null && fromMessage.exists()) {
            return fromMessage.getAbsolutePath();
        }

        File fromAttach = FileLoader.getInstance(currentAccount).getPathToAttach(object.getDocument(), true);
        if (fromAttach != null && fromAttach.exists()) {
            return fromAttach.getAbsolutePath();
        }
        return null;
    }

    private void addRow(String title, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        rows.add(new RowData(title, value));
    }

    private String formatTime(int timestamp) {
        if (timestamp == 0x7ffffffe) {
            return LocaleController.getString(R.string.MessageScheduledUntilOnline);
        }
        Date date = new Date(timestamp * 1000L);
        return timestamp + "\n" + LocaleController.formatString(
                R.string.formatDateAtTime,
                LocaleController.getInstance().getFormatterYear().format(date),
                LocaleController.getInstance().getFormatterDayWithSeconds().format(date));
    }

    private static class RowData {
        final String title;
        final String value;

        RowData(String title, String value) {
            this.title = title;
            this.value = value;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextDetailSettingsCell cell = new TextDetailSettingsCell(parent.getContext());
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cell.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            cell.setMultilineDetail(true);
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowData row = rows.get(position);
            ((TextDetailSettingsCell) holder.itemView).setTextAndValue(row.title, row.value, false);
        }
    }
}
