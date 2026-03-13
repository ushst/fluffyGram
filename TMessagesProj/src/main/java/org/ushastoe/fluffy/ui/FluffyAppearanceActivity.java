package org.ushastoe.fluffy.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;
import org.ushastoe.fluffy.hooks.DialogsAppTitleHook;
import org.ushastoe.fluffy.patches.AppFontPatch;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.ui.components.DialogsListSizeCell;
import org.ushastoe.fluffy.ui.components.DialogsListPreviewCell;
import org.ushastoe.fluffy.ui.components.DoubleTapEditPreviewCell;

import java.util.ArrayList;

public class FluffyAppearanceActivity extends BaseFragment {

    private static final String ARG_TARGET = "fluffy_appearance_target";

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_TEXT = 2;
    private static final int VIEW_TYPE_DOUBLE_TAP_PREVIEW = 3;
    private static final int VIEW_TYPE_SHADOW = 4;
    private static final int VIEW_TYPE_DIALOGS_LIST_SIZE = 5;
    private static final int VIEW_TYPE_DIALOGS_LIST_PREVIEW = 6;

    private static final int ROW_TITLES_HEADER = 0;
    private static final int ROW_DIALOGS_TITLE_MODE = 1;
    private static final int ROW_DIALOGS_APP_TITLE = 2;
    private static final int ROW_APP_FONT = 3;
    private static final int ROW_IMPORT_FONT = 4;
    private static final int ROW_DOUBLE_TAP_SECTION = 5;
    private static final int ROW_DOUBLE_TAP_HEADER = 6;
    private static final int ROW_DOUBLE_TAP_EDIT_PREVIEW = 7;
    private static final int ROW_FORMATTING_SECTION = 8;
    private static final int ROW_FORMATTING_HEADER = 9;
    private static final int ROW_TIME_WITH_SECONDS = 10;
    private static final int ROW_DISABLE_ROUNDED_NUMBERS = 11;
    private static final int ROW_THOUSANDS_SEPARATOR = 12;
    private static final int ROW_CHAT_LIST_SECTION = 13;
    private static final int ROW_CHAT_LIST_HEADER = 14;
    private static final int ROW_DIALOGS_LIST_SIZE = 15;
    private static final int ROW_DIALOGS_LIST_PREVIEW = 16;
    private static final int ROW_CHAT_UI_SECTION = 17;
    private static final int ROW_CHAT_UI_HEADER = 18;
    private static final int ROW_TABS = 19;
    private static final int ROW_CENTER_CHAT_HEADER = 20;
    private static final int ROW_MAP_PROVIDER = 21;
    private static final int ROW_HIDE_CHANNEL_POST_STARS_OFFER = 22;
    private static final int ROW_NOTIFICATION_ICON = 23;
    private static final int ROW_RECORDER_SECTION = 24;
    private static final int ROW_RECORDER_HEADER = 25;
    private static final int ROW_ROUND_VIDEO_CAMERA_FEATURE = 26;
    private static final int ROW_ROUND_VIDEO_CAMERA = 27;

    private static final int REQUEST_CODE_PICK_FONT = 4201;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    public FluffyAppearanceActivity() {
        super();
    }

    public FluffyAppearanceActivity(Bundle args) {
        super(args);
    }

