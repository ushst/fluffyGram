/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.ushastoe.fluffy.activities;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.*;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.*;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.telegram.messenger.LocaleController.getString;

public class MessageDetailsActivity extends BaseFragment {
    private final MessageObject messageObject;
    public static final int OPEN_PROFILE_ID = 2;
    private LinearLayoutManager layoutManager;

    public MessageDetailsActivity(MessageObject messageObject) {
        System.out.println(messageObject.messageOwner.id);
        this.messageObject = messageObject;
    }

    private String filePath;
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    @SuppressWarnings("FieldCanBeLocal")

    private int rowCount;

    private static class RowData {
        int id;
        String title;
        String value;
        int action;
        long data;
        public RowData(int id, String title, String value) {
            this(id, title, value, 0, 0);
        }

        public RowData(int id, String title, String value, int action) {
            this(id, title, value, action, 0);
        }

        public RowData(int id, String title, String value, int action, long data) {
            this.id = id;
            this.title = title;
            this.value = value;
            this.action = action;
            this.data = data;
        }


    }
    private List<RowData> rowDataList;

    private String formatTime(int timestamp) {
        if (timestamp == 0x7ffffffe) {
            return "When online";
        } else {
            return timestamp + "\n" + LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterYear().format(new Date(timestamp * 1000L)), LocaleController.getInstance().getFormatterDayWithSeconds().format(new Date(timestamp * 1000L)));
        }
    }
    private TLRPC.Peer forwardFromPeer;

    private void updateRows(boolean fullNotify) {
        rowDataList = new ArrayList<>();

        rowDataList.add(new RowData(rowCount++, getString(R.string.idRow), Integer.toString(messageObject.messageOwner.id)));
        rowDataList.add(new RowData(rowCount++, getString(R.string.dateRow), formatTime(messageObject.messageOwner.date)));

        if (messageObject.caption != null){
            rowDataList.add(new RowData(rowCount++, getString(R.string.captionRow), messageObject.caption.toString()));
        }

        if (messageObject.messageOwner.from_scheduled){
            rowDataList.add(new RowData(rowCount++, getString(R.string.scheduleRow), String.valueOf(messageObject.messageOwner.from_scheduled)));
        }

        if (messageObject.messageOwner.silent){
            rowDataList.add(new RowData(rowCount++, getString(R.string.silenceRow), String.valueOf(messageObject.messageOwner.silent)));
        }

        if (TextUtils.isEmpty(messageObject.messageText)) {
            rowDataList.add(new RowData(rowCount++, getString(R.string.textRow), messageObject.messageText.toString()));
        }

        if (messageObject.messageOwner.edit_date != 0) {
            rowDataList.add(new RowData(rowCount++, getString(R.string.editDateRow), formatTime(messageObject.messageOwner.edit_date)));
        }

        if (messageObject.messageOwner.from_id != null) {
            var peer = messageObject.messageOwner.from_id;
            if (peer.channel_id != 0 || peer.chat_id != 0) {
                TLRPC.Chat fromChat = getMessagesController().getChat(peer.channel_id != 0 ? peer.channel_id : peer.chat_id);
                rowDataList.add(new RowData(rowCount++, getString(R.string.ownerIdRow), String.valueOf(fromChat.id)));
                rowDataList.add(new RowData(rowCount++, getString(R.string.ownerChannelRow), String.valueOf(fromChat.title)));

            } else if (peer.user_id != 0) {
                TLRPC.User fromUser = getMessagesController().getUser(peer.user_id);
                rowDataList.add(new RowData(rowCount++, getString(R.string.ownerIdRow), String.valueOf(fromUser.id)));
                String fullName = fromUser.first_name + (fromUser.last_name != null ? " " + fromUser.last_name : "") + (fromUser.username != null ? "\n" + fromUser.username : "");
                rowDataList.add(new RowData(rowCount++, getString(R.string.ownerRow), fullName, OPEN_PROFILE_ID, fromUser.id));
            }
        }

        if (messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.from_id != null) {
            forwardFromPeer = messageObject.messageOwner.fwd_from.from_id;
            TLRPC.User user = getMessagesController().getUser(forwardFromPeer.user_id);
            rowDataList.add(new RowData(rowCount++, getString(R.string.forwardRow), user.id + " " + user.username ));
        }

        if (MessageObject.getMedia(messageObject.messageOwner) != null && MessageObject.getMedia(messageObject.messageOwner).document != null) {
            for (var attribute : MessageObject.getMedia(messageObject.messageOwner).document.attributes) {
                if (attribute instanceof TLRPC.TL_documentAttributeFilename) {
                    String filename = attribute.file_name;
                    rowDataList.add(new RowData(rowCount++, getString(R.string.fileNameRow), filename));
                }
            }
        }

        filePath = messageObject.messageOwner.attachPath;
        if (!TextUtils.isEmpty(filePath)) {
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }

        if (TextUtils.isEmpty(filePath)) {
            filePath = getFileLoader().getPathToMessage(messageObject.messageOwner).toString();
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }
        if (TextUtils.isEmpty(filePath)) {
            filePath = getFileLoader().getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(filePath);
            if (!temp.isFile()) {
                filePath = null;
            }
        }
        if (filePath != null) {
            rowDataList.add(new RowData(rowCount++, getString(R.string.filePathRow), filePath));
        }

        var media = MessageObject.getMedia(messageObject.messageOwner);
        if (media != null) {
            long id = 0;
            if (media.photo != null && media.photo.id > 0) {
                id = media.photo.id;
            } else if (media.document != null && media.document.id > 0) {
                id = media.document.id;
            }
            if (id != 0){
                rowDataList.add(new RowData(rowCount++, getString(R.string.docIdRow), String.valueOf(id)));
            }
        }

        if (listAdapter != null && fullNotify) {
            listAdapter.notifyDataSetChanged();
        }

    }
    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
        updateRows(true);

        return true;
    }


    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        CacheControlActivity.canceled = true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(getString(R.string.MessageDetails));
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

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
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);
        listView.setOnItemClickListener((view, position, x, y) -> {
            RowData rowData = rowDataList.get(position);
            switch (rowData.action) {
                case OPEN_PROFILE_ID:
                    TLRPC.User user = getMessagesController().getUser(rowData.data);
                    if (user != null) {
                        openProfile(getMessagesController().getUser(rowData.data));
                    }
                    break;
                default:
                    AndroidUtilities.addToClipboard(rowData.value);
                    break;
            }
            AndroidUtilities.addToClipboard(rowDataList.get(position).value);
        });
        return fragmentView;
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        DownloadController.getInstance(currentAccount).checkAutodownloadSettings();
    }

    private void openProfile(TLRPC.User user) {
        Bundle args = new Bundle();
        args.putLong("user_id", user.id);
        ProfileActivity fragment = new ProfileActivity(args);
        fragment.setPlayProfileAnimation(0);
        presentFragment(fragment);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRows(false);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowDataList.size();
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextDetailSettingsCell settingsCell = (TextDetailSettingsCell) holder.itemView;
            settingsCell.setMultilineDetail(true);
            RowData data = rowDataList.get(position);
            settingsCell.setTextAndValue(data.title, data.value, false);
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 1:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            return 1;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
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
