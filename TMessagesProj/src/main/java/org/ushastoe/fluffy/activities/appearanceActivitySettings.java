package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.LaunchActivity.getLastFragment;
import static org.ushastoe.fluffy.BulletinHelper.showRestartNotification;

import android.animation.AnimatorSet;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
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
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Reactions.ReactionsLayoutInBubble;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.SwipeGestureSettingsView;
import org.telegram.ui.DefaultThemesPreviewCell;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PeerColorActivity;
import org.telegram.ui.SelectAnimatedEmojiDialog;
import org.telegram.ui.ThemeActivity;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.activities.elements.ChatListPreviewCell;
import org.ushastoe.fluffy.activities.elements.DoubleTapCell;
import org.ushastoe.fluffy.activities.elements.StickerSizePreviewMessagesCell;
import org.ushastoe.fluffy.activities.elements.headerSettingsCell; // Не используется, можно удалить
import org.ushastoe.fluffy.fluffyConfig;

import java.util.ArrayList;
import java.util.Arrays;
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

    // Класс для представления строки
    private static class Row {
        RowType type;
        int id; // Уникальный идентификатор для каждой строки
        int textResId;
        int iconResId;
        int subtitleResId;
        // Дополнительные поля для различных типов строк

        Row(int id, RowType type, int textResId, int iconResId) {
            this.id = id;
            this.type = type;
            this.textResId = textResId;
            this.iconResId = iconResId;
        }

        Row(int id, RowType type, int textResId, int iconResId, int subtitleResId) {
            this(id, type, textResId, iconResId);
            this.subtitleResId = subtitleResId;
        }

        Row(int id, RowType type, int textResId) {
            this(id, type, textResId, 0);
        }

        Row(int id, RowType type) {
            this(id, type, 0, 0);
        }
    }

    private List<Row> rows = new ArrayList<>();

    private static final int ID_GENERAL_HEADER = 1;
    private static final int ID_CHAT_LIST_PREVIEW = 2;
    private static final int ID_CENTER_TITLE = 3;
    private static final int ID_STORIES_SHOW = 4;
    private static final int ID_SELECT_TITLE = 5;
    private static final int ID_SYSTEM_TYPEFACE = 6;
    private static final int ID_USE_SOLAR_ICONS = 7;
    private static final int ID_NEW_SWITCH_STYLE = 8;
    private static final int ID_DIVIDER_1 = 9;
    private static final int ID_MAIN_HEADER = 10;
    private static final int ID_ZODIAC_SHOW = 11;
    private static final int ID_DIVIDER_2 = 12;
    private static final int ID_CHAT_HEADER = 13;
    private static final int ID_DOUBLE_TAP = 14;
    private static final int ID_STICKER_SIZE = 15;
    private static final int ID_STICKER_SIZE_SEEKBAR = 16;
    private static final int ID_DISABLE_ROUND = 17;
    private static final int ID_CALL_SHOW = 18;
    private static final int ID_MORE_INFO = 19;
    private static final int ID_FORMAT_TIME_WITH_SECONDS = 20;
    private static final int ID_STICKER_TIME_STAMP = 21;
    private static final int ID_TRANSPARENCY = 22;
    private static final int ID_REMOVE_GIFTS = 23;
    private static final int ID_REMOVE_BUTTON = 24;
    private static final int ID_STICKER_HEADER = 25;
    private static final int ID_DIVIDER_3 = 26;
    private static final int ID_STICKER_RADIUS_SEEKBAR = 27;
    private static final int ID_DOUBLE_TAP_HEADER = 28;
    private static final int ID_DIVIDER_4 = 29;
    private static final int ID_QUICK_SWITCHER = 30;

    private static final int ID_MENU_CUSTOMIZATION = 31;

    private static final int stickerRaduisMax = 130;
    private Parcelable recyclerViewState = null;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
        updateRows(); // Обновляем строки при создании фрагмента

        return true;
    }

    // Метод для определения порядка и типов строк
    private void updateRows() {
        recyclerViewState = layoutManager != null ? layoutManager.onSaveInstanceState() : null;

        rows.clear();

        // ОБЩИЕ
        rows.add(new Row(ID_GENERAL_HEADER, RowType.HEADER, R.string.General));
        rows.add(new Row(ID_CHAT_LIST_PREVIEW, RowType.CHAT_LIST_PREVIEW));
        rows.add(new Row(ID_CENTER_TITLE, RowType.TEXT_CHECK, R.string.centerTitle, R.drawable.msg_contacts_name));
        rows.add(new Row(ID_STORIES_SHOW, RowType.TEXT_CHECK, R.string.storiesShower, R.drawable.menu_feature_stories));
        rows.add(new Row(ID_SELECT_TITLE, RowType.TEXT_CELL, R.string.TitleSelecter, R.drawable.menu_tag_rename));
        rows.add(new Row(ID_SYSTEM_TYPEFACE, RowType.TEXT_CHECK, R.string.UseSystemTypeface, R.drawable.msg_photo_text_framed));
        rows.add(new Row(ID_USE_SOLAR_ICONS, RowType.TEXT_CHECK, R.string.useSolarIcons, R.drawable.media_magic_cut));
        rows.add(new Row(ID_NEW_SWITCH_STYLE, RowType.TEXT_CHECK, R.string.NewMaterialSwith, R.drawable.msg_photo_switch2));
        rows.add(new Row(ID_DIVIDER_1, RowType.SHADOW_SECTION));

        // ПРОФИЛЬ
        rows.add(new Row(ID_MAIN_HEADER, RowType.HEADER, R.string.Profile));
        rows.add(new Row(ID_ZODIAC_SHOW, RowType.TEXT_CHECK, R.string.zodiacShow, R.drawable.msg_calendar2));
        rows.add(new Row(ID_DIVIDER_2, RowType.SHADOW_SECTION));

        // СТИКЕРЫ
        rows.add(new Row(ID_STICKER_HEADER, RowType.HEADER, R.string.Stickers));
        rows.add(new Row(ID_STICKER_SIZE_SEEKBAR, RowType.STICKER_SIZE_SEEKBAR));
        rows.add(new Row(ID_STICKER_RADIUS_SEEKBAR, RowType.STICKER_RADIUS_SEEKBAR));
        rows.add(new Row(ID_STICKER_TIME_STAMP, RowType.TEXT_CELL, R.string.TimestampSelecter, R.drawable.msg2_sticker));
        rows.add(new Row(ID_STICKER_SIZE, RowType.STICKER_SIZE_PREVIEW));
        rows.add(new Row(ID_DIVIDER_3, RowType.SHADOW_SECTION));

        // ДВОЙНОЕ НАЖАТИЕ
        rows.add(new Row(ID_DOUBLE_TAP_HEADER, RowType.HEADER, R.string.DoubleTapAction));
        rows.add(new Row(ID_DOUBLE_TAP, RowType.DOUBLE_TAP_CELL));
        rows.add(new Row(ID_QUICK_SWITCHER, RowType.QUICK_SWITCHER));
        rows.add(new Row(ID_MENU_CUSTOMIZATION, RowType.TEXT_CELL, R.string.ContextMenuSettings, R.drawable.msg_settings));
        rows.add(new Row(ID_DIVIDER_4, RowType.SHADOW_SECTION));

        // ЧАТЫ
        rows.add(new Row(ID_CHAT_HEADER, RowType.HEADER, R.string.Chats));
        rows.add(new Row(ID_DISABLE_ROUND, RowType.TEXT_CHECK, R.string.DisableNumberRounding, R.drawable.msg_archive_show, R.string.DisableNumberRoundingSubtitle));
        rows.add(new Row(ID_CALL_SHOW, RowType.TEXT_CHECK, R.string.callShower, R.drawable.calls_menu_phone));
        rows.add(new Row(ID_MORE_INFO, RowType.TEXT_CHECK, R.string.ExtendedStatusOnline, R.drawable.msg_contacts_time, R.string.ExtendedStatusOnlineSubtitle));
        rows.add(new Row(ID_FORMAT_TIME_WITH_SECONDS, RowType.TEXT_CHECK, R.string.formatTime, R.drawable.menu_premium_clock, R.string.formatTimeSubtitle));
        rows.add(new Row(ID_TRANSPARENCY, RowType.TEXT_CELL, R.string.Transparency, R.drawable.msg_blur_radial));
        rows.add(new Row(ID_REMOVE_GIFTS, RowType.TEXT_CHECK, R.string.HideGiftFromInput, R.drawable.filled_gift_simple));
        rows.add(new Row(ID_REMOVE_BUTTON, RowType.TEXT_CHECK, R.string.HideFloatingButton, R.drawable.msg_openin));

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
            if (recyclerViewState != null) {
                layoutManager.onRestoreInstanceState(recyclerViewState);
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
                context.getString(R.string.save_for_notifications),
                () -> fluffyConfig.showSaveForNotifications,
                () -> fluffyConfig.showSaveForNotifications = !fluffyConfig.showSaveForNotifications
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.gemini_ai_header),
                () -> fluffyConfig.showGemini,
                () -> fluffyConfig.showGemini = !fluffyConfig.showGemini
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.reply),
                () -> fluffyConfig.showReply,
                () -> fluffyConfig.showReply = !fluffyConfig.showReply
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.copy_photo),
                () -> fluffyConfig.showCopyPhoto,
                () -> fluffyConfig.showCopyPhoto = !fluffyConfig.showCopyPhoto
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.copy_photo_as_sticker),
                () -> fluffyConfig.showCopyPhotoAsSticker,
                () -> fluffyConfig.showCopyPhotoAsSticker = !fluffyConfig.showCopyPhotoAsSticker
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.clear_from_cache),
                () -> fluffyConfig.showClearFromCache,
                () -> fluffyConfig.showClearFromCache = !fluffyConfig.showClearFromCache
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.forward),
                () -> fluffyConfig.showForward,
                () -> fluffyConfig.showForward = !fluffyConfig.showForward
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.forward_wo_author),
                () -> fluffyConfig.showForwardWoAuthorship,
                () -> fluffyConfig.showForwardWoAuthorship = !fluffyConfig.showForwardWoAuthorship
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.view_user_history),
                () -> fluffyConfig.showViewHistory,
                () -> fluffyConfig.showViewHistory = !fluffyConfig.showViewHistory
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.save_message),
                () -> fluffyConfig.showSaveMessage,
                () -> fluffyConfig.showSaveMessage = !fluffyConfig.showSaveMessage
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.report_chat),
                () -> fluffyConfig.showReport,
                () -> fluffyConfig.showReport = !fluffyConfig.showReport
        ));

        menuItems.add(new MenuItemConfig(
                context.getString(R.string.json),
                () -> fluffyConfig.showJSON,
                () -> fluffyConfig.showJSON = !fluffyConfig.showJSON
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

        AlertDialog dialog = new AlertDialog.Builder(getParentActivity())
                .setTitle(getString(R.string.ContextMenuSettings)) // Используем строку из ресурсов
                .setView(linearLayout)
                .setPositiveButton(getString("Close", R.string.Close), null)
                .create();

        showDialog(dialog);
    }

    // Метод для поиска строки по идентификатору
    private Row getRowById(int id) {
        for (Row row : rows) {
            if (row.id == id) {
                return row;
            }
        }
        return null; // Должно быть unreachable при правильном использовании
    }

    // Метод для поиска позиции строки по идентификатору
    private int getRowPositionById(int id) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id == id) {
                return i;
            }
        }
        return -1; // Строка не найдена
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

        listView.setOnItemClickListener((view, position) -> {
            Row row = rows.get(position);
            handleItemClick(row.id, view, context);
        });

        return fragmentView;
    }

    // Метод для обработки кликов по элементам списка
    private void handleItemClick(int rowId, View view, Context context) {
        switch (rowId) {
            case ID_ZODIAC_SHOW:
                fluffyConfig.toogleZodiacShow();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.zodiacShow);
                }
                break;
            case ID_STORIES_SHOW:
                fluffyConfig.toggleShowStories();
                if (chatListPreviewCell != null) {
                    chatListPreviewCell.updateStories(true);
                }
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.showStories);
                }
                break;
            case ID_CALL_SHOW:
                fluffyConfig.toggleShowCallIcon();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.showCallIcon);
                }
                break;
            case ID_CENTER_TITLE:
                fluffyConfig.toggleCenterTitle();
                if (chatListPreviewCell != null) {
                    chatListPreviewCell.updateCenteredTitle(true);
                }
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.centerTitle);
                }
                break;
            case ID_DISABLE_ROUND:
                fluffyConfig.toogleRoundingNumber();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.disableRoundingNumber);
                }
                break;
            case ID_REMOVE_GIFTS:
                fluffyConfig.toggleGiftSwitch();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.hideGift);
                }
                break;
            case ID_REMOVE_BUTTON:
                fluffyConfig.togglehideButtonWrite();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.hideButtonWrite);
                }
                break;
            case ID_MORE_INFO:
                fluffyConfig.toggleMoreInfoOnline();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.moreInfoOnline);
                }
                break;
            case ID_NEW_SWITCH_STYLE:
                fluffyConfig.toogleNewSwitchStyle();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.newSwitchStyle);
                }
                // Обновляем строки, если тип Switch меняется
                updateRows();
                break;
            case ID_SYSTEM_TYPEFACE:
                fluffyConfig.toogleUseSystemFonts();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.useSystemFonts);
                }
                break;
            case ID_USE_SOLAR_ICONS:
                fluffyConfig.toggleUseSolarIcons();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.useSolarIcons);
                }
                break;
            case ID_FORMAT_TIME_WITH_SECONDS:
                fluffyConfig.toogleFormatTimeWithSeconds();
                if (view instanceof TextCell) {
                    ((TextCell) view).setChecked(fluffyConfig.formatTimeWithSeconds);
                }
                break;
            case ID_SELECT_TITLE:
                titleSelecter(context);
                break;
            case ID_DOUBLE_TAP:
                selectorReaction();
                break;
            case ID_QUICK_SWITCHER:
                if (view instanceof SetDefaultReactionCell) {
                    showSelectStatusDialog((SetDefaultReactionCell) view);
                }
                break;
            case ID_STICKER_TIME_STAMP:
                timeStampSelecter(context);
                break;
            case ID_TRANSPARENCY:
                showTransparencyDialog(context);
                break;
            case ID_MENU_CUSTOMIZATION:
                showMenuItemConfigurator(context);
                break;
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
                    int oldIndex = types.indexOf(old);
                    if (oldIndex != -1) { // Проверка на существование старого значения в types
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
        showDialog(builder.create());
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
                getNotificationCenter().postNotificationName(NotificationCenter.currentUserPremiumStatusChanged); // Убедитесь, что это действительно нужно
                int position = getRowPositionById(ID_SELECT_TITLE);
                if (position != -1) {
                    RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(position);
                    if (holder != null) {
                        listAdapter.onBindViewHolder(holder, position);
                    }
                }


                if (LaunchActivity.getSafeLastFragment() != null) {
                    showRestartNotification(LaunchActivity.getSafeLastFragment());
                }
                getNotificationCenter().postNotificationName(NotificationCenter.currentUserPremiumStatusChanged); // Убедитесь, что это действительно нужно
                dialogRef.get().dismiss();
                if (chatListPreviewCell != null) {
                    chatListPreviewCell.updateTitle(true);
                }
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

    private void showTransparencyDialog(Context context) {
        if (getParentActivity() == null) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.Transparency)); // Заголовок диалога
        builder.setMessage(getString(R.string.EnterValueBetween0And255)); // Информационное сообщение

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER); // Устанавливаем ввод только цифр
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)}); // Ограничиваем ввод 3 символами
        input.setHint("0-255"); // Подсказка

        // Устанавливаем текущее значение в поле ввода
        input.setText(String.valueOf(fluffyConfig.transparency));

        builder.setView(input);

        builder.setPositiveButton(getString(R.string.OK), (dialog, which) -> {
            try {
                int value = Integer.parseInt(input.getText().toString());
                if (value >= 0 && value <= 255) {
                    fluffyConfig.setTransparency(value); // Устанавливаем значение в fluffyConfig
                    showRestartNotification(LaunchActivity.getSafeLastFragment());
                    // Опционально: обновить текст в ячейке списка, если нужно

                    int position = getRowPositionById(ID_TRANSPARENCY);
                    if (position != -1) {
                        RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(position);
                        if (holder != null) {
                            listAdapter.onBindViewHolder(holder, position);
                        }
                    }

                } else {
                    BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.InvalidValue), getString(R.string.EnterValueBetween0And255));
                }
            } catch (NumberFormatException e) {
                BulletinHelper.showSimpleBulletin(LaunchActivity.getSafeLastFragment(), getString(R.string.InvalidInput), getString(R.string.PleaseEnterNumber));
            }
        });

        builder.setNegativeButton(getString(R.string.Cancel), (dialog, which) -> dialog.cancel());

        showDialog(builder.create());
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
            cell.setTextAndValue(items[index], index == fluffyConfig.readSticker);
            cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), Theme.RIPPLE_MASK_ALL));
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                fluffyConfig.setReadSticker(index);
                int position = getRowPositionById(ID_STICKER_TIME_STAMP);
                if (position != -1) {
                    RecyclerView.ViewHolder holder = listView.findViewHolderForAdapterPosition(position);
                    if (holder != null) {
                        listAdapter.onBindViewHolder(holder, position);
                    }
                }


                dialogRef.get().dismiss();
                if (chatListPreviewCell != null) {
                    chatListPreviewCell.updateTitle(true); // Возможно, это не влияет на chatListPreviewCell, проверьте
                }
                if (stickerSizePreview != null) {
                    stickerSizePreview.invalidate();
                    stickerSizePreview.rebuildStickerPreview();
                } else {
                    Log.w(TAG, "stickerSizePreview is null, can't invalidate");
                }
            });
        }

        Dialog dialog = new AlertDialog.Builder(getParentActivity())
                .setTitle(getString(R.string.TimestampSelecter))
                .setView(linearLayout)
                .setNegativeButton(getString(R.string.Cancel), null)
                .create();
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

            setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));

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
                // Сохраняем выбранную анимированную реакцию
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
                // Сохраняем выбранную обычную реакцию
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

            // HORIZONTAL LAYOUT FOR TITLE + VALUE
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

            // Добавить горизонтальный layout в корень
            addView(hLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 0, 8, 0, 0));

            // SeekBar ниже
            seekBarView = new SeekBarView(context);
            seekBarView.setReportChanges(true);

            seekBarView.setDelegate((stop, progress) -> {
                int value = (int) (5 + 15 * progress); // Диапазон 5–20
                valueView.setText(String.format(Locale.US, "%d", value));
                // (твой остальной код)
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

            // HORIZONTAL LAYOUT FOR TITLE + VALUE
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
                case SHADOW_SECTION:
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case NOTIFICATIONS_CHECK:
                    // Логика для NotificationsCheckCell (если используется)
                    // NotificationsCheckCell checkCell = (NotificationsCheckCell) holder.itemView;
                    // checkCell.setTextAndValueAndIconAndCheck(getString(R.string.InappBrowser), getString(R.string.InappBrowserInfo), R.drawable.msg2_language, SharedConfig.inappBrowser, 0, false, true);
                    break;
                case HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(getString(row.textResId));
                    break;
                case TEXT_INFO_PRIVACY:
                    TextInfoPrivacyCell textInfoPrivacyCell = (TextInfoPrivacyCell) holder.itemView;
                    textInfoPrivacyCell.setText(getString(row.textResId));
                    break;
                case CHAT_LIST_PREVIEW:
                    chatListPreviewCell = (ChatListPreviewCell) holder.itemView;
                    break;
                case TEXT_CELL:
                    TextCell textCell6 = (TextCell) holder.itemView;
                    String value = "";
                    switch (row.id) {
                        case ID_SELECT_TITLE:
                            value = switch (fluffyConfig.typeTitle) {
                                case 0 -> fluffyConfig.getUsername();
                                case 1 -> "fluffy";
                                case 2 -> "telegram";
                                case 3 -> getString(R.string.Disable);
                                default -> LocaleController.getString(R.string.AppName);
                            };
                            break;
                        case ID_STICKER_TIME_STAMP:
                            value = switch (fluffyConfig.readSticker) {
                                case 0 -> getString(R.string.TimeWithReadStatus);
                                case 1 -> getString(R.string.ReadStatus);
                                case 2 -> getString(R.string.None);
                                default -> getString(R.string.None);
                            };
                            break;
                        case ID_TRANSPARENCY:
                            value = String.valueOf(fluffyConfig.transparency);
                            break;
                        // Для новой кнопки настройки меню значение справа не нужно, поэтому value останется пустым.
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
                    int currentSize = fluffyConfig.getStickerSize();
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
                    int currentRadius = fluffyConfig.getStickerRadius();
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
                        case ID_ZODIAC_SHOW:
                            checked = fluffyConfig.zodiacShow;
                            break;
                        case ID_STORIES_SHOW:
                            checked = fluffyConfig.showStories;
                            break;
                        case ID_CALL_SHOW:
                            checked = fluffyConfig.showCallIcon;
                            break;
                        case ID_CENTER_TITLE:
                            checked = fluffyConfig.centerTitle;
                            break;
                        case ID_SYSTEM_TYPEFACE:
                            checked = fluffyConfig.useSystemFonts;
                            break;
                        case ID_USE_SOLAR_ICONS:
                            checked = fluffyConfig.useSolarIcons;
                            break;
                        case ID_DISABLE_ROUND:
                            checked = fluffyConfig.disableRoundingNumber;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                        case ID_REMOVE_GIFTS:
                            checked = fluffyConfig.hideGift;
                            break;
                        case ID_REMOVE_BUTTON:
                            checked = fluffyConfig.hideButtonWrite;
                            break;
                        case ID_MORE_INFO:
                            checked = fluffyConfig.moreInfoOnline;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                        case ID_NEW_SWITCH_STYLE:
                            checked = fluffyConfig.newSwitchStyle;
                            break;
                        case ID_FORMAT_TIME_WITH_SECONDS:
                            checked = fluffyConfig.formatTimeWithSeconds;
                            if (row.subtitleResId != 0) {
                                subtitle = getString(row.subtitleResId);
                            }
                            break;
                    }
                    textCell.setTextAndCheckAndIcon(getString(row.textResId), checked, row.iconResId, true);

                    textCell.setSubtitle(subtitle);
                    break;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            Row row = rows.get(holder.getAdapterPosition());
            // SeekBar и предпросмотр не должны быть кликабельными
            if (row.type == RowType.STICKER_SIZE_PREVIEW || row.type == RowType.STICKER_SIZE_SEEKBAR || row.type == RowType.STICKER_RADIUS_SEEKBAR) {
                return false;
            }
            return row.type != RowType.SHADOW_SECTION && row.type != RowType.HEADER && row.type != RowType.TEXT_INFO_PRIVACY;
        }

        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 1:
                    view = new TextCell(mContext, 0, false, true, null);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
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
                    break;
                case 8:
                    view = new StickerSizePreviewMessagesCell(mContext, appearanceActivitySettings.this);
                    break;
                case 9:
                    view = new StickerSizeSeekBarCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 10:
                    view = new StickerRadiusSeekBarCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 11:
                    view = new SetDefaultReactionCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new View(mContext); // Заглушка
                    break;
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
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
                default: return -1; // Неизвестный тип
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, BrightnessControlCell.class, ThemeTypeCell.class, ChatListCell.class, NotificationsCheckCell.class, ThemesHorizontalListCell.class, TextCell.class, PeerColorActivity.ChangeNameColorCell.class, SwipeGestureSettingsView.class, DefaultThemesPreviewCell.class, AppIconsSelectorCell.class, ChatListPreviewCell.class, DoubleTapCell.class, StickerSizePreviewMessagesCell.class, StickerSizeSeekBarCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
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


        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));


        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));

        // Добавление ThemeDescription для новой ячейки
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeSeekBarCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeSeekBarCell.class}, new String[]{"seekBarView"}, null, null, null, Theme.key_player_progressBackground));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeSeekBarCell.class}, new String[]{"seekBarView"}, null, null, null, Theme.key_player_progress));

        return themeDescriptions;
    }
}