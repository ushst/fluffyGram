package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.text.InputType;
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
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;
import org.ushastoe.fluffy.hooks.DialogsAppTitleHook;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;

import java.util.ArrayList;

public class FluffyAppearanceActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_CHECK = 1;
    private static final int VIEW_TYPE_TEXT = 2;

    private static final int ROW_APPEARANCE_HEADER = 0;
    private static final int ROW_HIDE_CHANNEL_POST_STARS_OFFER = 1;
    private static final int ROW_DIALOGS_TITLE_MODE = 2;
    private static final int ROW_DIALOGS_APP_TITLE = 3;
    private static final int ROW_NOTIFICATION_ICON = 4;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

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
            } else if (item.id == ROW_DIALOGS_TITLE_MODE) {
                showDialogsTitleModeDialog();
            } else if (item.id == ROW_DIALOGS_APP_TITLE) {
                showDialogsAppTitleDialog();
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();

        fragmentView = frameLayout;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateItems();
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_APPEARANCE_HEADER,
                LocaleController.getString(R.string.FluffyAppearanceSection), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_HIDE_CHANNEL_POST_STARS_OFFER,
                LocaleController.getString(R.string.FluffyHideChannelPostStarsOffer),
                AppearanceSettingsHook.isChannelPostStarsOfferHidden()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DIALOGS_TITLE_MODE,
                LocaleController.getString(R.string.FluffyCenterDialogsTitle),
                false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_DIALOGS_APP_TITLE,
                LocaleController.getString(R.string.FluffyDialogsAppTitle),
                false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_NOTIFICATION_ICON,
                LocaleController.getString(R.string.FluffyNotificationIcon),
                AppearanceSettingsHook.useFluffyNotificationIcon()));
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
            } else if (viewType == VIEW_TYPE_CHECK) {
                view = new TextCheckCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
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
            } else if (holder.getItemViewType() == VIEW_TYPE_CHECK) {
                ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, item.checked, false);
            } else {
                CharSequence value;
                if (item.id == ROW_DIALOGS_TITLE_MODE) {
                    value = getDialogsTitleModeValue();
                } else if (item.id == ROW_DIALOGS_APP_TITLE) {
                    value = getDialogsAppTitleValue();
                } else {
                    value = "";
                }
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, value, false);
            }
        }
    }
}
