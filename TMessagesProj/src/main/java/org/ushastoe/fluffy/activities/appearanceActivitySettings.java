package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.LaunchActivity.getLastFragment;
import static org.ushastoe.fluffy.BulletinHelper.showRestartNotification;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.documentfile.provider.DocumentFile;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.AppIconsSelectorCell;
import org.telegram.ui.Cells.BrightnessControlCell;
import org.telegram.ui.Cells.ChatListCell;
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
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.ui.Components.MotionBackgroundDrawable;
import org.telegram.ui.DefaultThemesPreviewCell;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PeerColorActivity;
import org.telegram.ui.SelectAnimatedEmojiDialog;
import org.telegram.ui.ThemeActivity;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.activities.elements.ChatListPreviewCell;
import org.ushastoe.fluffy.activities.elements.DoubleTapCell;
import org.ushastoe.fluffy.activities.elements.FluffyDialogUtils;
import org.ushastoe.fluffy.activities.elements.StickerSizePreviewMessagesCell;
import org.ushastoe.fluffy.fluffyConfig;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class appearanceActivitySettings extends BaseFragment {
    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private LinearLayoutManager layoutManager;

    private ChatListPreviewCell chatListPreviewCell;
    private DoubleTapCell doubleTapCell;
    private SetDefaultReactionCell setDefaultReactionCell;
    private StickerSizePreviewMessagesCell stickerSizePreview;
    private String TAG = "fluffy";

    private enum RowType {
        HERO_CARD,
        INFO_BLOCK,
        HEADER,
        TEXT_CHECK,
        TEXT_CELL,
        TEXT_INFO_PRIVACY,
        SHADOW_SECTION,
        CHAT_LIST_PREVIEW,
        DOUBLE_TAP_CELL,
        QUICK_SWITCHER,
        STICKER_SIZE_PREVIEW,
        STICKER_SIZE_SEEKBAR,
        STICKER_RADIUS_SEEKBAR,
        NOTIFICATIONS_CHECK
    }

    private enum RowIdentifier {
        APPEARANCE_HERO,
        APPEARANCE_INFO,
        CHAT_LIST_PREVIEW,
        CENTER_TITLE,
        CENTER_TITLE_IN_CHAT,
        STORIES_SHOW,
        SHOW_DIVIDER,
        SELECT_TITLE,
        SELECT_CUSTOM_FONT,
        SYSTEM_TYPEFACE,
        USE_SOLAR_ICONS,
        NEW_SWITCH_STYLE,
        CUSTOM_FONT_HINT,
        GENERAL_LAYOUT_HEADER,
        GENERAL_TYPOGRAPHY_HEADER,
        PROFILE_VISIBILITY_HEADER,
        ZODIAC_SHOW,
        CHAT_INDICATORS_HEADER,
        CHAT_CONTROLS_HEADER,
        CHAT_INPUT_HEADER,
        STICKER_SIZE,
        STICKER_SIZE_SEEKBAR,
        STICKER_RADIUS_SEEKBAR,
        STICKER_APPEARANCE_HEADER,
        STICKER_MANAGEMENT_HEADER,
        DISABLE_ROUND,
        CALL_SHOW,
        MORE_INFO,
        FORMAT_TIME_WITH_SECONDS,
        STICKER_TIME_STAMP,
        TRANSPARENCY,
        REMOVE_GIFTS,
        HIDE_PAID_REACTIONS,
        REMOVE_BUTTON,
        FORCE_CHAT_SNOW,
        STICKER_BLACKLIST,
        DOUBLE_TAP,
        QUICK_SWITCHER,
        MENU_CUSTOMIZATION,
        HIDE_BIZ_BOT_BAR,
        EMOJI_LONGPRESS_MENU,
        GESTURE_ACTIONS_HEADER,
        GESTURE_SHORTCUTS_HEADER,
        SECTION_DIVIDER,
        ONLINE_STATUS_RING
    }
    private static class Row {
        RowType type;
        RowIdentifier id;
        int textResId;
        int iconResId;
        int subtitleResId;
        CharSequence customText;

        Row(RowIdentifier id, RowType type, int textResId, int iconResId) {
            this.id = id;
            this.type = type;
            this.textResId = textResId;
            this.iconResId = iconResId;
        }

        Row(RowIdentifier id, RowType type, int textResId, int iconResId, int subtitleResId) {
            this(id, type, textResId, iconResId);
            this.subtitleResId = subtitleResId;
        }

        Row(RowIdentifier id, RowType type, int textResId) {
            this(id, type, textResId, 0);
        }

        Row(RowIdentifier id, RowType type) {
            this(id, type, 0, 0);
        }

        Row(RowIdentifier id, RowType type, CharSequence customText) {
            this(id, type);
            this.customText = customText;
        }

        static Row createHero(CharSequence subtitle, int iconResId) {
            Row row = new Row(RowIdentifier.APPEARANCE_HERO, RowType.HERO_CARD, R.string.Appearance, iconResId);
            row.customText = subtitle;
            return row;
        }

        static Row createInfo(CharSequence text) {
            Row row = new Row(RowIdentifier.APPEARANCE_INFO, RowType.INFO_BLOCK);
            row.customText = text;
            return row;
        }
    }

    private List<Row> rows = new ArrayList<>();
    private static final int stickerRaduisMax = 130;
    private static final int REQUEST_CODE_IMPORT_CUSTOM_FONT = 2010;
    private Parcelable recyclerViewState = null;

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

        rows.add(Row.createHero(LocaleController.getString("AppearanceHeroSubtitle", R.string.AppearanceHeroSubtitle), R.drawable.msg_theme));

        buildGeneralSection();
        buildProfileSection();
        buildStickerSection();
        buildGesturesSection();
        buildChatSection();

        if (!rows.isEmpty() && rows.get(rows.size() - 1).type == RowType.SHADOW_SECTION) {
            rows.remove(rows.size() - 1);
        }
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
            if (recyclerViewState != null) {
                layoutManager.onRestoreInstanceState(recyclerViewState);
            }
        }
    }

    private void buildGeneralSection() {
        addHeaderRow(new Row(RowIdentifier.GENERAL_LAYOUT_HEADER, RowType.HEADER, "Layout"));
        List<Row> layoutRows = new ArrayList<>();
        layoutRows.add(new Row(RowIdentifier.CENTER_TITLE, RowType.TEXT_CHECK, R.string.centerTitle, R.drawable.msg_contacts_name));
        layoutRows.add(new Row(RowIdentifier.CENTER_TITLE_IN_CHAT, RowType.TEXT_CHECK, R.string.centerTitleInChat, R.drawable.msg_contacts_name));
        layoutRows.add(new Row(RowIdentifier.STORIES_SHOW, RowType.TEXT_CHECK, R.string.storiesShower, R.drawable.menu_feature_stories));
        layoutRows.add(new Row(RowIdentifier.SHOW_DIVIDER, RowType.TEXT_CHECK, R.string.dividerShower, R.drawable.ic_colorpicker_solar));
        sortRows(layoutRows);
        rows.add(new Row(RowIdentifier.CHAT_LIST_PREVIEW, RowType.CHAT_LIST_PREVIEW));
        rows.addAll(layoutRows);

        addHeaderRow(new Row(RowIdentifier.GENERAL_TYPOGRAPHY_HEADER, RowType.HEADER, "Typography"));
        List<Row> typographyRows = new ArrayList<>();
        typographyRows.add(new Row(RowIdentifier.SELECT_TITLE, RowType.TEXT_CELL, R.string.TitleSelecter, R.drawable.menu_tag_rename));
        typographyRows.add(new Row(RowIdentifier.SYSTEM_TYPEFACE, RowType.TEXT_CHECK, R.string.UseSystemTypeface, R.drawable.msg_photo_text_framed));
        typographyRows.add(new Row(RowIdentifier.USE_SOLAR_ICONS, RowType.TEXT_CHECK, R.string.useSolarIcons, R.drawable.media_magic_cut));
        typographyRows.add(new Row(RowIdentifier.NEW_SWITCH_STYLE, RowType.TEXT_CHECK, R.string.NewMaterialSwith, R.drawable.msg_photo_switch2));
        sortRows(typographyRows);
        if (!fluffyConfig.useSystemFonts) {
            int systemIndex = indexOfRow(typographyRows, RowIdentifier.SYSTEM_TYPEFACE);
            Row customFontRow = new Row(RowIdentifier.SELECT_CUSTOM_FONT, RowType.TEXT_CELL, R.string.SelectCustomFont, R.drawable.msg_photo_text_framed);
            Row hintRow = new Row(RowIdentifier.CUSTOM_FONT_HINT, RowType.TEXT_INFO_PRIVACY,
                    LocaleController.formatString("CustomFontHint", R.string.CustomFontHint,
                            fluffyConfig.getFontsDirectory().getAbsolutePath()));
            if (systemIndex == -1) {
                typographyRows.add(customFontRow);
                typographyRows.add(hintRow);
            } else {
                typographyRows.add(systemIndex + 1, customFontRow);
                typographyRows.add(systemIndex + 2, hintRow);
            }
        }
        rows.addAll(typographyRows);
    }

    private void buildProfileSection() {
        addHeaderRow(new Row(RowIdentifier.PROFILE_VISIBILITY_HEADER, RowType.HEADER, "Visibility"));
        rows.add(new Row(RowIdentifier.ZODIAC_SHOW, RowType.TEXT_CHECK, R.string.zodiacShow, R.drawable.msg_calendar2));
    }

    private void buildStickerSection() {
        addHeaderRow(new Row(RowIdentifier.STICKER_APPEARANCE_HEADER, RowType.HEADER, "Appearance"));
        rows.add(new Row(RowIdentifier.STICKER_SIZE_SEEKBAR, RowType.STICKER_SIZE_SEEKBAR));
        rows.add(new Row(RowIdentifier.STICKER_RADIUS_SEEKBAR, RowType.STICKER_RADIUS_SEEKBAR));
        rows.add(new Row(RowIdentifier.STICKER_SIZE, RowType.STICKER_SIZE_PREVIEW));

        addHeaderRow(new Row(RowIdentifier.STICKER_MANAGEMENT_HEADER, RowType.HEADER, "Preferences"));
        List<Row> stickerRows = new ArrayList<>();
        stickerRows.add(new Row(RowIdentifier.STICKER_TIME_STAMP, RowType.TEXT_CELL, R.string.TimestampSelecter, R.drawable.msg2_sticker));
        stickerRows.add(new Row(RowIdentifier.STICKER_BLACKLIST, RowType.TEXT_CELL, R.string.StickerBlacklist, R.drawable.msg_block));
        sortRows(stickerRows);
        rows.addAll(stickerRows);
    }

    private void buildGesturesSection() {
        List<Row> interactionRows = new ArrayList<>();
        interactionRows.add(new Row(RowIdentifier.DOUBLE_TAP, RowType.DOUBLE_TAP_CELL));
        interactionRows.add(new Row(RowIdentifier.QUICK_SWITCHER, RowType.QUICK_SWITCHER));
        addSubcategory(RowIdentifier.GESTURE_ACTIONS_HEADER, "Interactions", interactionRows, false);

        List<Row> shortcutRows = new ArrayList<>();
        shortcutRows.add(new Row(RowIdentifier.MENU_CUSTOMIZATION, RowType.TEXT_CELL, R.string.ContextMenuSettings, R.drawable.msg_settings));
        addSubcategory(RowIdentifier.GESTURE_SHORTCUTS_HEADER, "Shortcuts", shortcutRows, false);
    }

    private void buildChatSection() {
        List<Row> indicatorRows = new ArrayList<>();
        indicatorRows.add(new Row(RowIdentifier.DISABLE_ROUND, RowType.TEXT_CHECK, R.string.DisableNumberRounding, R.drawable.msg_archive_show, R.string.DisableNumberRoundingSubtitle));
        indicatorRows.add(new Row(RowIdentifier.MORE_INFO, RowType.TEXT_CHECK, R.string.ExtendedStatusOnline, R.drawable.msg_contacts_time, R.string.ExtendedStatusOnlineSubtitle));
        indicatorRows.add(new Row(RowIdentifier.ONLINE_STATUS_RING, RowType.TEXT_CHECK, R.string.OnlineRingIndicator, R.drawable.msg_contacts_time, R.string.OnlineRingIndicatorSubtitle));
        indicatorRows.add(new Row(RowIdentifier.FORMAT_TIME_WITH_SECONDS, RowType.TEXT_CHECK, R.string.formatTime, R.drawable.menu_premium_clock, R.string.formatTimeSubtitle));
        addSubcategory(RowIdentifier.CHAT_INDICATORS_HEADER, "Indicators", indicatorRows, true);

        List<Row> controlRows = new ArrayList<>();
        controlRows.add(new Row(RowIdentifier.CALL_SHOW, RowType.TEXT_CHECK, R.string.callShower, R.drawable.calls_menu_phone));
        controlRows.add(new Row(RowIdentifier.TRANSPARENCY, RowType.TEXT_CELL, R.string.Transparency, R.drawable.msg_blur_radial));
        controlRows.add(new Row(RowIdentifier.REMOVE_BUTTON, RowType.TEXT_CHECK, R.string.HideFloatingButton, R.drawable.msg_openin));
        controlRows.add(new Row(RowIdentifier.FORCE_CHAT_SNOW, RowType.TEXT_CHECK, R.string.AlwaysSnowflakes, R.drawable.msg_theme, R.string.AlwaysSnowflakesSubtitle));
        controlRows.add(new Row(RowIdentifier.HIDE_BIZ_BOT_BAR, RowType.TEXT_CHECK, R.string.HideThisBar, R.drawable.msg_cancel));
        addSubcategory(RowIdentifier.CHAT_CONTROLS_HEADER, "Controls", controlRows, true);

        List<Row> inputRows = new ArrayList<>();
        inputRows.add(new Row(RowIdentifier.REMOVE_GIFTS, RowType.TEXT_CHECK, R.string.HideGiftFromInput, R.drawable.filled_gift_simple));
        inputRows.add(new Row(RowIdentifier.HIDE_PAID_REACTIONS, RowType.TEXT_CHECK, R.string.HidePaidReactionsButton, R.drawable.star_reaction));
        inputRows.add(new Row(RowIdentifier.EMOJI_LONGPRESS_MENU, RowType.TEXT_CHECK, R.string.EmojiButtonLongPressMenu, R.drawable.msg_spoiler, R.string.EmojiButtonLongPressMenuSubtitle));
        addSubcategory(RowIdentifier.CHAT_INPUT_HEADER, "Input", inputRows, true);
    }

    private void addSubcategory(RowIdentifier headerId, CharSequence title, List<Row> entries, boolean sortEntries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        addHeaderRow(new Row(headerId, RowType.HEADER, title));
        if (sortEntries) {
            sortRows(entries);
        }
        rows.addAll(entries);
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

    private int indexOfRow(List<Row> rowsList, RowIdentifier id) {
        for (int i = 0; i < rowsList.size(); i++) {
            if (rowsList.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    private void addHeaderRow(Row headerRow) {
        addDividerRow();
        rows.add(headerRow);
    }

    private void addDividerRow() {
        if (!rows.isEmpty()) {
            Row lastRow = rows.get(rows.size() - 1);
            if (lastRow.type != RowType.SHADOW_SECTION && lastRow.type != RowType.HERO_CARD) {
                rows.add(new Row(RowIdentifier.SECTION_DIVIDER, RowType.SHADOW_SECTION));
            }
        }
    }

    private void showMenuItemConfigurator(Context context) {
        if (getParentActivity() == null) {
            return;
        }

        class MenuItemConfig {
            final String title;
            final Runnable onToggle;
            final BooleanSupplier isChecked;

            @FunctionalInterface
            interface BooleanSupplier { boolean get(); }
            @FunctionalInterface
            interface Runnable { void run(); }

            MenuItemConfig(String title, BooleanSupplier isChecked, Runnable onToggle) {
                this.title = title;
                this.isChecked = isChecked;
                this.onToggle = onToggle;
            }
        }

        List<MenuItemConfig> menuItems = new ArrayList<>();

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.copy_photo),
                () -> fluffyConfig.showCopyPhoto,
                fluffyConfig::toggleShowCopyPhoto
        ));


        menuItems.add(new MenuItemConfig(
                context.getString(R.string.forward_wo_author),
                () -> fluffyConfig.showForwardWoAuthorship,
                fluffyConfig::toggleShowForwardWoAuthorship
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.view_user_history),
                () -> fluffyConfig.showViewMessageFromUser,
                fluffyConfig::toggleShowViewMessageFromUser
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.json),
                () -> fluffyConfig.showJSON,
                fluffyConfig::toggleShowJSON
        ));


        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        for (int i = 0; i < menuItems.size(); i++) {
            MenuItemConfig item = menuItems.get(i);
            TextCheckCell cell = new TextCheckCell(context);

            cell.setTextAndCheck(item.title, item.isChecked.get(), i < menuItems.size() - 1);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));

            cell.setOnClickListener(v -> {
                item.onToggle.run();
                cell.setChecked(item.isChecked.get());
            });

            linearLayout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        AlertDialog dialog = FluffyDialogUtils.themedBuilder(getParentActivity())
                .setTitle(getString(R.string.ContextMenuSettings))
                .setView(FluffyDialogUtils.wrapWithStandardPadding(linearLayout))
                .setPositiveButton(getString("Close", R.string.Close), null)
                .create();
        FluffyDialogUtils.applyWindowStyling(dialog);

        showDialog(dialog);
    }
    private int getRowPositionById(RowIdentifier id) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id == id) {
                return i;
            }
        }
        return -1;
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
        int padding = AndroidUtilities.dp(12);
        listView.setPadding(padding, padding, padding, padding);
        listView.setClipToPadding(false);
        listView.addItemDecoration(new CardBackgroundDecoration());
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);

        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        listView.setItemAnimator(itemAnimator);

        listView.setOnItemClickListener((view, position) -> {
            Row row = rows.get(position);
            handleItemClick(row.id, view, context);
        });

        return fragmentView;
    }
    private void handleItemClick(RowIdentifier rowId, View view, Context context) {
        switch (rowId) {
            case ZODIAC_SHOW:
                fluffyConfig.toggleZodiacShow();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.zodiacShow);
                }
                break;
            case STORIES_SHOW:
                fluffyConfig.toggleShowStories();
                if (chatListPreviewCell != null) {
                    chatListPreviewCell.updateStories(true);
                }
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.showStories);
                }
                break;
            case SHOW_DIVIDER:
                fluffyConfig.toggleShowDivider();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.showDivider);
                    Theme.applyCommonTheme();
                    parentLayout.rebuildAllFragmentViews(true, true);
                }
                break;
            case CALL_SHOW:
                fluffyConfig.toggleShowCallIcon();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.showCallIcon);
                }
                break;
            case CENTER_TITLE:
                fluffyConfig.toggleCenterTitle();
                if (chatListPreviewCell != null) {
                    chatListPreviewCell.updateCenteredTitle(true);
                }
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.centerTitle);
                    parentLayout.rebuildAllFragmentViews(false, false);
                }
                break;
            case CENTER_TITLE_IN_CHAT:
                fluffyConfig.toggleCenterTitleInChat();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.centerTitleInChat);
                    parentLayout.rebuildAllFragmentViews(false, false);
                }
                break;
            case DISABLE_ROUND:
                fluffyConfig.toggleRoundingNumber();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.disableRoundingNumber);
                }
                break;
            case REMOVE_GIFTS:
                fluffyConfig.toggleGift();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.hideGift);
                }
                break;
            case HIDE_PAID_REACTIONS:
                fluffyConfig.toggleHidePaidReactions();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.hidePaidReactions);
                }
                break;
            case REMOVE_BUTTON:
                fluffyConfig.toggleHideButtonWrite();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.hideButtonWrite);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.fluffy_floatingButtonSettingsChanged);
                break;
            case HIDE_BIZ_BOT_BAR:
                fluffyConfig.toggleHideTopBar();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.hideTopBar);
                }
                break;
            case FORCE_CHAT_SNOW:
                fluffyConfig.toggleForceChatSnow();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.forceChatSnow);
                }
                if (parentLayout != null) {
                    parentLayout.rebuildAllFragmentViews(false, false);
                }
                break;
            case EMOJI_LONGPRESS_MENU:
                fluffyConfig.toggleEmojiButtonLongPressMenu();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.emojiButtonLongPressMenu);
                }
                break;
            case MORE_INFO:
                fluffyConfig.toggleMoreInfoOnline();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.moreInfoOnline);
                }
                break;
            case ONLINE_STATUS_RING:
                fluffyConfig.toggleOnlineStatusRing();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.onlineStatusRing);
                }
                break;
            case NEW_SWITCH_STYLE:
                fluffyConfig.toggleNewSwitchStyle();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.newSwitchStyle);
                }
                updateRows();
                break;
            case SELECT_CUSTOM_FONT:
                showCustomFontDialog(context);
                break;
            case SYSTEM_TYPEFACE:
                fluffyConfig.toggleUseSystemFonts();
                AndroidUtilities.clearTypefaceCache();
                showRestartNotification(LaunchActivity.getSafeLastFragment());
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.useSystemFonts);
                }
                updateRows();
                break;
            case USE_SOLAR_ICONS:
                fluffyConfig.toggleUseSolarIcons();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.useSolarIcons);
                }
                break;
            case FORMAT_TIME_WITH_SECONDS:
                fluffyConfig.toggleFormatTimeWithSeconds();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.formatTimeWithSeconds);
                }
                break;
            case SELECT_TITLE:
                titleSelecter(context);
                break;
            case DOUBLE_TAP:
                selectorReaction();
                break;
            case QUICK_SWITCHER:
                if (view instanceof SetDefaultReactionCell) {
                    showSelectStatusDialog((SetDefaultReactionCell) view);
                }
                break;
            case STICKER_TIME_STAMP:
                timeStampSelecter(context);
                break;
            case STICKER_BLACKLIST:
                presentFragment(new StickerBlacklistActivity());
                break;
            case TRANSPARENCY:
                showTransparencyDialog(context);
                break;
            case MENU_CUSTOMIZATION:
                showMenuItemConfigurator(context);
                break;
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        super.onActivityResultFragment(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_IMPORT_CUSTOM_FONT && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                importCustomFontFromUri(uri);
            } else {
                BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportError), null);
            }
        }
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
        AlertDialog.Builder builder = FluffyDialogUtils.themedBuilder(context);

        var linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setView(FluffyDialogUtils.wrapWithStandardPadding(linearLayout));

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
                    int oldIndex = types.indexOf(old);
                    if (oldIndex != -1) {
                        ((RadioColorCell) layout.getChildAt(oldIndex)).setChecked(false, true);
                    }
                    cell.setChecked(true, true);
                    if (doubleTapCell != null) {
                        doubleTapCell.updateIcons(out ? 2 : 1, true);
                    }
                    previewCell.updateIcons(out ? 2 : 1 , true);
                });
            }
        }
        builder.setNegativeButton(LocaleController.getString(R.string.OK), null);
        AlertDialog dialog = builder.create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        showDialog(dialog);
    }

    private void showCustomFontDialog(Context context) {
        if (context == null || getParentActivity() == null) {
            return;
        }

        fluffyConfig.refreshCustomFontsFromStorage();
        ArrayList<File> fontFiles = new ArrayList<>(fluffyConfig.scanAvailableFonts());

        CharSequence[] items = new CharSequence[fontFiles.size() + 1];
        items[0] = getString(R.string.CustomFontDisabled);
        int checkedIndex = 0;
        for (int i = 0; i < fontFiles.size(); i++) {
            File file = fontFiles.get(i);
            items[i + 1] = file.getName();
            if (TextUtils.equals(fluffyConfig.customFontPath, file.getAbsolutePath())) {
                checkedIndex = i + 1;
            }
        }

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingHorizontal = dp(24);
        int paddingBottom = dp(12);
        rootLayout.setPadding(paddingHorizontal, 0, paddingHorizontal, paddingBottom);

        List<RadioColorCell> radioCells = new ArrayList<>();
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            RadioColorCell cell = new RadioColorCell(getParentActivity());
            cell.setPadding(dp(4), 0, dp(4), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(items[index], index == checkedIndex);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(2);
            cell.setLayoutParams(params);
            rootLayout.addView(cell);
            radioCells.add(cell);

            cell.setOnClickListener(v -> {
                if (handleCustomFontSelection(index, fontFiles)) {
                    for (int j = 0; j < radioCells.size(); j++) {
                        radioCells.get(j).setChecked(j == index, true);
                    }
                    Dialog dialog = dialogRef.get();
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }
            });

            if (index > 0) {
                cell.setOnLongClickListener(v -> {
                    handleCustomFontDeletion(context, fontFiles.get(index - 1), dialogRef);
                    return true;
                });
            }
        }

        TextView addFontButton = new TextView(context);
        addFontButton.setText(getString(R.string.CustomFontUpload));
        addFontButton.setTextColor(Theme.getColor(Theme.key_dialogTextBlue2));
        addFontButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        addFontButton.setGravity(Gravity.CENTER);
        addFontButton.setPadding(0, dp(16), 0, dp(4));
        addFontButton.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        addFontButton.setOnClickListener(v -> {
            Dialog dialog = dialogRef.get();
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
            startCustomFontPicker();
        });
        LinearLayout.LayoutParams addFontParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        addFontParams.topMargin = dp(12);
        rootLayout.addView(addFontButton, addFontParams);

        AlertDialog dialog = FluffyDialogUtils.themedBuilder(context)
                .setTitle(getString(R.string.SelectCustomFont))
                .setView(FluffyDialogUtils.wrapWithStandardPadding(rootLayout))
                .setNegativeButton(getString("Cancel", R.string.Cancel), null)
                .create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        dialogRef.set(dialog);
        showDialog(dialog);
    }

    private boolean handleCustomFontSelection(int which, List<File> fontFiles) {
        int currentIndex = 0;
        if (fluffyConfig.hasCustomFont()) {
            String currentPath = fluffyConfig.customFontPath;
            for (int i = 0; i < fontFiles.size(); i++) {
                if (TextUtils.equals(currentPath, fontFiles.get(i).getAbsolutePath())) {
                    currentIndex = i + 1;
                    break;
                }
            }
        }

        if (which == currentIndex) {
            return false;
        }

        boolean changed = false;
        if (which == 0) {
            if (fluffyConfig.hasCustomFont()) {
                fluffyConfig.clearCustomFont();
                changed = true;
            }
        } else {
            File file = fontFiles.get(which - 1);
            if (!TextUtils.equals(fluffyConfig.customFontPath, file.getAbsolutePath())) {
                fluffyConfig.setCustomFont(file.getName(), file.getAbsolutePath());
                changed = true;
            }
        }

        if (!changed) {
            return false;
        }

        boolean systemFontToggledOff = false;
        if (fluffyConfig.useSystemFonts) {
            fluffyConfig.toggleUseSystemFonts();
            systemFontToggledOff = true;
        }
        AndroidUtilities.clearTypefaceCache();
        if (systemFontToggledOff) {
            updateRows();
        } else {
            int position = getRowPositionById(RowIdentifier.SELECT_CUSTOM_FONT);
            if (position != -1) {
                listAdapter.notifyItemChanged(position);
            }
        }
        CharSequence subtitle = which == 0 ? getString(R.string.CustomFontDisabled) : fontFiles.get(which - 1).getName();
        BaseFragment fragment = LaunchActivity.getSafeLastFragment();
        if (fragment != null) {
            BulletinHelper.showSimpleBulletin(fragment, getString(R.string.CustomFontApplied), subtitle);
            AndroidUtilities.runOnUIThread(() -> showRestartNotification(fragment), 200);
        }
        return true;
    }

    private void handleCustomFontDeletion(Context context, File file, AtomicReference<Dialog> dialogRef) {
        AlertDialog.Builder confirmBuilder = FluffyDialogUtils.themedBuilder(context);
        confirmBuilder.setTitle(file.getName());
        confirmBuilder.setMessage(LocaleController.formatString("CustomFontDeleteConfirm", R.string.CustomFontDeleteConfirm, file.getName()));
        confirmBuilder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, which) -> {
            boolean deleted = file.delete();
            BaseFragment fragment = LaunchActivity.getSafeLastFragment();
            if (deleted) {
                if (fragment != null) {
                    BulletinHelper.showSimpleBulletin(fragment, getString(R.string.CustomFontDeleteSuccess), file.getName());
                }
                if (TextUtils.equals(fluffyConfig.customFontPath, file.getAbsolutePath())) {
                    fluffyConfig.clearCustomFont();
                    AndroidUtilities.clearTypefaceCache();
                    int systemRow = getRowPositionById(RowIdentifier.SYSTEM_TYPEFACE);
                    if (systemRow != -1) {
                        listAdapter.notifyItemChanged(systemRow);
                    }
                    int selectionRow = getRowPositionById(RowIdentifier.SELECT_CUSTOM_FONT);
                    if (selectionRow != -1) {
                        listAdapter.notifyItemChanged(selectionRow);
                    }
                    if (fragment != null) {
                        AndroidUtilities.runOnUIThread(() -> showRestartNotification(fragment), 200);
                    }
                }
                Dialog dialog = dialogRef.get();
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                }
                AndroidUtilities.runOnUIThread(() -> showCustomFontDialog(context));
            } else {
                if (fragment != null) {
                    BulletinHelper.showSimpleBulletin(fragment, getString(R.string.CustomFontDeleteFailed), file.getAbsolutePath());
                }
            }
        });
        confirmBuilder.setNegativeButton(getString("Cancel", R.string.Cancel), null);
        AlertDialog dialog = confirmBuilder.create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        showDialog(dialog);
    }

    private void startCustomFontPicker() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                    "font/ttf",
                    "application/x-font-ttf",
                    "application/x-font-truetype",
                    "application/font-sfnt"
            });
            startActivityForResult(Intent.createChooser(intent, getString(R.string.CustomFontUpload)), REQUEST_CODE_IMPORT_CUSTOM_FONT);
        } catch (Exception e) {
            FileLog.e(e);
            BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportError), e.getLocalizedMessage());
        }
    }

    private boolean importCustomFontFromUri(Uri uri) {
        Activity activity = getParentActivity();
        if (activity == null || uri == null) {
            return false;
        }

        DocumentFile documentFile = DocumentFile.fromSingleUri(activity, uri);
        String displayName = documentFile != null ? documentFile.getName() : null;
        if (TextUtils.isEmpty(displayName)) {
            displayName = MediaController.getFileName(uri);
        }

        if (TextUtils.isEmpty(displayName)) {
            BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportError), null);
            return false;
        }

        String lowerCaseName = displayName.toLowerCase(Locale.ROOT);
        if (!lowerCaseName.endsWith(".ttf")) {
            BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontWrongFile), displayName);
            return false;
        }

        File fontsDir = fluffyConfig.getFontsDirectory();
        if (!fontsDir.exists() && !fontsDir.mkdirs()) {
            BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportError), fontsDir.getAbsolutePath());
            return false;
        }

        File destination = resolveFontDestination(fontsDir, displayName);

        try (InputStream inputStream = activity.getContentResolver().openInputStream(uri)) {
            if (inputStream == null) {
                BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportError), displayName);
                return false;
            }
            if (!AndroidUtilities.copyFile(inputStream, destination)) {
                BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportError), displayName);
                return false;
            }
            BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportSuccess), destination.getAbsolutePath());
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.CustomFontImportError), e.getLocalizedMessage());
            return false;
        }
    }

    private File resolveFontDestination(File directory, String fileName) {
        File destination = new File(directory, fileName);
        if (!destination.exists()) {
            return destination;
        }

        int dot = fileName.lastIndexOf('.');
        String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String extension = dot >= 0 ? fileName.substring(dot) : "";
        int index = 1;
        while (destination.exists()) {
            destination = new File(directory, base + " (" + index + ")" + extension);
            index++;
        }
        return destination;
    }

    private void titleSelecter(Context context) {
        if (getParentActivity() == null) {
            return;
        }
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        // Контейнер с нужными отступами
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        int paddingHorizontal = dp(24);
        int paddingBottom = dp(16);
        rootLayout.setPadding(paddingHorizontal, 0, paddingHorizontal, paddingBottom);

        CharSequence[] items = new CharSequence[]{
                fluffyConfig.getUsername(),
                "fluffy",
                "telegram",
                "Disable",
                "Custom"
        };

        // Список радио-клеток для удобной синхронизации состояний
        List<RadioColorCell> radioCells = new ArrayList<>();

        // Поле для кастомного текста
        final EditText customEditText = new EditText(context);
        customEditText.setHint("Введите свой вариант");
        customEditText.setText(fluffyConfig.customTitle != null ? fluffyConfig.customTitle : "");
        LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        editTextParams.topMargin = dp(8);
        customEditText.setLayoutParams(editTextParams);
        FluffyDialogUtils.styleTextInput(customEditText, resourceProvider);

        // Создаём радиокнопки
        for (int i = 0; i < items.length; ++i) {
            final int index = i;
            RadioColorCell cell = new RadioColorCell(getParentActivity());
            cell.setPadding(dp(4), 0, dp(4), 0);
            cell.setCheckColor(
                    Theme.getColor(Theme.key_radioBackground),
                    Theme.getColor(Theme.key_dialogRadioBackgroundChecked)
            );
            cell.setTextAndValue(items[index], index == fluffyConfig.titleType);
            cell.setBackground(Theme.createSelectorDrawable(
                    Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cellParams.bottomMargin = dp(2);
            cell.setLayoutParams(cellParams);
            rootLayout.addView(cell);
            radioCells.add(cell);

            cell.setOnClickListener(v -> {
                // Снимаем выбор со всех, кроме текущего
                for (int j = 0; j < radioCells.size(); j++) {
                    radioCells.get(j).setChecked(j == index, true);
                }
                if (index == 4) {
                    customEditText.setVisibility(View.VISIBLE);
                    customEditText.requestFocus();
                    if (fluffyConfig.customTitle != null) {
                        customEditText.setText(fluffyConfig.customTitle);
                        customEditText.setSelection(fluffyConfig.customTitle.length());
                    } else {
                        customEditText.setText("");
                    }
                } else {
                    customEditText.setVisibility(View.GONE);
                    fluffyConfig.setTitleType(index);
                    getNotificationCenter().postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
                    int position = getRowPositionById(RowIdentifier.SELECT_TITLE);
                    if (position != -1) {
                        RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(position);
                        if (holder != null) {
                            listAdapter.onBindViewHolder(holder, position);
                        }
                    }
                    getNotificationCenter().postNotificationName(NotificationCenter.currentUserPremiumStatusChanged);
                    dialogRef.get().dismiss();
                    if (chatListPreviewCell != null) {
                        chatListPreviewCell.updateTitle(true);
                    }
                }
            });
        }

        rootLayout.addView(customEditText);

        if (fluffyConfig.titleType == 4) {
            customEditText.setVisibility(View.VISIBLE);
            if (fluffyConfig.customTitle != null) {
                customEditText.setText(fluffyConfig.customTitle);
                customEditText.setSelection(fluffyConfig.customTitle.length());
            }
        } else {
            customEditText.setVisibility(View.GONE);
        }

        AlertDialog dialog = FluffyDialogUtils.themedBuilder(getParentActivity())
                .setTitle(getString(R.string.TitleSelecter))
                .setView(FluffyDialogUtils.wrapWithStandardPadding(rootLayout))
                .setNegativeButton(getString("Cancel", R.string.Cancel), null)
                .setPositiveButton("OK", (d, id) -> {
                    if (radioCells.get(4).isChecked()) {
                        String customTitle = customEditText.getText().toString().trim();
                        if (!customTitle.isEmpty()) {
                            fluffyConfig.setTitleType(4);
                            fluffyConfig.setСustomTitle(customTitle);
                            onCustomTitleEntered(customTitle);
                            if (chatListPreviewCell != null) {
                                chatListPreviewCell.updateTitle(true);
                            }
                        }
                    }
                    int position = getRowPositionById(RowIdentifier.SELECT_TITLE);
                    if (position != -1) {
                        listAdapter.notifyItemChanged(position);
                    }
                })
                .create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        dialogRef.set(dialog);
        showDialog(dialog);
    }

    // Утилита dp для отступов
    private int dp(int value) {
        float density = getParentActivity().getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }

    // Пример обработчика кастомного текста:
    private void onCustomTitleEntered(String customTitle) {
        // Здесь пиши, что тебе нужно делать с customTitle
        // Можно модифицировать fluffyConfig или отправить куда-то еще
    }

    private static class TransparencyPreviewView extends FrameLayout {

        private final Theme.ResourcesProvider resourcesProvider;
        private final TextView incomingBubble;
        private final TextView outgoingBubble;
        private final int incomingBaseColor;
        private final int outgoingBaseColor;

        private final CharSequence incomingSampleText;
        private final CharSequence outgoingSampleText;

        TransparencyPreviewView(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;

            setClipChildren(true);
            setClipToPadding(true);
            setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
            setMinimumHeight(AndroidUtilities.dp(180));

            Drawable wallpaper = Theme.getCachedWallpaperNonBlocking();
            if (wallpaper != null) {
                Drawable drawable = wallpaper.getConstantState() != null ? wallpaper.getConstantState().newDrawable().mutate() : wallpaper.mutate();
                if (drawable instanceof MotionBackgroundDrawable) {
                    ((MotionBackgroundDrawable) drawable).setParentView(this);
                }
                setBackground(drawable);
            } else {
                setBackgroundColor(Theme.getColor(Theme.key_chat_wallpaper, resourcesProvider));
            }

            BackgroundPlaceholderView placeholderView = new BackgroundPlaceholderView(context, resourcesProvider);
            placeholderView.setAlpha(0.55f);
            addView(placeholderView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            View scrim = new View(context);
            scrim.setBackgroundColor(Theme.multAlpha(0xFF000000, 0.09f));
            addView(scrim, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            LinearLayout bubbleContainer = new LinearLayout(context);
            bubbleContainer.setOrientation(LinearLayout.VERTICAL);
            bubbleContainer.setGravity(Gravity.BOTTOM);
            bubbleContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
            addView(bubbleContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

            incomingBaseColor = Theme.getColor(Theme.key_chat_inBubble, resourcesProvider);
            outgoingBaseColor = Theme.getColor(Theme.key_chat_outBubble, resourcesProvider);
            incomingSampleText = LocaleController.getString(R.string.ThemePreviewLine2);
            outgoingSampleText = LocaleController.getString(R.string.ThemePreviewLine1);

            incomingBubble = buildBubble(context, false);
            outgoingBubble = buildBubble(context, true);

            bubbleContainer.addView(incomingBubble, createBubbleLayoutParams(false));
            bubbleContainer.addView(outgoingBubble, createBubbleLayoutParams(true));
        }

        private LinearLayout.LayoutParams createBubbleLayoutParams(boolean outgoing) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
            params.gravity = outgoing ? Gravity.END : Gravity.START;
            params.topMargin = AndroidUtilities.dp(8);
            return params;
        }

        private TextView buildBubble(Context context, boolean outgoing) {
            TextView textView = new TextView(context);
            textView.setText(outgoing ? outgoingSampleText : incomingSampleText);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textView.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
            textView.setMaxWidth(AndroidUtilities.dp(260));
            textView.setTextColor(Theme.getColor(outgoing ? Theme.key_chat_messageTextOut : Theme.key_chat_messageTextIn, resourcesProvider));
            textView.setBackground(createBubbleDrawable(outgoing ? outgoingBaseColor : incomingBaseColor, fluffyConfig.transparency));
            return textView;
        }

        private Drawable createBubbleDrawable(int baseColor, int alpha) {
            GradientDrawable drawable = new GradientDrawable();
            float radius = AndroidUtilities.dp(16);
            drawable.setCornerRadii(new float[]{radius, radius, radius, radius, radius, radius, radius, radius});
            drawable.setColor(ColorUtils.setAlphaComponent(baseColor, alpha));
            return drawable;
        }

        void setTransparency(int alpha) {
            incomingBubble.setBackground(createBubbleDrawable(incomingBaseColor, alpha));
            outgoingBubble.setBackground(createBubbleDrawable(outgoingBaseColor, alpha));
        }
    }

    private static class BackgroundPlaceholderView extends View {

        private final Paint lightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int tileSize;
        private final int stripeSpacing;
        private final int stripeHeight;

        BackgroundPlaceholderView(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            tileSize = AndroidUtilities.dp(32);
            stripeSpacing = AndroidUtilities.dp(70);
            stripeHeight = AndroidUtilities.dp(2);

            int baseColor = Theme.getColor(Theme.key_chat_wallpaper, resourcesProvider);
            int accentColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider);
            int overlay = Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider);

            lightPaint.setStyle(Paint.Style.FILL);
            lightPaint.setColor(Theme.multAlpha(overlay, 0.08f));

            darkPaint.setStyle(Paint.Style.FILL);
            darkPaint.setColor(Theme.multAlpha(baseColor, 0.2f));

            accentPaint.setStyle(Paint.Style.FILL);
            accentPaint.setColor(Theme.multAlpha(accentColor, 0.45f));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int width = getWidth();
            int height = getHeight();

            for (int y = 0; y < height + tileSize; y += tileSize) {
                for (int x = 0; x < width + tileSize; x += tileSize) {
                    boolean even = ((x / tileSize) + (y / tileSize)) % 2 == 0;
                    Paint paint = even ? lightPaint : darkPaint;
                    canvas.drawRect(x, y, x + tileSize, y + tileSize, paint);
                }
            }

            for (int y = 0; y < height + stripeSpacing; y += stripeSpacing) {
                canvas.drawRect(0, y, width, y + stripeHeight, accentPaint);
            }
        }
    }


    private void showTransparencyDialog(Context context) {
        if (getParentActivity() == null) {
            return;
        }

        AlertDialog.Builder builder = FluffyDialogUtils.themedBuilder(getParentActivity());
        builder.setTitle(getString(R.string.Transparency));
        builder.setMessage(getString(R.string.EnterValueBetween0And255));

        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);

        final TransparencyPreviewView previewView = new TransparencyPreviewView(context, resourceProvider);
        rootLayout.addView(previewView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 220, 0, 4, 0, 12));

        TextView valueView = new TextView(context);
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourceProvider));
        valueView.setTextSize(14);
        valueView.setGravity(Gravity.CENTER_HORIZONTAL);
        rootLayout.addView(valueView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 6));

        SeekBarView seekBarView = new SeekBarView(context);
        seekBarView.setReportChanges(true);
        rootLayout.addView(seekBarView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 38, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 0));

        final int[] selectedValue = { clampTransparency(fluffyConfig.transparency) };
        float initialProgress = selectedValue[0] / 255f;
        previewView.setTransparency(selectedValue[0]);
        valueView.setText(formatTransparencyValue(selectedValue[0]));
        seekBarView.setProgress(initialProgress, false);
        seekBarView.setDelegate((stop, progress) -> {
            int newValue = clampTransparency(Math.round(progress * 255f));
            selectedValue[0] = newValue;
            valueView.setText(formatTransparencyValue(newValue));
            previewView.setTransparency(newValue);
        });

        builder.setView(FluffyDialogUtils.wrapWithStandardPadding(rootLayout));

        builder.setPositiveButton(getString(R.string.OK), (dialog, which) -> {
            int value = selectedValue[0];
            fluffyConfig.setTransparency(value);
            showRestartNotification(LaunchActivity.getSafeLastFragment());

            int position = getRowPositionById(RowIdentifier.TRANSPARENCY);
            if (position != -1 && listAdapter != null) {
                RecyclerView.ViewHolder holder = listView != null ? listView.findViewHolderForAdapterPosition(position) : null;
                if (holder != null) {
                    listAdapter.onBindViewHolder(holder, position);
                } else {
                    listAdapter.notifyItemChanged(position);
                }
            }
        });

        builder.setNegativeButton(getString(R.string.Cancel), (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        showDialog(dialog);
    }

    private String formatTransparencyValue(int value) {
        int percent = Math.round((value / 255f) * 100f);
        return String.format(Locale.getDefault(), "%d (%d%%)", value, percent);
    }

    private static int clampTransparency(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private void timeStampSelecter(Context context) {
        if (getParentActivity() == null) {
            return;
        }
        AtomicReference<Dialog> dialogRef = new AtomicReference<>();

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        CharSequence[] items = new CharSequence[]{
                getString(R.string.TimeWithReadStatus),
                getString(R.string.ReadStatus),
                getString(R.string.None)
        };

        for (int i = 0; i < items.length; ++i) {
            final int index = i;
            RadioColorCell cell = new RadioColorCell(getParentActivity());
            cell.setPadding(dp(4), 0, dp(4), 0);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(items[index], index == fluffyConfig.readStickerMode);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                fluffyConfig.setReadStickerMode(index);
                int position = getRowPositionById(RowIdentifier.STICKER_TIME_STAMP);
                if (position != -1) {
                    RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(position);
                    if (holder != null) {
                        listAdapter.onBindViewHolder(holder, position);
                    }
                }


                dialogRef.get().dismiss();
                if (chatListPreviewCell != null) {
                    chatListPreviewCell.updateTitle(true);
                }
                if (stickerSizePreview != null) {
                    stickerSizePreview.invalidate();
                    stickerSizePreview.rebuildStickerPreview();
                } else {
                    Log.w(TAG, "stickerSizePreview is null, can't invalidate");
                }
            });
        }

        AlertDialog dialog = FluffyDialogUtils.themedBuilder(getParentActivity())
                .setTitle(getString(R.string.TimestampSelecter))
                .setView(FluffyDialogUtils.wrapWithStandardPadding(linearLayout))
                .setNegativeButton(getString(R.string.Cancel), null)
                .create();
        FluffyDialogUtils.applyWindowStyling(dialog);
        dialogRef.set(dialog);
        showDialog(dialog);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateRows();
    }

    private class SetDefaultReactionCell extends FrameLayout {

        private TextView textView;
        private AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable imageDrawable;

        public SetDefaultReactionCell(Context context) {
            super(context);

            setBackground(null);
            setPadding(AndroidUtilities.dp(21), 0, AndroidUtilities.dp(21), 0);

            textView = new TextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setText(LocaleController.getString(R.string.DoubleTapSetting));
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 20, 0, 48, 0));

            imageDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(this, AndroidUtilities.dp(24));
        }

        public void update(boolean animated) {
            String reactionString = MediaDataController.getInstance(currentAccount).getDoubleTapReaction();
            if (reactionString != null && reactionString.startsWith("animated_")) {
                try {
                    long documentId = Long.parseLong(reactionString.substring(9));
                    imageDrawable.set(documentId, animated);
                    return;
                } catch (Exception ignore) {}
            }
            TLRPC.TL_availableReaction reaction = MediaDataController.getInstance(currentAccount).getReactionsMap().get(reactionString);
            if (reaction != null) {
                imageDrawable.set(reaction.static_icon, animated);
            }
        }

        public void updateImageBounds() {
            imageDrawable.setBounds(
                    getWidth() - imageDrawable.getIntrinsicWidth() - AndroidUtilities.dp(21),
                    (getHeight() - imageDrawable.getIntrinsicHeight()) / 2,
                    getWidth() - AndroidUtilities.dp(21),
                    (getHeight() + imageDrawable.getIntrinsicHeight()) / 2
            );
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            updateImageBounds();
            imageDrawable.draw(canvas);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY)
            );
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            imageDrawable.detach();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            imageDrawable.attach();
        }
    }

    private List<TLRPC.TL_availableReaction> getAvailableReactions() {
        return getMediaDataController().getReactionsList();
    }
    private SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow selectAnimatedEmojiDialog;
    public void showSelectStatusDialog(SetDefaultReactionCell cell) {
        if (selectAnimatedEmojiDialog != null) {
            return;
        }
        final SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[] popup = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow[1];
        int xoff = 0, yoff = 0;
        AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable scrimDrawable = null;
        View scrimDrawableParent = null;
        if (cell != null) {
            scrimDrawable = cell.imageDrawable;
            scrimDrawableParent = cell;
            if (cell.imageDrawable != null) {
                cell.imageDrawable.play();
                cell.updateImageBounds();
                AndroidUtilities.rectTmp2.set(cell.imageDrawable.getBounds());
                yoff = -(cell.getHeight() - AndroidUtilities.rectTmp2.centerY()) - AndroidUtilities.dp(16);
                int popupWidth = (int) Math.min(AndroidUtilities.dp(340 - 16), AndroidUtilities.displaySize.x * .95f);
                xoff = AndroidUtilities.rectTmp2.centerX() - (AndroidUtilities.displaySize.x - popupWidth);
            }
        }
        SelectAnimatedEmojiDialog popupLayout = new SelectAnimatedEmojiDialog(this, getContext(), false, xoff, SelectAnimatedEmojiDialog.TYPE_SET_DEFAULT_REACTION, null) {
            @Override
            protected void onEmojiSelected(View emojiView, Long documentId, TLRPC.Document document, TL_stars.TL_starGiftUnique gift, Integer until) {
                if (documentId == null) {
                    return;
                }
                MediaDataController.getInstance(currentAccount).setDoubleTapReaction("animated_" + documentId);
                if (cell != null) {
                    cell.update(true);
                }
                if (popup[0] != null) {
                    selectAnimatedEmojiDialog = null;
                    popup[0].dismiss();
                }
            }

            @Override
            protected void onReactionClick(ImageViewEmoji emoji, ReactionsLayoutInBubble.VisibleReaction reaction) {
                MediaDataController.getInstance(currentAccount).setDoubleTapReaction(reaction.emojicon);
                if (cell != null) {
                    cell.update(true);
                }
                if (popup[0] != null) {
                    selectAnimatedEmojiDialog = null;
                    popup[0].dismiss();
                }
            }
        };
        String selectedReaction = getMediaDataController().getDoubleTapReaction();
        if (selectedReaction != null && selectedReaction.startsWith("animated_")) {
            try {
                popupLayout.setSelected(Long.parseLong(selectedReaction.substring(9)));
            } catch (Exception e) {}
        }
        List<TLRPC.TL_availableReaction> availableReactions = getAvailableReactions();
        ArrayList<ReactionsLayoutInBubble.VisibleReaction> reactions = new ArrayList<>(20);
        for (int i = 0; i < availableReactions.size(); ++i) {
            ReactionsLayoutInBubble.VisibleReaction reaction = new ReactionsLayoutInBubble.VisibleReaction();
            TLRPC.TL_availableReaction tlreaction = availableReactions.get(i);
            reaction.emojicon = tlreaction.reaction;
            reactions.add(reaction);
        }
        popupLayout.setRecentReactions(reactions);
        popupLayout.setSaveState(3);
        popupLayout.setScrimDrawable(scrimDrawable, scrimDrawableParent);
        popup[0] = selectAnimatedEmojiDialog = new SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
            @Override
            public void dismiss() {
                super.dismiss();
                selectAnimatedEmojiDialog = null;
            }
        };
        popup[0].showAsDropDown(cell, 0, yoff, Gravity.TOP | Gravity.RIGHT);
        popup[0].dimBehind();
    }

    public class StickerSizeSeekBarCell extends FrameLayout {
        private TextView titleView;        private TextView valueView;
        private SeekBarView seekBarView;

        public StickerSizeSeekBarCell(Context context) {
            super(context);
            setWillNotDraw(false);
            setPadding(dp(21), 0, dp(21), 0);
            setBackground(null);

            LinearLayout hLayout = new LinearLayout(context);
            hLayout.setOrientation(LinearLayout.HORIZONTAL);
            hLayout.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            titleView = new TextView(context);
            titleView.setText("Stickers Size");
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setTextSize(15);
            hLayout.addView(titleView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            valueView = new TextView(context);
            valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            valueView.setTextSize(13);
            valueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            hLayout.addView(valueView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            addView(hLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 0, 8, 0, 0));

            seekBarView = new SeekBarView(context);
            seekBarView.setReportChanges(true);

            seekBarView.setDelegate((stop, progress) -> {
                int value = (int) (5 + 15 * progress);
                valueView.setText(String.format(Locale.US, "%d", value));
            });
            addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.START, 0, 34, 0, 0));
        }

        public void setOnValueChange(SeekBarView.SeekBarViewDelegate delegate) {
            seekBarView.setDelegate(delegate);
        }

        public void setValue(int value) {
            valueView.setText(String.format(Locale.US, "%d", value));
            float progress = (value - 5) / 15.0f;
            seekBarView.setProgress(progress, false);
        }

        public TextView getValueView() {
            return valueView;
        }
    }
    public class StickerRadiusSeekBarCell extends FrameLayout {
        private TextView titleView;
        private TextView valueView;
        private SeekBarView seekBarView;

        public StickerRadiusSeekBarCell(Context context) {
            super(context);
            setWillNotDraw(false);
            setPadding(dp(21), 0, dp(21), 0);
            setBackground(null);

            LinearLayout hLayout = new LinearLayout(context);
            hLayout.setOrientation(LinearLayout.HORIZONTAL);
            hLayout.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);

            titleView = new TextView(context);
            titleView.setText("Sticker Radius");
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            titleView.setTextSize(15);
            hLayout.addView(titleView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            valueView = new TextView(context);
            valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            valueView.setTextSize(13);
            valueView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            hLayout.addView(valueView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

            addView(hLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 0, 8, 0, 0));

            seekBarView = new SeekBarView(context);
            seekBarView.setReportChanges(true);

            seekBarView.setDelegate((stop, progress) -> {
                int value = (int) (stickerRaduisMax * progress);
                valueView.setText(String.format(Locale.US, "%d", value));
            });
            addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.TOP | Gravity.START, 0, 34, 0, 0));
        }

        public void setOnValueChange(SeekBarView.SeekBarViewDelegate delegate) {
            seekBarView.setDelegate(delegate);
        }

        public void setValue(int value) {
            valueView.setText(String.format(Locale.US, "%d", value));
            float progress = value / (float) stickerRaduisMax;
            seekBarView.setProgress(progress, false);
        }

        public TextView getValueView() {
            return valueView;
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
                case HERO_CARD:
                    HeroCardHolder heroHolder = (HeroCardHolder) holder;
                    heroHolder.bind(row);
                    break;
                case INFO_BLOCK:
                    InfoBlockHolder infoHolder = (InfoBlockHolder) holder;
                    infoHolder.bind(row);
                    break;
                case SHADOW_SECTION:
                    holder.itemView.setBackground(null);
                    break;
                case NOTIFICATIONS_CHECK:
                    break;
                case HEADER:
                    TextView headerTextView = (TextView) holder.itemView;
                    CharSequence headerText = row.customText != null ? row.customText : (row.textResId != 0 ? getString(row.textResId) : "");
                    headerTextView.setText(headerText);
                    break;
                case TEXT_INFO_PRIVACY:
                    TextInfoPrivacyCell textInfoPrivacyCell = (TextInfoPrivacyCell) holder.itemView;
                    CharSequence infoText = row.customText != null ? row.customText : (row.textResId != 0 ? getString(row.textResId) : "");
                    textInfoPrivacyCell.setText(infoText);
                    break;
                case CHAT_LIST_PREVIEW:
                    chatListPreviewCell = (ChatListPreviewCell) holder.itemView;
                    break;
                case TEXT_CELL:
                    TextCell textCell6 = (TextCell) holder.itemView;
                    String value = "";
                    switch (row.id) {
                        case SELECT_TITLE:
                            value = switch (fluffyConfig.titleType) {
                                case 0 -> fluffyConfig.getUsername();
                                case 1 -> "fluffy";
                                case 2 -> "telegram";
                                case 3 -> getString(R.string.Disable);
                                case 4 -> fluffyConfig.customTitle;
                                default -> LocaleController.getString(R.string.AppName);
                            };
                            break;
                        case STICKER_TIME_STAMP:
                            value = switch (fluffyConfig.readStickerMode) {
                                case 0 -> getString(R.string.TimeWithReadStatus);
                                case 1 -> getString(R.string.ReadStatus);
                                case 2 -> getString(R.string.None);
                                default -> getString(R.string.None);
                            };
                            break;
                        case TRANSPARENCY:
                            value = String.valueOf(fluffyConfig.transparency);
                            break;
                        case SELECT_CUSTOM_FONT:
                            value = TextUtils.isEmpty(fluffyConfig.customFontName) ? getString(R.string.CustomFontDisabled) : fluffyConfig.customFontName;
                            break;
                    }
                    textCell6.setTextAndValueAndIcon(getString(row.textResId), value, row.iconResId, true);
                    break;
                case DOUBLE_TAP_CELL:
                    doubleTapCell = (DoubleTapCell) holder.itemView;
                    break;
                case QUICK_SWITCHER:
                    setDefaultReactionCell = (SetDefaultReactionCell) holder.itemView;
                    setDefaultReactionCell.update(false);
                    break;
                case STICKER_SIZE_PREVIEW:
                    stickerSizePreview = (StickerSizePreviewMessagesCell) holder.itemView;
                    break;
                case STICKER_SIZE_SEEKBAR:
                    StickerSizeSeekBarCell seekBarCell = (StickerSizeSeekBarCell) holder.itemView;
                    int currentSize = fluffyConfig.stickerSize;
                    seekBarCell.setValue(currentSize);
                    seekBarCell.setOnValueChange((stop, progress) -> {
                        int newValue = (int) (5 + 15 * progress);
                        Log.d(TAG, "SeekBar changed: progress=" + progress + ", newValue=" + newValue);

                        seekBarCell.getValueView().setText(String.format(Locale.US, "%d", newValue));
                        fluffyConfig.setStickerSize(newValue);

                        if (stickerSizePreview != null) {
                            Log.d(TAG, "Invalidate stickerSizePreview (size=" + newValue + ")");
                            Log.d(TAG, "stickerSizePreview: " + stickerSizePreview);
                            stickerSizePreview.invalidate();
                            stickerSizePreview.rebuildStickerPreview();
                        } else {
                            Log.w(TAG, "stickerSizePreview is null, can't invalidate");
                        }
                    });
                    break;
                case STICKER_RADIUS_SEEKBAR:
                    StickerRadiusSeekBarCell seekRadiusBarCell = (StickerRadiusSeekBarCell) holder.itemView;
                    int currentRadius = fluffyConfig.stickerRadius;
                    seekRadiusBarCell.setValue(currentRadius);
                    seekRadiusBarCell.setOnValueChange((stop, progress) -> {
                        int newValue = (int) (stickerRaduisMax * progress);
                        Log.d(TAG, "SeekBar changed: progress=" + progress + ", newValue=" + newValue);

                        seekRadiusBarCell.getValueView().setText(String.format(Locale.US, "%d", newValue));
                        fluffyConfig.setStickerRadius(newValue);

                        if (stickerSizePreview != null) {
                            Log.d(TAG, "Invalidate stickerSizePreview (size=" + newValue + ")");
                            Log.d(TAG, "stickerSizePreview: " + stickerSizePreview);
                            stickerSizePreview.invalidate();
                            stickerSizePreview.rebuildStickerPreview();
                        } else {
                            Log.w(TAG, "stickerSizePreview is null, can't invalidate");
                        }
                    });
                    break;
                case TEXT_CHECK:
                    TextCell textCell = (TextCell) holder.itemView;
                    textCell.setEnabled(true);
                    boolean checked = false;

                    String subtitle = null;

                    switch (row.id) {
                        case ZODIAC_SHOW:
                            checked = fluffyConfig.zodiacShow;
                            break;
                        case STORIES_SHOW:
                            checked = fluffyConfig.showStories;
                            break;
                        case CALL_SHOW:
                            checked = fluffyConfig.showCallIcon;
                            break;
                        case SHOW_DIVIDER:
                            checked = fluffyConfig.showDivider;
                            break;
                        case CENTER_TITLE:
                            checked = fluffyConfig.centerTitle;
                            break;
                        case SYSTEM_TYPEFACE:
                            checked = fluffyConfig.useSystemFonts;
                            break;
                        case USE_SOLAR_ICONS:
                            checked = fluffyConfig.useSolarIcons;
                            break;
                        case CENTER_TITLE_IN_CHAT:
                            checked = fluffyConfig.centerTitleInChat;
                            break;
                        case DISABLE_ROUND:
                            checked = fluffyConfig.disableRoundingNumber;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                        case REMOVE_GIFTS:
                            checked = fluffyConfig.hideGift;
                            break;
                        case HIDE_PAID_REACTIONS:
                            checked = fluffyConfig.hidePaidReactions;
                            break;
                        case REMOVE_BUTTON:
                            checked = fluffyConfig.hideButtonWrite;
                            break;
                        case FORCE_CHAT_SNOW:
                            checked = fluffyConfig.forceChatSnow;
                            break;
                        case HIDE_BIZ_BOT_BAR:
                            checked = fluffyConfig.hideTopBar;
                            break;
                        case EMOJI_LONGPRESS_MENU:
                            checked = fluffyConfig.emojiButtonLongPressMenu;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                        case MORE_INFO:
                            checked = fluffyConfig.moreInfoOnline;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                        case ONLINE_STATUS_RING:
                            checked = fluffyConfig.onlineStatusRing;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                        case NEW_SWITCH_STYLE:
                            checked = fluffyConfig.newSwitchStyle;
                            break;
                        case FORMAT_TIME_WITH_SECONDS:
                            checked = fluffyConfig.formatTimeWithSeconds;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                    }
                    if (subtitle == null && row.subtitleResId != 0) {
                        subtitle = getString(row.subtitleResId);
                    }
                    textCell.setTextAndCheckAndIcon(getString(row.textResId), checked, row.iconResId, true);

                    textCell.setSubtitle(subtitle);
                    break;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            Row row = rows.get(holder.getAdapterPosition());
            if (row.type == RowType.HERO_CARD || row.type == RowType.INFO_BLOCK) {
                return false;
            }
            if (row.type == RowType.STICKER_SIZE_PREVIEW || row.type == RowType.STICKER_SIZE_SEEKBAR || row.type == RowType.STICKER_RADIUS_SEEKBAR) {
                return false;
            }
            return row.type != RowType.SHADOW_SECTION && row.type != RowType.HEADER && row.type != RowType.TEXT_INFO_PRIVACY;
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            boolean customLayoutParams = false;
            switch (viewType) {
                case 0:
                    view = new View(mContext);
                    RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(12));
                    params.topMargin = AndroidUtilities.dp(2);
                    params.bottomMargin = AndroidUtilities.dp(2);
                    view.setLayoutParams(params);
                    view.setBackground(null);
                    customLayoutParams = true;
                    break;
                case 1:
                    view = new TextCell(mContext, 0, false, true, null);
                    applyCellBackground(view);
                    break;
                case 2:
                    view = new NotificationsCheckCell(mContext, 21, 60, true);
                    applyCellBackground(view);
                    break;
                case 3:
                    TextView headerView = new TextView(mContext);
                    headerView.setTypeface(AndroidUtilities.bold());
                    headerView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                    headerView.setLetterSpacing(0.02f);
                    headerView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                    headerView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
                    view = headerView;
                    break;
                case 4:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 5:
                    view = new ChatListPreviewCell(mContext);
                    applyCellBackground(view);
                    break;
                case 6:
                    view = new TextCell(mContext);
                    applyCellBackground(view);
                    break;
                case 7:
                    view = new DoubleTapCell(mContext);
                    break;
                case 8:
                    view = new StickerSizePreviewMessagesCell(mContext, appearanceActivitySettings.this);
                    break;
                case 9:
                    view = new StickerSizeSeekBarCell(mContext);
                    view.setBackground(null);
                    break;
                case 10:
                    view = new StickerRadiusSeekBarCell(mContext);
                    view.setBackground(null);
                    break;
                case 11:
                    view = new SetDefaultReactionCell(mContext);
                    applyCellBackground(view);
                    break;
                case 12:
                    view = new AppearanceHeroCardView(mContext);
                    break;
                case 13:
                    view = createInfoBlockView(mContext);
                    break;
                default:
                    view = new View(mContext);
                    break;
            }
            if (!customLayoutParams) {
                view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            }
            if (viewType == 12) {
                return new HeroCardHolder((AppearanceHeroCardView) view);
            } else if (viewType == 13) {
                return new InfoBlockHolder(view);
            }
            return new RecyclerListView.Holder(view);
        }


        @Override
        public int getItemViewType(int position) {
            Row row = rows.get(position);
            switch (row.type) {
                case SHADOW_SECTION: return 0;
                case TEXT_CHECK: return 1;
                case NOTIFICATIONS_CHECK: return 2;
                case HEADER: return 3;
                case TEXT_INFO_PRIVACY: return 4;
                case CHAT_LIST_PREVIEW: return 5;
                case TEXT_CELL: return 6;
                case DOUBLE_TAP_CELL: return 7;
                case STICKER_SIZE_PREVIEW: return 8;
                case STICKER_SIZE_SEEKBAR: return 9;
                case STICKER_RADIUS_SEEKBAR: return 10;
                case QUICK_SWITCHER: return 11;
                case HERO_CARD: return 12;
                case INFO_BLOCK: return 13;
                default: return -1;
            }
        }

        private class HeroCardHolder extends RecyclerView.ViewHolder {
            private final AppearanceHeroCardView heroCardView;

            HeroCardHolder(AppearanceHeroCardView view) {
                super(view);
                heroCardView = view;
            }

            void bind(Row row) {
                CharSequence title = row.textResId != 0 ? getString(row.textResId) : row.customText;
                heroCardView.bind(row.iconResId, title, row.customText);
            }
        }

        private class InfoBlockHolder extends RecyclerView.ViewHolder {
            InfoBlockHolder(View view) {
                super(view);
            }

            void bind(Row row) {
                if (itemView instanceof TextView) {
                    ((TextView) itemView).setText(row.customText != null ? row.customText : (row.textResId != 0 ? getString(row.textResId) : ""));
                }
            }
        }

        private View createInfoBlockView(Context context) {
            TextView textView = new TextView(context);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = AndroidUtilities.dp(16);
            textView.setLayoutParams(params);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            textView.setLineSpacing(AndroidUtilities.dp(2), 1.05f);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText4));
            textView.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(18), Theme.getColor(Theme.key_windowBackgroundWhite)));
            textView.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(14));
            return textView;
        }

        private void applyCellBackground(View view) {
            view.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
        }
    }

    private static class AppearanceHeroCardView extends FrameLayout {
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView subtitleView;

        AppearanceHeroCardView(Context context) {
            super(context);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = AndroidUtilities.dp(16);
            setLayoutParams(params);
            setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(22), Theme.getColor(Theme.key_windowBackgroundWhite)));
            setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(20), AndroidUtilities.dp(18));

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

    private class CardBackgroundDecoration extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Path path = new Path();
        private final float radius = AndroidUtilities.dp(22);
        private final float verticalPadding = AndroidUtilities.dp(4);
        private final float horizontalInset = AndroidUtilities.dp(2);
        private final float[] radii = new float[8];

        CardBackgroundDecoration() {
            paint.setStyle(Paint.Style.FILL);
        }

        @Override
        public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                int position = parent.getChildAdapterPosition(child);
                if (!isCardRow(position)) {
                    continue;
                }
                boolean top = !isCardRow(position - 1);
                boolean bottom = !isCardRow(position + 1);
                float left = child.getLeft() + horizontalInset;
                float right = child.getRight() - horizontalInset;
                float topY = child.getTop() - (top ? verticalPadding : 0);
                float bottomY = child.getBottom() + (bottom ? verticalPadding : 0);
                rect.set(left, topY, right, bottomY);
                setRadii(top, bottom);
                path.reset();
                path.addRoundRect(rect, radii, Path.Direction.CW);
                canvas.drawPath(path, paint);
            }
        }

        private void setRadii(boolean top, boolean bottom) {
            float topRadius = top ? radius : 0f;
            float bottomRadius = bottom ? radius : 0f;
            radii[0] = radii[1] = topRadius;
            radii[2] = radii[3] = topRadius;
            radii[4] = radii[5] = bottomRadius;
            radii[6] = radii[7] = bottomRadius;
        }

        private boolean isCardRow(int position) {
            if (position < 0 || position >= rows.size()) {
                return false;
            }
            RowType type = rows.get(position).type;
            switch (type) {
                case TEXT_CELL:
                case TEXT_CHECK:
                case TEXT_INFO_PRIVACY:
                case NOTIFICATIONS_CHECK:
                case CHAT_LIST_PREVIEW:
                case DOUBLE_TAP_CELL:
                case QUICK_SWITCHER:
                case STICKER_SIZE_PREVIEW:
                case STICKER_SIZE_SEEKBAR:
                case STICKER_RADIUS_SEEKBAR:
                    return true;
                default:
                    return false;
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class, BrightnessControlCell.class, ThemeTypeCell.class, ChatListCell.class, NotificationsCheckCell.class, ThemesHorizontalListCell.class, TextCell.class, PeerColorActivity.ChangeNameColorCell.class, SwipeGestureSettingsView.class, DefaultThemesPreviewCell.class, AppIconsSelectorCell.class, ChatListPreviewCell.class, DoubleTapCell.class, StickerSizePreviewMessagesCell.class, StickerSizeSeekBarCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
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
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));


        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));


        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));


        return themeDescriptions;
    }
}
