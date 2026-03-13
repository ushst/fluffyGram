package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DialogsActivity;
import org.ushastoe.fluffy.hooks.MainTabsConfigHook;
import org.ushastoe.fluffy.patches.MainTabsConfigPatch;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.ui.components.FluffyTabsNavbarPreviewView;
import org.ushastoe.fluffy.ui.components.FluffyTabsPreviewCell;
import org.ushastoe.fluffy.utils.FluffyTextUtils;

import java.util.ArrayList;

public class FluffyTabsActivity extends BaseFragment {

    private static final int PREVIEW_BOTTOM_MARGIN_DP = 6;

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_ADD = 2;
    private static final int VIEW_TYPE_PREVIEW = 3;

    private static final int ROW_VISIBLE_HEADER = 0;
    private static final int ROW_HIDDEN_HEADER = 1;
    private static final int ROW_QUICK_CHATS_HEADER = 2;
    private static final int ROW_ADD_CHAT = 3;

    private static final int ITEM_KIND_BASE = 0;
    private static final int ITEM_KIND_QUICK_CHAT = 1;
    private static final int ITEM_KIND_EMPTY = 2;

    private final ArrayList<ItemInner> items = new ArrayList<>();
    private RecyclerListView listView;
    private ListAdapter adapter;
    private FluffyTabsNavbarPreviewView navbarPreviewView;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyTabs));
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
        listView.setPadding(0, 0, 0, AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS + 12 + PREVIEW_BOTTOM_MARGIN_DP));
        listView.setClipToPadding(false);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= items.size()) {
                return;
            }
            ItemInner item = items.get(position);
            if (item.viewType == VIEW_TYPE_ADD) {
                showQuickChatPicker();
                return;
            }
            if (item.viewType != VIEW_TYPE_TEXT) {
                return;
            }
            if (item.kind == ITEM_KIND_QUICK_CHAT) {
                showQuickChatOptions(item.dialogId);
                return;
            }
            if (item.kind != ITEM_KIND_BASE || item.baseType == MainTabsConfigPatch.TAB_CHATS) {
                return;
            }
            if (item.hidden) {
                MainTabsConfigHook.showOptionalTab(item.baseType);
                updateItems();
            } else {
                showBaseTabOptions(item.baseType);
            }
        });
        listView.setOnItemLongClickListener((view, position) ->
                FluffySettingsDeepLinkPatch.copyLink(this, FluffySettingsDeepLinkPatch.buildSettingsLink("appearance", "tabs")));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        navbarPreviewView = new FluffyTabsNavbarPreviewView(context, currentAccount, getResourceProvider());
        frameLayout.addView(navbarPreviewView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT,
                DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS + 12,
                Gravity.BOTTOM,
                0,
                0,
                0,
                PREVIEW_BOTTOM_MARGIN_DP
        ));

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
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_VISIBLE_HEADER, ITEM_KIND_BASE,
                MainTabsConfigPatch.TAB_CHATS, 0, false,
                LocaleController.getString(R.string.FluffyTabsVisibleSection),
                null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, 10, ITEM_KIND_BASE,
                MainTabsConfigPatch.TAB_CHATS, 0, false,
                LocaleController.getString(R.string.MainTabsChats),
                LocaleController.getString(R.string.FluffyTabsRequired)));

        int[] visible = MainTabsConfigHook.getOptionalVisibleTypes();
        for (int type : visible) {
            items.add(new ItemInner(VIEW_TYPE_TEXT, 100 + type, ITEM_KIND_BASE, type, 0, false,
                    getBaseTypeTitle(type), LocaleController.getString(R.string.FluffyTabsVisibleValue)));
        }

        int[] hidden = MainTabsConfigHook.getHiddenOptionalTypes();
        if (hidden.length > 0) {
            items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_HIDDEN_HEADER, ITEM_KIND_BASE,
                    -1, 0, false, LocaleController.getString(R.string.FluffyTabsHiddenSection), null));
            for (int type : hidden) {
                items.add(new ItemInner(VIEW_TYPE_TEXT, 200 + type, ITEM_KIND_BASE, type, 0, true,
                        getBaseTypeTitle(type), LocaleController.getString(R.string.FluffyTabsHiddenValue)));
            }
        }

        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_QUICK_CHATS_HEADER, ITEM_KIND_QUICK_CHAT,
                -1, 0, false, LocaleController.getString(R.string.FluffyTabsQuickChatsSection), null));

        long[] quickDialogs = MainTabsConfigHook.getQuickDialogIds();
        if (quickDialogs.length == 0) {
            items.add(new ItemInner(VIEW_TYPE_TEXT, 300, ITEM_KIND_EMPTY,
                    -1, 0, false, LocaleController.getString(R.string.FluffyTabsNoQuickChats), null));
        } else {
            for (int i = 0; i < quickDialogs.length; i++) {
                long dialogId = quickDialogs[i];
                items.add(new ItemInner(VIEW_TYPE_TEXT, 400 + i, ITEM_KIND_QUICK_CHAT,
                        -1, dialogId, false, getQuickDialogDisplayTitle(dialogId),
                        getQuickDialogValue(dialogId)));
            }
        }
        items.add(new ItemInner(VIEW_TYPE_ADD, ROW_ADD_CHAT, ITEM_KIND_QUICK_CHAT,
                -1, 0, false, LocaleController.getString(R.string.FluffyTabsAddChat), null));

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        if (navbarPreviewView != null) {
            navbarPreviewView.updatePreview();
        }
    }

    private void showBaseTabOptions(int type) {
        if (getParentActivity() == null) {
            return;
        }
        ArrayList<CharSequence> options = new ArrayList<>();
        ArrayList<Integer> actions = new ArrayList<>();

        int[] visible = MainTabsConfigHook.getOptionalVisibleTypes();
        int position = MainTabsConfigHook.getPositionForType(visible, type);
        if (position > 0) {
            options.add(LocaleController.getString(R.string.FluffyTabsMoveUp));
            actions.add(0);
        }
        if (position >= 0 && position < visible.length - 1) {
            options.add(LocaleController.getString(R.string.FluffyTabsMoveDown));
            actions.add(1);
        }
        if (MainTabsConfigHook.canHideOptionalTab(type)) {
            options.add(LocaleController.getString(R.string.FluffyTabsHide));
            actions.add(2);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getBaseTypeTitle(type));
        builder.setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
            int action = actions.get(which);
            if (action == 0) {
                MainTabsConfigHook.moveOptionalTabUp(type);
            } else if (action == 1) {
                MainTabsConfigHook.moveOptionalTabDown(type);
            } else if (action == 2) {
                MainTabsConfigHook.hideOptionalTab(type);
            }
            updateItems();
        });
        showDialog(builder.create());
    }

    private void showQuickChatOptions(long dialogId) {
        if (getParentActivity() == null) {
            return;
        }
        ArrayList<CharSequence> options = new ArrayList<>();
        ArrayList<Integer> actions = new ArrayList<>();

        options.add(LocaleController.getString(R.string.FluffyTabsRename));
        actions.add(0);

        long[] quickDialogs = MainTabsConfigHook.getQuickDialogIds();
        int position = indexOf(quickDialogs, dialogId);
        if (position > 0) {
            options.add(LocaleController.getString(R.string.FluffyTabsMoveUp));
            actions.add(1);
        }
        if (position >= 0 && position < quickDialogs.length - 1) {
            options.add(LocaleController.getString(R.string.FluffyTabsMoveDown));
            actions.add(2);
        }
        options.add(LocaleController.getString(R.string.Remove));
        actions.add(3);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(getQuickDialogDisplayTitle(dialogId));
        builder.setItems(options.toArray(new CharSequence[0]), (dialog, which) -> {
            int action = actions.get(which);
            if (action == 0) {
                showRenameQuickChatDialog(dialogId);
            } else if (action == 1) {
                MainTabsConfigHook.moveQuickDialogUp(dialogId);
            } else if (action == 2) {
                MainTabsConfigHook.moveQuickDialogDown(dialogId);
            } else if (action == 3) {
                MainTabsConfigHook.removeQuickDialog(dialogId);
            }
            updateItems();
        });
        showDialog(builder.create());
    }

    private void showQuickChatPicker() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("resetDelegate", false);
        DialogsActivity activity = new DialogsActivity(args);
        activity.setDelegate((fragment, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids == null || dids.isEmpty()) {
                return false;
            }
            long dialogId = dids.get(0).dialogId;
            if (dialogId == 0 || DialogObject.isEncryptedDialog(dialogId)) {
                return false;
            }
            MainTabsConfigHook.addQuickDialog(dialogId);
            updateItems();
            fragment.finishFragment();
            return true;
        });
        presentFragment(activity);
    }

    private CharSequence getBaseTypeTitle(int type) {
        switch (type) {
            case MainTabsConfigPatch.TAB_CONTACTS:
                return LocaleController.getString(R.string.MainTabsContacts);
            case MainTabsConfigPatch.TAB_SETTINGS:
                return LocaleController.getString(R.string.Settings);
            case MainTabsConfigPatch.TAB_PROFILE:
                return LocaleController.getString(R.string.MainTabsProfile);
            case MainTabsConfigPatch.TAB_CHATS:
            default:
                return LocaleController.getString(R.string.MainTabsChats);
        }
    }

    private void showRenameQuickChatDialog(long dialogId) {
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
        editText.setHint(LocaleController.getString(R.string.FluffyTabsCustomLabelHint));
        editText.setMaxLines(1);
        editText.setLines(1);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setGravity(Gravity.LEFT | Gravity.TOP);
        editText.setSingleLine(true);
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setPadding(0, AndroidUtilities.dp(4), 0, 0);

        String currentLabel = MainTabsConfigHook.getQuickDialogCustomLabel(dialogId);
        if (currentLabel != null) {
            editText.setText(currentLabel);
            editText.setSelection(editText.length());
        }

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView message = new TextView(context);
        message.setText(LocaleController.getString(R.string.FluffyTabsCustomLabel));
        message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        message.setPadding(AndroidUtilities.dp(23), AndroidUtilities.dp(12), AndroidUtilities.dp(23), AndroidUtilities.dp(6));
        message.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        linearLayout.addView(message, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        linearLayout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyTabsRename));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        builder.setPositiveButton(LocaleController.getString(R.string.Done), (dialog, which) -> {
            MainTabsConfigHook.setQuickDialogCustomLabel(dialogId, editText.getText().toString());
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

    private CharSequence getQuickDialogDisplayTitle(long dialogId) {
        String customLabel = MainTabsConfigHook.getQuickDialogCustomLabel(dialogId);
        if (customLabel != null) {
            return FluffyTextUtils.truncateLongWords(customLabel);
        }
        return getQuickDialogTitle(dialogId);
    }

    private CharSequence getQuickDialogValue(long dialogId) {
        if (MainTabsConfigHook.getQuickDialogCustomLabel(dialogId) == null) {
            return LocaleController.getString(R.string.FluffyTabsVisibleValue);
        }
        return getQuickDialogTitle(dialogId);
    }

    private CharSequence getQuickDialogTitle(long dialogId) {
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user == null) {
                user = MessagesStorage.getInstance(currentAccount).getUserSync(dialogId);
            }
            if (user == null) {
                return String.valueOf(dialogId);
            }
            String name = user.first_name != null || user.last_name != null
                    ? org.telegram.messenger.ContactsController.formatName(user.first_name, user.last_name)
                    : null;
            if (name == null || name.trim().isEmpty()) {
                if (user.username != null && !user.username.isEmpty()) {
                    return FluffyTextUtils.truncateLongWords("@" + user.username);
                }
                return String.valueOf(dialogId);
            }
            return FluffyTextUtils.truncateLongWords(name);
        }
        if (DialogObject.isChatDialog(dialogId)) {
            long chatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
            if (chat == null) {
                chat = MessagesStorage.getInstance(currentAccount).getChatSync(chatId);
            }
            if (chat == null || chat.title == null || chat.title.trim().isEmpty()) {
                return String.valueOf(dialogId);
            }
            return FluffyTextUtils.truncateLongWords(chat.title);
        }
        return String.valueOf(dialogId);
    }

    private ArrayList<FluffyTabsPreviewCell.PreviewItem> buildPreviewItems() {
        ArrayList<FluffyTabsPreviewCell.PreviewItem> previewItems = new ArrayList<>();
        int[] visibleTypes = MainTabsConfigHook.getVisibleTabTypes();
        for (int i = 0; i < visibleTypes.length; i++) {
            int type = visibleTypes[i];
            boolean selected = i == 0;
            if (type == MainTabsConfigPatch.TAB_PROFILE) {
                previewItems.add(FluffyTabsPreviewCell.PreviewItem.withAvatar(
                        getBaseTypeTitle(type),
                        createProfileAvatarDrawable(),
                        selected
                ));
            } else {
                previewItems.add(FluffyTabsPreviewCell.PreviewItem.withIcon(
                        getBaseTypeTitle(type),
                        getBaseTypeIcon(type),
                        selected
                ));
            }
        }

        long[] quickDialogs = MainTabsConfigHook.getQuickDialogIds();
        for (long dialogId : quickDialogs) {
            previewItems.add(FluffyTabsPreviewCell.PreviewItem.withAvatar(
                    getQuickDialogDisplayTitle(dialogId),
                    createDialogAvatarDrawable(dialogId),
                    false
            ));
        }
        return previewItems;
    }

    private int getBaseTypeIcon(int type) {
        switch (type) {
            case MainTabsConfigPatch.TAB_CONTACTS:
                return R.drawable.msg_contacts;
            case MainTabsConfigPatch.TAB_SETTINGS:
                return R.drawable.msg_settings;
            case MainTabsConfigPatch.TAB_CHATS:
            default:
                return R.drawable.msg_message;
        }
    }

    private Drawable createProfileAvatarDrawable() {
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        TLRPC.User user = UserConfig.getInstance(currentAccount).getCurrentUser();
        if (user != null) {
            avatarDrawable.setInfo(currentAccount, user);
        } else {
            avatarDrawable.setInfo(0, LocaleController.getString(R.string.MainTabsProfile), null);
        }
        return avatarDrawable;
    }

    private Drawable createDialogAvatarDrawable(long dialogId) {
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user == null) {
                user = MessagesStorage.getInstance(currentAccount).getUserSync(dialogId);
            }
            if (user != null) {
                avatarDrawable.setInfo(currentAccount, user);
                return avatarDrawable;
            }
        } else if (DialogObject.isChatDialog(dialogId)) {
            long chatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
            if (chat == null) {
                chat = MessagesStorage.getInstance(currentAccount).getChatSync(chatId);
            }
            if (chat != null) {
                avatarDrawable.setInfo(currentAccount, chat);
                return avatarDrawable;
            }
        }
        avatarDrawable.setInfo(dialogId, getQuickDialogDisplayTitle(dialogId).toString(), null);
        return avatarDrawable;
    }

    private static int indexOf(long[] values, long value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == value) {
                return i;
            }
        }
        return -1;
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final int kind;
        final int baseType;
        final long dialogId;
        final boolean hidden;
        final CharSequence text;
        final CharSequence value;

        ItemInner(int viewType, int id, int kind, int baseType, long dialogId, boolean hidden, CharSequence text, CharSequence value) {
            this.viewType = viewType;
            this.id = id;
            this.kind = kind;
            this.baseType = baseType;
            this.dialogId = dialogId;
            this.hidden = hidden;
            this.text = text;
            this.value = value;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return false;
            }
            ItemInner item = items.get(position);
            if (item.viewType == VIEW_TYPE_ADD) {
                return true;
            }
            if (item.viewType != VIEW_TYPE_TEXT) {
                return false;
            }
            return item.kind != ITEM_KIND_EMPTY && item.baseType != MainTabsConfigPatch.TAB_CHATS;
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
            } else if (viewType == VIEW_TYPE_PREVIEW) {
                view = new FluffyTabsPreviewCell(parent.getContext());
            } else if (viewType == VIEW_TYPE_ADD) {
                view = new TextCell(parent.getContext());
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
            } else if (holder.getItemViewType() == VIEW_TYPE_PREVIEW) {
                ((FluffyTabsPreviewCell) holder.itemView).setItems(buildPreviewItems());
            } else if (holder.getItemViewType() == VIEW_TYPE_ADD) {
                ((TextCell) holder.itemView).setTextAndIcon(item.text, R.drawable.msg_add, false);
            } else {
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, FluffyTextUtils.truncateParameterValue(item.value), false);
            }
        }
    }
}
