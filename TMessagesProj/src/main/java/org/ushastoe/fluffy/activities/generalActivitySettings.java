package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
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
import org.telegram.ui.LaunchActivity;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.activities.elements.actionBarSettingsCell;
import org.ushastoe.fluffy.activities.elements.headerSettingsCell;
import org.ushastoe.fluffy.fluffyConfig;
import org.ushastoe.fluffy.helpers.WhisperHelper;
import org.ushastoe.fluffy.settings.fluffySettingsActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class generalActivitySettings extends BaseFragment {
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private int rowCount;
    private LinearLayoutManager layoutManager;

    private headerSettingsCell headerSettingsCell;
    private View actionBarBackground;
    private AnimatorSet actionBarAnimator;

    private int[] location = new int[2];

    private int divider;
    private int divider2;
    private int mainRow;
    private int voiceRecognitionRow;
    private int voiceRecognitionSelecterRow;
    private int downloadSpeedBoostRow;
    private int saveEditRow;
    private int saveDelRow;
    private int chatRow;
    private int unmuteVideoWithVolumeRow;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
        updateRows(true);

        return true;
    }

    private void updateRows(boolean fullNotify) {
        rowCount = 0;

        mainRow = rowCount++;
        downloadSpeedBoostRow = rowCount++;
        saveEditRow = rowCount++;
        saveDelRow = rowCount++;
        unmuteVideoWithVolumeRow = rowCount++;
        divider2 = rowCount++;
        chatRow = rowCount++;
        voiceRecognitionSelecterRow = rowCount++;
        voiceRecognitionRow = rowCount++;

        if (listAdapter != null && fullNotify) {
            listAdapter.notifyDataSetChanged();
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
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == mainRow || position == divider2 || position == chatRow) {
                return;
            }
            TextCell textCell = (TextCell) view;
            if (position == downloadSpeedBoostRow) {
                fluffyConfig.toogleDownloadSpeedBoost();
                textCell.setChecked(fluffyConfig.downloadSpeedBoost);
            } else if (position == saveEditRow) {
                fluffyConfig.saveEditSwitch();
                textCell.setChecked(fluffyConfig.saveEdit);
            } else if (position == saveDelRow) {
                fluffyConfig.saveDelSwitch();
                textCell.setChecked(fluffyConfig.saveDel);
            } else if (position == unmuteVideoWithVolumeRow) {
                fluffyConfig.toggleUnmuteVideoWithVolume();
                textCell.setChecked(fluffyConfig.unmuteVideoWithVolume);
            } else if (position == voiceRecognitionRow) {
                WhisperHelper.showCfCredentialsDialog(this);
            } else if (position == voiceRecognitionSelecterRow) {
                selectProvider(context);
                textCell.setTextAndValueAndIcon(getString(R.string.UseCloudflare), fluffyConfig.useCloudFlare() ? "Cloudflare" : "Telegram", R.drawable.voicechat_muted, true);
            }
        });
        return fragmentView;
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
                RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(voiceRecognitionSelecterRow);
                if (holder != null) {
                    listAdapter.onBindViewHolder(holder, voiceRecognitionSelecterRow);
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
                // HeaderCell
                case 0:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 2:
                    TextCell textCellc = (TextCell) holder.itemView;
                    if (position == voiceRecognitionRow) {
                        textCellc.setTextAndIcon(getString(R.string.CloudflareCredentials), R.drawable.msg_voicechat_solar, true);
                    } else if (position == voiceRecognitionSelecterRow) {
                        textCellc.setTextAndValueAndIcon(getString(R.string.UseCloudflare), fluffyConfig.useCloudFlare() ? "Cloudflare" : "Telegram", R.drawable.voicechat_muted, true);
                    }
                    break;
                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == mainRow) {
                        headerCell.setText(getString(R.string.General));
                    } else if (position == chatRow) {
                        headerCell.setText(getString(R.string.Chats));
                    }
                    break;
                case 1:
                default:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setEnabled(true);
                    if (position == downloadSpeedBoostRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.downloadSpeedBoost), fluffyConfig.downloadSpeedBoost, R.drawable.msg_download, true);
                    } else if (position == saveEditRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.saveEditRow), fluffyConfig.saveEdit, R.drawable.msg_edit, true);
                    } else if (position == saveDelRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.saveDelRow), fluffyConfig.saveDel, R.drawable.msg_delete, true);
                    } else if (position == unmuteVideoWithVolumeRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.unmuteVideoWithVolume), fluffyConfig.unmuteVideoWithVolume, R.drawable.media_unmute, true);
                    }
                    break;
                }
    }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case 1:
                default:
                    view = new TextCell(mContext, 0, false, true, null);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }


        @Override
        public int getItemViewType(int position) {
            Set<Integer> settingsRows = new HashSet<>(Arrays.asList(
                downloadSpeedBoostRow, saveEditRow, saveDelRow, unmuteVideoWithVolumeRow
            ));
            Set<Integer> settingsWithCheckRows = new HashSet<>(Arrays.asList(
                voiceRecognitionRow, voiceRecognitionSelecterRow
            ));
            Set<Integer> headersRows = new HashSet<>(Arrays.asList(
                mainRow, chatRow
            ));

            if (settingsRows.contains(position)) {
                return 1;
            }

            if (settingsWithCheckRows.contains(position)) {
                return 2;
            }
            if (headersRows.contains(position)) {
                return 3;
            }
            return 0;
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