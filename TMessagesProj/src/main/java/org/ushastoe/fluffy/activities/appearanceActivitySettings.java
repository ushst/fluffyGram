package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;

import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.AppIconsSelectorCell;
import org.telegram.ui.Cells.BrightnessControlCell;
import org.telegram.ui.Cells.ChatListCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.ThemePreviewMessagesCell;
import org.telegram.ui.Cells.ThemeTypeCell;
import org.telegram.ui.Cells.ThemesHorizontalListCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.ui.DefaultThemesPreviewCell;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PeerColorActivity;
import org.telegram.ui.ThemeActivity;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.activities.elements.ChatListPreviewCell;
import org.ushastoe.fluffy.activities.elements.DoubleTapCell;
import org.ushastoe.fluffy.activities.elements.headerSettingsCell;
import org.ushastoe.fluffy.fluffyConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class appearanceActivitySettings extends BaseFragment {
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private int rowCount;
    private LinearLayoutManager layoutManager;

    ChatListPreviewCell chatListPreviewCell;
    DoubleTapCell doubleTapCell;

    private View actionBarBackground;
    private AnimatorSet actionBarAnimator;

    private int[] location = new int[2];

    private int divider2;
    private int divider;
    private int mainRow;
    private int generalRow;
    private int zodiacShowRow;
    private int storiesShowRow;
    private int chatRow;
    private int callShowRow;
    private int chatListPreviewRow;

    private int centerTitleRow;
    private int disableRoundRow;
    private int selectTitleRow;
    private int moreInfoRow;
    private int systemTypefaceRow;
    private int useSolarIconsRow;
    private int formatTimeWithSecondsRow;
    private int doubleTapRow;


    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
        updateRows(true);

        return true;
    }

    private void updateRows(boolean fullNotify) {
        rowCount = 0;
//
        generalRow = rowCount++;
        chatListPreviewRow = rowCount++;
        centerTitleRow = rowCount++;
        storiesShowRow = rowCount++;
        selectTitleRow = rowCount++;
        systemTypefaceRow = rowCount++;
        useSolarIconsRow = rowCount++;
        divider = rowCount++;
        mainRow = rowCount++;
        zodiacShowRow = rowCount++;
        divider2 = rowCount++;
        chatRow = rowCount++;
        doubleTapRow = rowCount++;
        disableRoundRow = rowCount++;
        callShowRow = rowCount++;
        moreInfoRow = rowCount++;
        formatTimeWithSecondsRow = rowCount++;

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
        actionBar.setTitle(getString(R.string.Appearance));
        actionBar.setAllowOverlayTitle(true);
        actionBar.createMenu().addItem(1000, (Drawable) null).setVisibility(View.INVISIBLE);
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
            if (position == zodiacShowRow) {
                fluffyConfig.toogleZodiacShow();
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.zodiacShow);
            } else if (position == storiesShowRow) {
                fluffyConfig.toggleShowStories();
                chatListPreviewCell.updateStories(true);
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.showStories);
            } else if (position == callShowRow) {
                fluffyConfig.toggleShowCallIcon();
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.showCallIcon);
            } else if (position == centerTitleRow) {
                fluffyConfig.toggleCenterTitle();
                chatListPreviewCell.updateCenteredTitle(true);
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.centerTitle);
            } else if (position == disableRoundRow) {
                fluffyConfig.toogleRoundingNumber();
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.disableRoundingNumber);
            } else if (position == moreInfoRow) {
                fluffyConfig.toggleMoreInfoOnline();
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.moreInfoOnline);
            } else if (position == systemTypefaceRow) {
                fluffyConfig.toogleUseSystemFonts();
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.useSystemFonts);
            } else if (position == useSolarIconsRow) {
                fluffyConfig.toggleUseSolarIcons();
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.useSolarIcons);
            } else if (position == formatTimeWithSecondsRow) {
                fluffyConfig.toogleFormatTimeWithSeconds();
                TextCell textCell = (TextCell) view;
                textCell.setChecked(fluffyConfig.formatTimeWithSeconds);
            } else if (position == selectTitleRow) {
                titleSelecter(context);
            } else if (position == doubleTapRow) {
                selectorReaction();
            }
        });
        return fragmentView;
    }
    
    private void selectorReaction () {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        arrayList.add(LocaleController.getString(R.string.Disable));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_NONE);
        arrayList.add(LocaleController.getString(R.string.Reactions));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_REACTION);
        arrayList.add(LocaleController.getString(R.string.Reply));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_REPLY);
        arrayList.add(LocaleController.getString(R.string.Copy));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_COPY);
        arrayList.add(LocaleController.getString(R.string.Forward));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_FORWARD);
        arrayList.add(LocaleController.getString(R.string.Edit));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_EDIT);
        arrayList.add(LocaleController.getString(R.string.Save));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_SAVE);
        arrayList.add(LocaleController.getString(R.string.Delete));
        types.add(fluffyConfig.DOUBLE_TAP_ACTION_DELETE);

        var context = getParentActivity();
        var builder = new AlertDialog.Builder(context);

        var linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setView(linearLayout);

        DoubleTapCell previewCell = new DoubleTapCell(context);
        linearLayout.addView(previewCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        var hLayout = new LinearLayout(context);
        hLayout.setOrientation(LinearLayout.HORIZONTAL);
        hLayout.setPadding(0, AndroidUtilities.dp(8), 0, 0);
        linearLayout.addView(hLayout);

        for (int i = 0; i < 2; i++) {
            var out = i == 1;
            var layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            hLayout.addView(layout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, .5f));

            for (int a = 0; a < arrayList.size(); a++) {

                var cell = new RadioColorCell(context);
                cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                cell.setTag(a);
                cell.setTextAndValue(arrayList.get(a), a == types.indexOf(out ? fluffyConfig.doubleTapOutAction : fluffyConfig.doubleTapInAction));
                cell.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector), out ? AndroidUtilities.dp(6) : 0, out ? 0 : AndroidUtilities.dp(6), out ? 0 : AndroidUtilities.dp(6), out ? AndroidUtilities.dp(6) : 0));
                layout.addView(cell);
                cell.setOnClickListener(v -> {
                    var which = (Integer) v.getTag();
                    var old = out ? fluffyConfig.doubleTapOutAction : fluffyConfig.doubleTapInAction;
                    if (types.get(which) == old) {
                        return;
                    }
                    if (out) {
                        fluffyConfig.setDoubleTapOutAction(types.get(which));
                    } else {
                        fluffyConfig.setDoubleTapInAction(types.get(which));
                    }
                    ((RadioColorCell) layout.getChildAt(types.indexOf(old))).setChecked(false, true);
                    cell.setChecked(true, true);
                    doubleTapCell.updateIcons(out ? 2 : 1, true);
                    previewCell.updateIcons(out ? 2 : 1 , true);
                });
            }
        }
        builder.setNegativeButton(LocaleController.getString(R.string.OK), null);
        builder.show();
    }
    private void titleSelecter(Context context) {
        if (getParentActivity() == null) {
            return;
        }
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        CharSequence[] items = new CharSequence[]{
                fluffyConfig.getUsername(),
                "fluffy",
                "telegram",
                "Disable"
        };

        for (int i = 0; i < items.length; ++i) {
            final int index = i;
            RadioColorCell cell = new RadioColorCell(getParentActivity());
            cell.setPadding(dp(4), 0, dp(4), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(items[index], index == fluffyConfig.typeTitle);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                fluffyConfig.setTypeTitle(index);
                getNotificationCenter().postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
                RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(selectTitleRow);
                if (holder != null) {
                    listAdapter.onBindViewHolder(holder, selectTitleRow);
                }

                if (LaunchActivity.getSafeLastFragment() != null) {
                    BulletinHelper.showRestartNotification(LaunchActivity.getSafeLastFragment());
                }
                getNotificationCenter().postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
                dialogRef.get().dismiss();
                chatListPreviewCell.updateTitle(true);
            });
        }

        Dialog dialog = new AlertDialog.Builder(getParentActivity())
                .setTitle(getString(R.string.TitleSelecter))
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
                    NotificationsCheckCell checkCell = (NotificationsCheckCell) holder.itemView;
                    checkCell.setTextAndValueAndIconAndCheck(getString(R.string.InappBrowser), getString(R.string.InappBrowserInfo), R.drawable.msg2_language, SharedConfig.inappBrowser, 0, false, true);
                    break;

                case 3:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == mainRow) {
                        headerCell.setText(getString(R.string.Profile));
                    } else if (position == chatRow) {
                        headerCell.setText(getString(R.string.Chats));
                    } else if (position == generalRow) {
                        headerCell.setText(getString(R.string.General));
                    }
                    break;
                case 4:
                    TextInfoPrivacyCell textInfoPrivacyCell = (TextInfoPrivacyCell) holder.itemView;
                    textInfoPrivacyCell.setText("(4.8K -> 4777)");
                    break;
                case 5:
                    chatListPreviewCell = (ChatListPreviewCell) holder.itemView;
                    break;
                case 6:
                    TextCell textCell6 = (TextCell) holder.itemView;
                    if (position == selectTitleRow) {
                        String value = switch (fluffyConfig.typeTitle) {
                            case 0 -> fluffyConfig.getUsername();
                            case 1 -> "fluffy";
                            case 2 -> "telegram";
                            case 3 -> "Disable";
                            default -> LocaleController.getString(R.string.AppName);
                        };
                        textCell6.setTextAndValueAndIcon(getString("TitleType", R.string.TitleSelecter), value, R.drawable.menu_tag_rename, true);
                    }
                    break;
                case 7:
                    doubleTapCell = (DoubleTapCell) holder.itemView;
                    break;
                case 1:
                default:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setEnabled(true);
                    if (position == zodiacShowRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.zodiacShow), fluffyConfig.zodiacShow, R.drawable.msg_calendar2, true);
                    } else if (position == storiesShowRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.storiesShower), fluffyConfig.showStories, R.drawable.menu_feature_stories, true);
                    } else if (position == callShowRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.callShower), fluffyConfig.showCallIcon, R.drawable.calls_menu_phone, true);
                    } else if (position == centerTitleRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.centerTitle), fluffyConfig.centerTitle, R.drawable.msg_contacts_name, true);
                    } else if (position == systemTypefaceRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.UseSystemTypeface), fluffyConfig.useSystemFonts, R.drawable.msg_photo_text_framed, true);
                    } else if (position == useSolarIconsRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.useSolarIcons), fluffyConfig.useSolarIcons, R.drawable.media_magic_cut, true);
                    } else if (position == disableRoundRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.DisableNumberRounding), fluffyConfig.disableRoundingNumber, R.drawable.msg_archive_show, true);
                        textCell.setSubtitle("4.8K -> 4777");
                    } else if (position == moreInfoRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.ExtendedStatusOnline), fluffyConfig.disableRoundingNumber, R.drawable.msg_contacts_time, true);
                        textCell.setSubtitle("last seen at * PM -> * (2h 13m)");
                    } else if (position == formatTimeWithSecondsRow) {
                        textCell.setTextAndCheckAndIcon(getString(R.string.formatTime), fluffyConfig.formatTimeWithSeconds, R.drawable.menu_premium_clock, true);
                        textCell.setSubtitle("12:34 PM -> 12:34:56 PM");
                    }
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
                    view = new NotificationsCheckCell(mContext, 21, 60, true);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 5:
                    view = new ChatListPreviewCell(mContext);
                    break;
                case 6:
                    view = new TextCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 7:
                    view = new DoubleTapCell(mContext);
//                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
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
                    zodiacShowRow, storiesShowRow, callShowRow,
                    centerTitleRow, systemTypefaceRow, disableRoundRow,
                    moreInfoRow, useSolarIconsRow, formatTimeWithSecondsRow

            ));

            Set<Integer> privacyRows = new HashSet<>(Arrays.asList(

            ));

            Set<Integer> headersRows = new HashSet<>(Arrays.asList(
                mainRow, chatRow, generalRow
            ));
            if (position == chatListPreviewRow) {
                return 5;
            }
            if (settingsRows.contains(position)) {
                return 1;
            }
            if (privacyRows.contains(position)) {
                return 4;
            }
            if (headersRows.contains(position)) {
                return 3;
            }
            if (position == selectTitleRow) {
                return 6;
            }
            if (position == doubleTapRow) {
                return 7;
            }
            return 0;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, BrightnessControlCell.class, ThemeTypeCell.class, ChatListCell.class, NotificationsCheckCell.class, ThemesHorizontalListCell.class, TextCell.class, PeerColorActivity.ChangeNameColorCell.class, SwipeGestureSettingsView.class, DefaultThemesPreviewCell.class, AppIconsSelectorCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
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