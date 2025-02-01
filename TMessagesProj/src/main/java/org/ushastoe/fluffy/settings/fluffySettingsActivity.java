/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.ushastoe.fluffy.settings;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.*;
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
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.fluffyConfig;
import org.ushastoe.fluffy.helpers.WhisperHelper;

import java.io.File;
import java.util.ArrayList;

import static org.ushastoe.fluffy.fluffyConfig.writeCamera;

public class fluffySettingsActivity extends BaseFragment {

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    @SuppressWarnings("FieldCanBeLocal")
    private LinearLayoutManager layoutManager;

    private ArrayList<File> storageDirs;

    private int chatSettingsSectionRow;
    private int otherSettingsSectionRow;
    private int cameraSelectRow;
    private int localPremiumRow;
    private int voiceUseCloudflareRow;
    private int cfCredentialsRow;
    private int rowCount;

    private boolean updateCameraSelect;


    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
        updateRows(true);

        return true;
    }

    private void updateRows(boolean fullNotify) {

        rowCount = 0;
        chatSettingsSectionRow = rowCount++;
        voiceUseCloudflareRow = rowCount++;
        cfCredentialsRow = rowCount++;
        cameraSelectRow = rowCount++;
        otherSettingsSectionRow = rowCount++;
        localPremiumRow = rowCount++;

        if (listAdapter != null && fullNotify) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void rebind(int position) {
        if (listView == null || listAdapter == null) {
            return;
        }
        for (int i = 0; i < listView.getChildCount(); ++i) {
            View child = listView.getChildAt(i);
            RecyclerView.ViewHolder holder = listView.getChildViewHolder(child);
            if (holder != null && holder.getAdapterPosition() == position) {
                listAdapter.onBindViewHolder(holder, position);
                return;
            }
        }
    }

    private void rebindAll() {
        if (listView == null || listAdapter == null) {
            return;
        }
        for (int i = 0; i < listView.getChildCount(); ++i) {
            View child = listView.getChildAt(i);
            RecyclerView.ViewHolder holder = listView.getChildViewHolder(child);
            if (holder != null) {
                listAdapter.onBindViewHolder(holder, listView.getChildAdapterPosition(child));
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
        actionBar.setTitle(LocaleController.getString(R.string.fluffySettings));
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
            if (position == cameraSelectRow) {
                int selected;
                if (fluffyConfig.frontCamera) {
                    selected = 0;
                } else {
                    selected = 1;
                }
                Dialog dlg = AlertsCreator.createSingleChoiceDialog(getParentActivity(), new String[]{
                    LocaleController.getString(R.string.CameraFront),
                    LocaleController.getString(R.string.CameraBack)},
                    LocaleController.getString(R.string.SelectCamera), selected, (dialog, which) -> {
                        if (which == 0) {
                            fluffyConfig.frontCamera = true;
                        } else {
                            fluffyConfig.frontCamera = false;
                        }
                        updateCameraSelect = true;
                        writeCamera();

                        if (listAdapter != null) {
                            listAdapter.notifyItemChanged(position);
                        }

                        rebind(cameraSelectRow);
                });
                setVisibleDialog(dlg);
                dlg.show();
            } else if (position == localPremiumRow) {
                fluffyConfig.togglePremiumMode();
                TextCheckCell textCheckCell = (TextCheckCell) view;
                textCheckCell.setChecked(fluffyConfig.premiumMode);
            } else if (position == voiceUseCloudflareRow) {
                fluffyConfig.toggleVoiceUseCloudflare();
                TextCheckCell textCheckCell = (TextCheckCell) view;
                textCheckCell.setChecked(fluffyConfig.voiceUseCloudflare);
            } else if (position == cfCredentialsRow) {
                WhisperHelper.showCfCredentialsDialog(this);
            }
        });
        return fragmentView;
    }

    @Override
    protected void onDialogDismiss(Dialog dialog) {
        DownloadController.getInstance(currentAccount).checkAutodownloadSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        rebindAll();
        updateRows(false);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 0: {
                    holder.itemView.setBackgroundDrawable(Theme.getThemedDrawableByKey(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
                case 6: {
                    TextCell textCell = (TextCell) holder.itemView;
                    break;
                }
                case 1: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setCanDisable(false);
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == cameraSelectRow) {
                        textCell.setIcon(0);
                        String value = null;
                        if (fluffyConfig.frontCamera) {
                            value = LocaleController.getString(R.string.CameraFront);
                        } else {
                            value = LocaleController.getString(R.string.CameraBack);
                        }
                        textCell.setTextAndValue(LocaleController.getString(R.string.SelectCamera), value, updateCameraSelect, true);
                        updateCameraSelect = false;
                    } else if (position == cfCredentialsRow) {
                        textCell.setIcon(0);
                        textCell.setText(LocaleController.getString(R.string.CloudflareCredentials), true);
                    }
                    break;
                }
                case 2: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == chatSettingsSectionRow) {
                        headerCell.setText(LocaleController.getString(R.string.ChatTweak));
                    } else if (position == otherSettingsSectionRow) {
                        headerCell.setText(LocaleController.getString(R.string.Other));
                    }
                    break;
                }
                case 3: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == localPremiumRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.LocalPremium), fluffyConfig.premiumMode, true);
                    } else if (position == voiceUseCloudflareRow) {
                        checkCell.setTextAndCheck(LocaleController.getString(R.string.UseCloudflare), fluffyConfig.voiceUseCloudflare, true);
                    }
                    break;
                }
                case 4: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    break;
                }
                case 5: {
                    NotificationsCheckCell checkCell = (NotificationsCheckCell) holder.itemView;
                    break;
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            int viewType = holder.getItemViewType();
            if (viewType == 3) {
                TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                int position = holder.getAdapterPosition();
                if (position == localPremiumRow) {
                    checkCell.setChecked(fluffyConfig.premiumMode);
                } else if (position == voiceUseCloudflareRow) {
                    checkCell.setChecked(fluffyConfig.voiceUseCloudflare);
                }
            }
        }

        public boolean isRowEnabled(int position) {
            return position == chatSettingsSectionRow || position == otherSettingsSectionRow || position == localPremiumRow || position == voiceUseCloudflareRow;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return isRowEnabled(holder.getAdapterPosition());
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 1:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new HeaderCell(mContext, 22);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackgroundDrawable(Theme.getThemedDrawableByKey(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 5:
                    view = new NotificationsCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 6:
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
            if (position == chatSettingsSectionRow || position == otherSettingsSectionRow) {
                return 2;
            } else if (position == localPremiumRow || position == voiceUseCloudflareRow) {
                return 3;
            } else {
                return 1;
            }
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