    public static FluffyAppearanceActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffyAppearanceActivity(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyAppearance));
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
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            ItemInner item = items.get(position);
            if (item.id == ROW_HIDE_CHANNEL_POST_STARS_OFFER) {
                boolean hidden = !AppearanceSettingsHook.isChannelPostStarsOfferHidden();
                AppearanceSettingsHook.setChannelPostStarsOfferHidden(hidden);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(hidden);
                }
            } else if (item.id == ROW_NOTIFICATION_ICON) {
                boolean enabled = !AppearanceSettingsHook.useFluffyNotificationIcon();
                AppearanceSettingsHook.setUseFluffyNotificationIcon(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_TIME_WITH_SECONDS) {
                boolean enabled = !AppearanceSettingsHook.isTimeWithSecondsEnabled();
                AppearanceSettingsHook.setTimeWithSecondsEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
                onFormattingChanged();
            } else if (item.id == ROW_DISABLE_ROUNDED_NUMBERS) {
                boolean enabled = !AppearanceSettingsHook.isRoundedNumbersDisabled();
                AppearanceSettingsHook.setRoundedNumbersDisabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
                onFormattingChanged();
            } else if (item.id == ROW_THOUSANDS_SEPARATOR) {
                boolean enabled = !AppearanceSettingsHook.isThousandsSeparatorEnabled();
                AppearanceSettingsHook.setThousandsSeparatorEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
                onFormattingChanged();
            } else if (item.id == ROW_CENTER_CHAT_HEADER) {
                boolean enabled = !AppearanceSettingsHook.isCenterChatHeaderEnabled();
                AppearanceSettingsHook.setCenterChatHeaderEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
            } else if (item.id == ROW_MAP_PROVIDER) {
                showMapProviderDialog();
            } else if (item.id == ROW_ROUND_VIDEO_CAMERA_FEATURE) {
                boolean enabled = !AppearanceSettingsHook.isRoundVideoCameraFeatureEnabled();
                AppearanceSettingsHook.setRoundVideoCameraFeatureEnabled(enabled);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(enabled);
                }
                updateItems();
            } else if (item.id == ROW_ROUND_VIDEO_CAMERA) {
                showRoundVideoCameraDialog();
            } else if (item.id == ROW_APP_FONT) {
                showAppFontDialog();
            } else if (item.id == ROW_IMPORT_FONT) {
                startFontImport();
            } else if (item.id == ROW_TABS) {
                presentFragment(new FluffyTabsActivity());
            } else if (item.id == ROW_DIALOGS_TITLE_MODE) {
                showDialogsTitleModeDialog();
            } else if (item.id == ROW_DIALOGS_APP_TITLE) {
                showDialogsAppTitleDialog();
            }
        });
        listView.setOnItemLongClickListener((view, position) -> copyDeepLinkForPosition(position));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();
        applyTargetScroll();

        fragmentView = frameLayout;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateItems();
        applyTargetScroll();
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_TITLES_HEADER,
                LocaleController.getString(R.string.FluffyAppearanceTitlesSection), false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DIALOGS_TITLE_MODE,
                LocaleController.getString(R.string.FluffyCenterDialogsTitle),
                false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DIALOGS_APP_TITLE,
                LocaleController.getString(R.string.FluffyDialogsAppTitle),
                false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_APP_FONT,
                LocaleController.getString(R.string.FluffyAppFont),
                false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_IMPORT_FONT,
                LocaleController.getString(R.string.FluffyImportFont),
                false));

        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_DOUBLE_TAP_SECTION, "", false));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_DOUBLE_TAP_HEADER,
                LocaleController.getString(R.string.FluffyDoubleTapSection), false));
        items.add(new ItemInner(VIEW_TYPE_DOUBLE_TAP_PREVIEW, ROW_DOUBLE_TAP_EDIT_PREVIEW, "", false));

        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_FORMATTING_SECTION, "", false));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_FORMATTING_HEADER,
                LocaleController.getString(R.string.FluffyAppearanceFormattingSection), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_TIME_WITH_SECONDS,
                LocaleController.getString(R.string.FluffyTimeWithSeconds),
                AppearanceSettingsHook.isTimeWithSecondsEnabled()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_DISABLE_ROUNDED_NUMBERS,
                LocaleController.getString(R.string.FluffyDisableRoundedNumbers),
                AppearanceSettingsHook.isRoundedNumbersDisabled()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_THOUSANDS_SEPARATOR,
                LocaleController.getString(R.string.FluffyThousandsSeparator),
                AppearanceSettingsHook.isThousandsSeparatorEnabled()));

        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_CHAT_LIST_SECTION, "", false));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_CHAT_LIST_HEADER,
                LocaleController.getString(R.string.FluffyAppearanceChatListSection), false));
        items.add(new ItemInner(VIEW_TYPE_DIALOGS_LIST_SIZE, ROW_DIALOGS_LIST_SIZE,
                LocaleController.getString(R.string.FluffyDialogsListSize),
                false));
        items.add(new ItemInner(VIEW_TYPE_DIALOGS_LIST_PREVIEW, ROW_DIALOGS_LIST_PREVIEW, "", false));

        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_CHAT_UI_SECTION, "", false));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_CHAT_UI_HEADER,
                LocaleController.getString(R.string.FluffyAppearanceChatSection), false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_TABS,
                LocaleController.getString(R.string.FluffyTabs),
                false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_CENTER_CHAT_HEADER,
                LocaleController.getString(R.string.FluffyCenterChatHeader),
                AppearanceSettingsHook.isCenterChatHeaderEnabled()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_MAP_PROVIDER,
                LocaleController.getString(R.string.FluffyMapProvider),
                false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_HIDE_CHANNEL_POST_STARS_OFFER,
                LocaleController.getString(R.string.FluffyHideChannelPostStarsOffer),
                AppearanceSettingsHook.isChannelPostStarsOfferHidden()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_NOTIFICATION_ICON,
                LocaleController.getString(R.string.FluffyNotificationIcon),
                AppearanceSettingsHook.useFluffyNotificationIcon()));

        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_RECORDER_SECTION, "", false));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_RECORDER_HEADER,
                LocaleController.getString(R.string.FluffyAppearanceRecorderSection), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_ROUND_VIDEO_CAMERA_FEATURE,
                LocaleController.getString(R.string.FluffyRoundVideoCameraFeature),
                AppearanceSettingsHook.isRoundVideoCameraFeatureEnabled()));
        if (AppearanceSettingsHook.isRoundVideoCameraFeatureEnabled()) {
            items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_ROUND_VIDEO_CAMERA,
                    LocaleController.getString(R.string.FluffyRoundVideoCamera),
                    false));
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showDialogsTitleModeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[] {
                LocaleController.getString(R.string.FluffyDialogsTitleModeDefault),
                LocaleController.getString(R.string.FluffyDialogsTitleModeCentered),
                LocaleController.getString(R.string.FluffyDialogsTitleModeCenteredIgnoreActions)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyCenterDialogsTitle));
        builder.setItems(items, (dialog, which) -> {
            AppearanceSettingsHook.setDialogsTitleMode(which);
            updateItems();
        });
        showDialog(builder.create());
    }

    private CharSequence getDialogsTitleModeValue() {
        int mode = AppearanceSettingsHook.getDialogsTitleMode();
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_CENTERED) {
            return LocaleController.getString(R.string.FluffyDialogsTitleModeCentered);
        }
        if (mode == AppearanceSettingsPatch.DIALOGS_TITLE_MODE_CENTERED_IGNORE_ACTIONS) {
            return LocaleController.getString(R.string.FluffyDialogsTitleModeCenteredIgnoreActions);
        }
        return LocaleController.getString(R.string.FluffyDialogsTitleModeDefault);
    }

    private void showDialogsAppTitleDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] titleItems = new CharSequence[] {
                LocaleController.getString(R.string.FluffyDialogsAppTitleOptionFluffyGram),
                LocaleController.getString(R.string.FluffyDialogsAppTitleOptionFluffy),
                LocaleController.getString(R.string.FluffyDialogsAppTitleOptionTelegram),
                LocaleController.getString(R.string.FluffyDialogsAppTitleOptionUsername),
                LocaleController.getString(R.string.FluffyDialogsAppTitleOptionFirstName),
                LocaleController.getString(R.string.FluffyDialogsAppTitleOptionCustom)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyDialogsAppTitle));
        builder.setItems(titleItems, (dialog, which) -> {
            if (which == AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_CUSTOM) {
                showCustomDialogsAppTitleDialog();
                return;
            }
            AppearanceSettingsHook.setDialogsAppTitleMode(which);
            updateItems();
        });
        showDialog(builder.create());
    }

    private void showCustomDialogsAppTitleDialog() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }

        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_dialogInputField), Theme.getColor(Theme.key_dialogInputFieldActivated), Theme.getColor(Theme.key_text_RedBold));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHint(LocaleController.getString(R.string.FluffyDialogsAppTitleCustomHint));
        editText.setMaxLines(1);
        editText.setLines(1);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setGravity(Gravity.LEFT | Gravity.TOP);
        editText.setSingleLine(true);
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setPadding(0, AndroidUtilities.dp(4), 0, 0);

        String currentValue = AppearanceSettingsHook.getDialogsAppTitleCustom();
        if (currentValue != null) {
            editText.setText(currentValue);
            editText.setSelection(editText.length());
        }

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView message = new TextView(context);
        message.setText(LocaleController.getString(R.string.FluffyDialogsAppTitleCustomText));
        message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        message.setPadding(AndroidUtilities.dp(23), AndroidUtilities.dp(12), AndroidUtilities.dp(23), AndroidUtilities.dp(6));
        message.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        linearLayout.addView(message, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyDialogsAppTitleOptionCustom));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString(R.string.Done), (dialog, which) -> {
            AppearanceSettingsHook.setDialogsAppTitleCustom(editText.getText().toString());
            AppearanceSettingsHook.setDialogsAppTitleMode(AppearanceSettingsPatch.DIALOGS_APP_TITLE_MODE_CUSTOM);
            updateItems();
        });
        builder.setView(linearLayout);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 50));
        showDialog(dialog);
    }

    private CharSequence getDialogsAppTitleValue() {
        return DialogsAppTitleHook.getDialogsAppTitle(UserConfig.selectedAccount);
    }

    private void showDoubleTapActionDialog(boolean outgoing) {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        final int[] actions = new int[] {
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_NONE,
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REACTION,
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REPLY,
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_COPY,
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_FORWARD,
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_EDIT,
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_SAVE,
                AppearanceSettingsPatch.DOUBLE_TAP_ACTION_DELETE
        };
        CharSequence[] items = new CharSequence[actions.length];
        int currentAction = outgoing ? AppearanceSettingsHook.getDoubleTapOutAction() : AppearanceSettingsHook.getDoubleTapInAction();
        for (int i = 0; i < actions.length; i++) {
            CharSequence title = getDoubleTapActionLabel(actions[i]);
            items[i] = actions[i] == currentAction ? "\u2713 " + title : title;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, getResourceProvider());
        builder.setTitle(LocaleController.getString(outgoing ? R.string.FluffyDoubleTapOutgoing : R.string.FluffyDoubleTapIncoming));
        builder.setItems(items, (dialog, which) -> {
            int action = actions[which];
            if (outgoing) {
                AppearanceSettingsHook.setDoubleTapOutAction(action);
            } else {
                AppearanceSettingsHook.setDoubleTapInAction(action);
            }
            updateItems();
        });
        showDialog(builder.create());
    }

    private CharSequence getDoubleTapActionLabel(int action) {
        switch (action) {
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_NONE:
                return LocaleController.getString(R.string.FluffyDoubleTapActionNone);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REPLY:
                return LocaleController.getString(R.string.FluffyDoubleTapActionReply);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_COPY:
                return LocaleController.getString(R.string.FluffyDoubleTapActionCopy);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_FORWARD:
                return LocaleController.getString(R.string.FluffyDoubleTapActionForward);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_EDIT:
                return LocaleController.getString(R.string.FluffyDoubleTapActionEdit);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_SAVE:
                return LocaleController.getString(R.string.FluffyDoubleTapActionSave);
            case AppearanceSettingsPatch.DOUBLE_TAP_ACTION_DELETE:
                return LocaleController.getString(R.string.FluffyDoubleTapActionDelete);
            default:
                return LocaleController.getString(R.string.FluffyDoubleTapActionReaction);
        }
    }

    private void showRoundVideoCameraDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[] {
                LocaleController.getString(R.string.FluffyRoundVideoCameraFront),
                LocaleController.getString(R.string.FluffyRoundVideoCameraBack)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyRoundVideoCamera));
        builder.setItems(items, (dialog, which) -> {
            AppearanceSettingsHook.setDefaultRoundVideoCameraMode(which);
            updateItems();
        });
        showDialog(builder.create());
    }

    private CharSequence getRoundVideoCameraValue() {
        return AppearanceSettingsHook.getDefaultRoundVideoCameraMode() == AppearanceSettingsPatch.ROUND_VIDEO_CAMERA_BACK
                ? LocaleController.getString(R.string.FluffyRoundVideoCameraBack)
                : LocaleController.getString(R.string.FluffyRoundVideoCameraFront);
    }

    private void showMapProviderDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] items = new CharSequence[] {
                LocaleController.getString(R.string.FluffyMapProviderOpenStreetMap),
                LocaleController.getString(R.string.FluffyMapProviderGoogle)
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyMapProvider));
        builder.setItems(items, (dialog, which) -> {
            AppearanceSettingsHook.setMapProvider(which == 0
                    ? AppearanceSettingsPatch.MAP_PROVIDER_OPENSTREETMAP
                    : AppearanceSettingsPatch.MAP_PROVIDER_GOOGLE);
            updateItems();
        });
        showDialog(builder.create());
    }

    private CharSequence getMapProviderValue() {
        return AppearanceSettingsHook.getMapProvider() == AppearanceSettingsPatch.MAP_PROVIDER_OPENSTREETMAP
                ? LocaleController.getString(R.string.FluffyMapProviderOpenStreetMap)
                : LocaleController.getString(R.string.FluffyMapProviderGoogle);
    }

    private void showAppFontDialog() {
        Activity activity = getParentActivity();
        if (activity == null) {
            return;
        }
        ArrayList<String> availableFonts = AppFontPatch.getAvailableFonts();
        CharSequence[] dialogItems = new CharSequence[availableFonts.size() + 1];
        dialogItems[0] = LocaleController.getString(R.string.FluffyAppFontDefault);
        for (int i = 0; i < availableFonts.size(); i++) {
            dialogItems[i + 1] = AppFontPatch.getFontDisplayName(availableFonts.get(i));
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyAppFont));
        builder.setItems(dialogItems, (dialog, which) -> {
            if (which == 0) {
                AppearanceSettingsHook.setAppFontKey("");
            } else {
                AppearanceSettingsHook.setAppFontKey(availableFonts.get(which - 1));
            }
            onFontChanged();
        });
        showDialog(builder.create());
    }

    private void startFontImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "font/*",
                "font/ttf",
                "font/otf",
                "application/x-font-ttf",
                "application/x-font-opentype",
                "application/font-sfnt",
                "application/octet-stream"
        });
        startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.FluffyImportFont)), REQUEST_CODE_PICK_FONT);
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_PICK_FONT) {
            return;
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyImportFontFailed)).show();
            return;
        }
        try {
            String importedFontKey = AppFontPatch.importFont(getParentActivity(), uri);
            AppearanceSettingsHook.setAppFontKey(importedFontKey);
            BulletinFactory.of(this).createSuccessBulletin(LocaleController.formatString("FluffyImportFontSuccess", R.string.FluffyImportFontSuccess, AppFontPatch.getFontDisplayName(importedFontKey))).show();
            onFontChanged();
        } catch (Exception e) {
            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.FluffyImportFontFailed)).show();
        }
    }

    private void onFontChanged() {
        updateItems();
        if (getParentLayout() != null) {
            getParentLayout().rebuildAllFragmentViews(true, true);
        }
    }

    private void onFormattingChanged() {
        LocaleController.getInstance().recreateFormatters();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
        updateItems();
        if (getParentLayout() != null) {
            getParentLayout().rebuildAllFragmentViews(true, true);
        }
    }

    private void onDialogsListSizeChanged(int scale, boolean finalChange) {
        AppearanceSettingsHook.setDialogsListScale(scale);
        Theme.createDialogsResources(ApplicationLoader.applicationContext);
        if (adapter != null) {
            int index = findItemIndexById(ROW_DIALOGS_LIST_PREVIEW);
            if (index >= 0) {
                adapter.notifyItemChanged(index);
            }
        }
    }

    private int findItemIndexById(int id) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == id) {
                return i;
            }
        }
        return -1;
    }

    private boolean copyDeepLinkForPosition(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        return FluffySettingsDeepLinkPatch.copyLink(this, getDeepLinkForItem(items.get(position)));
    }

    private String getDeepLinkForItem(ItemInner item) {
        if (item == null) {
            return null;
        }
        switch (item.id) {
            case ROW_TITLES_HEADER:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "titles");
            case ROW_DIALOGS_TITLE_MODE:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "dialogs-title-mode");
            case ROW_DIALOGS_APP_TITLE:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "dialogs-app-title");
            case ROW_APP_FONT:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "app-font");
            case ROW_IMPORT_FONT:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "import-font");
            case ROW_DOUBLE_TAP_HEADER:
            case ROW_DOUBLE_TAP_EDIT_PREVIEW:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "double-tap");
            case ROW_FORMATTING_HEADER:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "formatting");
            case ROW_TIME_WITH_SECONDS:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "time-with-seconds");
            case ROW_DISABLE_ROUNDED_NUMBERS:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "disable-rounded-numbers");
            case ROW_THOUSANDS_SEPARATOR:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "thousands-separator");
            case ROW_CHAT_LIST_HEADER:
            case ROW_DIALOGS_LIST_PREVIEW:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "chat-list");
            case ROW_DIALOGS_LIST_SIZE:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "chat-list-size");
            case ROW_CHAT_UI_HEADER:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "chat-ui");
            case ROW_TABS:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "tabs");
            case ROW_CENTER_CHAT_HEADER:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "center-chat-header");
            case ROW_MAP_PROVIDER:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "map-provider");
            case ROW_HIDE_CHANNEL_POST_STARS_OFFER:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "hide-channel-post-stars-offer");
            case ROW_NOTIFICATION_ICON:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "notification-icon");
            case ROW_RECORDER_HEADER:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "recorder");
            case ROW_ROUND_VIDEO_CAMERA_FEATURE:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "round-video-camera-feature");
            case ROW_ROUND_VIDEO_CAMERA:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "round-video-camera");
            default:
                return FluffySettingsDeepLinkPatch.buildSettingsLink("appearance");
        }
    }

    private void applyTargetScroll() {
        if (listView == null) {
            return;
        }
        int rowId = getTargetRowId();
        if (rowId < 0) {
            return;
        }
        int index = findItemIndexById(rowId);
        if (index < 0) {
            return;
        }
        listView.post(() -> {
            if (listView == null) {
                return;
            }
            RecyclerView.LayoutManager layoutManager = listView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(index, 0);
            } else {
                listView.scrollToPosition(index);
            }
        });
    }

    private int getTargetRowId() {
        Bundle args = getArguments();
        if (args == null) {
            return -1;
        }
        String target = args.getString(ARG_TARGET);
        if (TextUtils.isEmpty(target)) {
            return -1;
        }
        switch (target) {
            case "titles":
                return ROW_TITLES_HEADER;
            case "dialogs-title-mode":
                return ROW_DIALOGS_TITLE_MODE;
            case "dialogs-app-title":
                return ROW_DIALOGS_APP_TITLE;
            case "app-font":
                return ROW_APP_FONT;
            case "import-font":
                return ROW_IMPORT_FONT;
            case "double-tap":
            case "double-tap/incoming":
            case "double-tap/outgoing":
                return ROW_DOUBLE_TAP_EDIT_PREVIEW;
            case "formatting":
                return ROW_FORMATTING_HEADER;
            case "time-with-seconds":
                return ROW_TIME_WITH_SECONDS;
            case "disable-rounded-numbers":
                return ROW_DISABLE_ROUNDED_NUMBERS;
            case "thousands-separator":
                return ROW_THOUSANDS_SEPARATOR;
            case "chat-list":
                return ROW_CHAT_LIST_HEADER;
            case "chat-list-size":
                return ROW_DIALOGS_LIST_SIZE;
            case "chat-ui":
                return ROW_CHAT_UI_HEADER;
            case "center-chat-header":
                return ROW_CENTER_CHAT_HEADER;
            case "map-provider":
                return ROW_MAP_PROVIDER;
            case "hide-channel-post-stars-offer":
                return ROW_HIDE_CHANNEL_POST_STARS_OFFER;
            case "notification-icon":
                return ROW_NOTIFICATION_ICON;
            case "recorder":
                return ROW_RECORDER_HEADER;
            case "round-video-camera-feature":
                return ROW_ROUND_VIDEO_CAMERA_FEATURE;
            case "round-video-camera":
                return ROW_ROUND_VIDEO_CAMERA;
            default:
                return -1;
        }
    }

    private CharSequence getShortSelectedFontDisplayName() {
        CharSequence value = AppFontPatch.getSelectedFontDisplayName();
        if (value == null) {
            return "";
        }
        String text = value.toString();
        if (text.length() <= 6) {
            return text;
        }
        return text.substring(0, 6);
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final boolean checked;

        ItemInner(int viewType, int id, CharSequence text, boolean checked) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.checked = checked;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == VIEW_TYPE_CHECK || type == VIEW_TYPE_TEXT;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) {
                view = new HeaderCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_SHADOW) {
                view = new ShadowSectionCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_DOUBLE_TAP_PREVIEW) {
                view = new DoubleTapEditPreviewCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_DIALOGS_LIST_SIZE) {
                view = new DialogsListSizeCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_DIALOGS_LIST_PREVIEW) {
                view = new DialogsListPreviewCell(parent.getContext());
            } else {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.getItemViewType() == VIEW_TYPE_SHADOW) {
                holder.itemView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
            } else if (holder.getItemViewType() == VIEW_TYPE_CHECK) {
                ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, item.checked, false);
            } else if (holder.getItemViewType() == VIEW_TYPE_DOUBLE_TAP_PREVIEW) {
                DoubleTapEditPreviewCell cell = (DoubleTapEditPreviewCell) holder.itemView;
                cell.syncTheme();
                cell.setActions(
                        AppearanceSettingsHook.getDoubleTapInAction(),
                        AppearanceSettingsHook.getDoubleTapOutAction(),
                        false,
                        false
                );
                cell.setOnBubbleClickListener(FluffyAppearanceActivity.this::showDoubleTapActionDialog);
            } else if (holder.getItemViewType() == VIEW_TYPE_DIALOGS_LIST_SIZE) {
                DialogsListSizeCell cell = (DialogsListSizeCell) holder.itemView;
                cell.bind(AppearanceSettingsHook.getDialogsListScale(), FluffyAppearanceActivity.this::onDialogsListSizeChanged);
            } else if (holder.getItemViewType() == VIEW_TYPE_DIALOGS_LIST_PREVIEW) {
                ((DialogsListPreviewCell) holder.itemView).refresh();
            } else {
                CharSequence value;
                if (item.id == ROW_DIALOGS_TITLE_MODE) {
                    value = getDialogsTitleModeValue();
                } else if (item.id == ROW_DIALOGS_APP_TITLE) {
                    value = getDialogsAppTitleValue();
                } else if (item.id == ROW_APP_FONT) {
                    value = getShortSelectedFontDisplayName();
                } else if (item.id == ROW_IMPORT_FONT) {
                    value = "";
                } else if (item.id == ROW_MAP_PROVIDER) {
                    value = getMapProviderValue();
                } else if (item.id == ROW_ROUND_VIDEO_CAMERA) {
                    value = getRoundVideoCameraValue();
                } else {
                    value = "";
                }
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, value, false);
            }
        }
    }
}
