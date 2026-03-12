package org.ushastoe.fluffy.ui.components;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.glass.GlassTabView;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.MainTabsLayout;
import org.ushastoe.fluffy.hooks.MainTabsConfigHook;
import org.ushastoe.fluffy.patches.MainTabsConfigPatch;

public class FluffyTabsNavbarPreviewView extends FrameLayout {

    private final MainTabsLayout tabsView;
    private final Theme.ResourcesProvider resourcesProvider;
    private final int currentAccount;

    public FluffyTabsNavbarPreviewView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.currentAccount = currentAccount;
        this.resourcesProvider = resourcesProvider;

        setClipChildren(false);
        setClipToPadding(false);
        setClickable(true);
        setFocusable(false);
        setOnTouchListener((v, event) -> true);

        tabsView = new MainTabsLayout(context);
        tabsView.setClipChildren(false);
        tabsView.setPadding(
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4),
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4),
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4),
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_MARGIN + 4)
        );
        tabsView.setBackground(Theme.createRoundRectDrawable(
                org.telegram.messenger.AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT / 2f),
                Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider)
        ));
        addView(tabsView, LayoutHelper.createFrame(
                328 + DialogsActivity.MAIN_TABS_MARGIN * 2,
                DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
        ));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void updatePreview() {
        tabsView.removeAllViews();

        int[] visibleTypes = MainTabsConfigHook.getVisibleTabTypes();
        for (int i = 0; i < visibleTypes.length; i++) {
            GlassTabView tabView = createBaseTab(visibleTypes[i]);
            tabView.setCounterBelowIcon(true);
            tabView.setSelected(i == 0, false);
            tabView.setClickable(false);
            tabView.setFocusable(false);
            tabsView.addView(tabView);
            tabsView.setViewVisible(tabView, true, false);
        }

        long[] quickDialogs = MainTabsConfigHook.getQuickDialogIds();
        for (long dialogId : quickDialogs) {
            GlassTabView tabView = createQuickDialogTab(dialogId);
            tabView.setCounterBelowIcon(true);
            tabView.setSelected(false, false);
            tabView.setClickable(false);
            tabView.setFocusable(false);
            tabsView.addView(tabView);
            tabsView.setViewVisible(tabView, true, false);
        }

        if (tabsView.getChildCount() == 0) {
            GlassTabView fallbackTab = createBaseTab(MainTabsConfigPatch.TAB_CHATS);
            fallbackTab.setCounterBelowIcon(true);
            fallbackTab.setSelected(true, false);
            fallbackTab.setClickable(false);
            fallbackTab.setFocusable(false);
            tabsView.addView(fallbackTab);
            tabsView.setViewVisible(fallbackTab, true, false);
        }

        tabsView.requestLayout();
        tabsView.invalidate();
    }

    private GlassTabView createBaseTab(int type) {
        switch (type) {
            case MainTabsConfigPatch.TAB_CONTACTS:
                return GlassTabView.createMainTab(getContext(), resourcesProvider, GlassTabView.TabAnimation.CONTACTS, R.string.MainTabsContacts);
            case MainTabsConfigPatch.TAB_SETTINGS:
                return GlassTabView.createMainTab(getContext(), resourcesProvider, GlassTabView.TabAnimation.SETTINGS, R.string.Settings);
            case MainTabsConfigPatch.TAB_PROFILE:
                return GlassTabView.createAvatar(getContext(), resourcesProvider, currentAccount, R.string.MainTabsProfile);
            case MainTabsConfigPatch.TAB_CHATS:
            default:
                return GlassTabView.createMainTab(getContext(), resourcesProvider, GlassTabView.TabAnimation.CHATS, R.string.MainTabsChats);
        }
    }

    private GlassTabView createQuickDialogTab(long dialogId) {
        GlassTabView tabView = GlassTabView.createAttachBotTab(getContext(), resourcesProvider);
        tabView.setText(getQuickDialogTitle(dialogId));
        tabView.getBackupImageView().setRoundRadius(org.telegram.messenger.AndroidUtilities.dp(11.33f));
        tabView.getBackupImageView().setLayoutParams(LayoutHelper.createFrame(22, 22, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 5, 0, 0));

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user == null) {
                user = MessagesStorage.getInstance(currentAccount).getUserSync(dialogId);
            }
            if (user != null) {
                avatarDrawable.setInfo(currentAccount, user);
                tabView.getBackupImageView().setForUserOrChat(user, avatarDrawable);
                return tabView;
            }
        } else if (DialogObject.isChatDialog(dialogId)) {
            long chatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
            if (chat == null) {
                chat = MessagesStorage.getInstance(currentAccount).getChatSync(chatId);
            }
            if (chat != null) {
                avatarDrawable.setInfo(currentAccount, chat);
                tabView.getBackupImageView().setForUserOrChat(chat, avatarDrawable);
                return tabView;
            }
        }

        avatarDrawable.setInfo(dialogId, getQuickDialogTitle(dialogId).toString(), null);
        tabView.getBackupImageView().setImageDrawable(avatarDrawable);
        return tabView;
    }

    private CharSequence getQuickDialogTitle(long dialogId) {
        String customLabel = MainTabsConfigHook.getQuickDialogCustomLabel(dialogId);
        if (customLabel != null && !customLabel.trim().isEmpty()) {
            return customLabel;
        }
        if (DialogObject.isUserDialog(dialogId)) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            if (user == null) {
                user = MessagesStorage.getInstance(currentAccount).getUserSync(dialogId);
            }
            if (user != null) {
                String name = org.telegram.messenger.ContactsController.formatName(user.first_name, user.last_name);
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
                if (user.username != null && !user.username.isEmpty()) {
                    return "@" + user.username;
                }
            }
        } else if (DialogObject.isChatDialog(dialogId)) {
            long chatId = -dialogId;
            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(chatId);
            if (chat == null) {
                chat = MessagesStorage.getInstance(currentAccount).getChatSync(chatId);
            }
            if (chat != null && chat.title != null && !chat.title.trim().isEmpty()) {
                return chat.title;
            }
        }
        return LocaleController.getString(R.string.HiddenName);
    }
}
