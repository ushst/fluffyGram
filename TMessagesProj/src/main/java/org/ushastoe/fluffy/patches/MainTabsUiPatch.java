package org.ushastoe.fluffy.patches;

import android.os.Bundle;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.glass.GlassTabView;
import org.telegram.ui.MainTabsActivity;
import org.ushastoe.fluffy.ui.FluffySettingsActivity;
import org.ushastoe.fluffy.ui.FluffyTabsActivity;
import org.ushastoe.fluffy.utils.FluffyTextUtils;

import java.util.ArrayList;

public final class MainTabsUiPatch {

    private MainTabsUiPatch() {
    }

    public static MainTabsUiState createState() {
        MainTabsUiState state = new MainTabsUiState();
        state.visibleTabTypes = MainTabsConfigPatch.getVisibleTabTypes();
        state.quickDialogIds = MainTabsConfigPatch.getQuickDialogIds();
        state.appliedTabsSignature = MainTabsConfigPatch.getConfigSignature();
        return state;
    }

    public static GlassTabView getTabViewForType(MainTabsActivity activity, GlassTabView[] tabs, int type) {
        if (tabs == null || activity == null) {
            return null;
        }
        if (type == MainTabsConfigPatch.TAB_CONTACTS) {
            return tabs[1];
        }
        if (type == MainTabsConfigPatch.TAB_SETTINGS) {
            return activity.getUserConfig().showCallsTab ? tabs[3] : tabs[2];
        }
        if (type == MainTabsConfigPatch.TAB_PROFILE) {
            return tabs[4];
        }
        return tabs[0];
    }

    public static int getTabTypeForIndex(int index) {
        if (index == 1) {
            return MainTabsConfigPatch.TAB_CONTACTS;
        }
        if (index == 2 || index == 3) {
            return MainTabsConfigPatch.TAB_SETTINGS;
        }
        if (index == 4) {
            return MainTabsConfigPatch.TAB_PROFILE;
        }
        return MainTabsConfigPatch.TAB_CHATS;
    }

    public static boolean isTabIndexActive(MainTabsActivity activity, MainTabsUiState state, int index) {
        if (activity == null) {
            return false;
        }
        if (index == 2) {
            return !activity.getUserConfig().showCallsTab && MainTabsConfigPatch.getPositionForType(state.visibleTabTypes, MainTabsConfigPatch.TAB_SETTINGS) >= 0;
        }
        if (index == 3) {
            return activity.getUserConfig().showCallsTab && MainTabsConfigPatch.getPositionForType(state.visibleTabTypes, MainTabsConfigPatch.TAB_SETTINGS) >= 0;
        }
        return MainTabsConfigPatch.getPositionForType(state.visibleTabTypes, getTabTypeForIndex(index)) >= 0;
    }

    public static boolean isTabSelected(MainTabsActivity activity, MainTabsUiState state, int index, int position) {
        if (!isTabIndexActive(activity, state, index)) {
            return false;
        }
        return MainTabsConfigPatch.getPositionForType(state.visibleTabTypes, getTabTypeForIndex(index)) == position;
    }

    public static void bindQuickDialogTab(MainTabsActivity activity, GlassTabView view, long dialogId) {
        if (activity == null || view == null) {
            return;
        }
        String customLabel = MainTabsConfigPatch.getQuickDialogCustomLabel(dialogId);
        if (DialogObject.isUserDialog(dialogId)) {
            bindUserDialogTab(activity, view, dialogId, customLabel);
            return;
        }
        if (DialogObject.isChatDialog(dialogId)) {
            bindChatDialogTab(activity, view, dialogId, customLabel);
        }
    }

    public static void updateQuickDialogCounter(MainTabsActivity activity, GlassTabView view, long dialogId, boolean animated) {
        if (activity == null || view == null) {
            return;
        }
        TLRPC.Dialog dialog = activity.getMessagesController().getDialog(dialogId);
        int unreadCount = dialog != null ? dialog.unread_count : 0;
        if (unreadCount > 0) {
            view.setCounter(LocaleController.formatNumber(unreadCount, ','), false, animated);
        } else {
            view.setCounter(null, false, animated);
        }
    }

    public static void openQuickDialog(MainTabsActivity activity, long dialogId) {
        if (activity == null || activity.getParentActivity() == null) {
            return;
        }
        Bundle args = new Bundle();
        if (DialogObject.isUserDialog(dialogId)) {
            args.putLong("user_id", dialogId);
        } else if (DialogObject.isChatDialog(dialogId)) {
            args.putLong("chat_id", -dialogId);
        } else {
            return;
        }
        if (!activity.getMessagesController().checkCanOpenChat(args, activity)) {
            return;
        }
        activity.presentFragment(new ChatActivity(args));
    }

    public static int getTabTypeAtPosition(MainTabsUiState state, int position) {
        return MainTabsConfigPatch.getTabTypeAtPosition(state.visibleTabTypes, position);
    }

    public static int getPositionForTabType(MainTabsUiState state, int type) {
        return MainTabsConfigPatch.getPositionForType(state.visibleTabTypes, type);
    }

    public static void rebuildTabsBar(MainTabsUiHost host, MainTabsUiState state) {
        if (host == null || state == null || host.getActivity() == null || host.getTabsView() == null || host.getTabs() == null) {
            return;
        }
        MainTabsActivity activity = host.getActivity();
        state.visibleTabTypes = MainTabsConfigPatch.getVisibleTabTypes();
        state.quickDialogIds = MainTabsConfigPatch.getQuickDialogIds();
        host.getTabsView().removeAllViews();
        for (int type : state.visibleTabTypes) {
            GlassTabView view = getTabViewForType(activity, host.getTabs(), type);
            if (view != null) {
                AndroidUtilities.removeFromParent(view);
                host.getTabsView().addView(view);
                host.getTabsView().setViewVisible(view, true, false);
            }
        }
        for (long dialogId : state.quickDialogIds) {
            GlassTabView view = getQuickDialogTabView(host, state, dialogId);
            if (view != null) {
                AndroidUtilities.removeFromParent(view);
                host.getTabsView().addView(view);
                host.getTabsView().setViewVisible(view, true, false);
                view.setSelected(false, false);
            }
        }
        host.getTabsView().requestLayout();
        host.getTabsView().invalidate();
    }

    public static void updateQuickDialogTabs(MainTabsUiHost host, MainTabsUiState state) {
        if (host == null || host.getActivity() == null || state == null) {
            return;
        }
        for (int i = 0; i < state.quickDialogIds.length; i++) {
            bindQuickDialogTab(host.getActivity(), getQuickDialogTabView(host, state, state.quickDialogIds[i]), state.quickDialogIds[i]);
        }
    }

    public static void updateQuickDialogCounters(MainTabsUiHost host, MainTabsUiState state, boolean animated) {
        if (host == null || host.getActivity() == null || state == null) {
            return;
        }
        for (int i = 0; i < state.quickDialogIds.length; i++) {
            updateQuickDialogCounter(host.getActivity(), getQuickDialogTabView(host, state, state.quickDialogIds[i]), state.quickDialogIds[i], animated);
        }
    }

    public static GlassTabView getQuickDialogTabView(MainTabsUiHost host, MainTabsUiState state, long dialogId) {
        MainTabsActivity activity = host.getActivity();
        GlassTabView view = state.quickDialogTabs.get(dialogId);
        if (view == null) {
            view = GlassTabView.createAvatar(activity.getContext(), host.getResourceProvider(), activity.getCurrentAccount(), R.string.MainTabsProfile);
            view.setCounterBelowIcon(true);
            final long quickDialogId = dialogId;
            view.setOnClickListener(v -> openQuickDialog(activity, quickDialogId));
            state.quickDialogTabs.put(dialogId, view);
        }
        bindQuickDialogTab(activity, view, dialogId);
        return view;
    }

    public static void applyMainTabsConfigIfNeeded(MainTabsUiHost host, MainTabsUiState state, boolean force) {
        if (host == null || host.getActivity() == null || state == null) {
            return;
        }
        MainTabsActivity activity = host.getActivity();
        String signature = MainTabsConfigPatch.getConfigSignature();
        boolean showCallsTab = activity.getUserConfig().showCallsTab;
        if (!force && signature.equals(state.appliedTabsSignature) && state.appliedShowCallsTab == showCallsTab) {
            return;
        }

        int currentType = MainTabsConfigPatch.TAB_CHATS;
        if (host.hasViewPager()) {
            currentType = getTabTypeAtPosition(state, host.getCurrentViewPagerPosition());
        }

        state.appliedTabsSignature = signature;
        state.appliedShowCallsTab = showCallsTab;
        rebuildTabsBar(host, state);

        ArrayList<Integer> positionsToDrop = host.collectNonRootFragmentPositions();
        for (int i = 0; i < positionsToDrop.size(); i++) {
            host.dropFragmentAtPosition(positionsToDrop.get(i));
        }

        int newPosition = getPositionForTabType(state, currentType);
        if (newPosition < 0) {
            newPosition = 0;
        }

        if (host.hasViewPager()) {
            int currentPosition = host.getCurrentViewPagerPosition();
            if (currentPosition == newPosition) {
                int tempPosition = newPosition == 0 ? Math.min(1, host.getLastFragmentPosition()) : 0;
                if (tempPosition != newPosition && tempPosition >= 0) {
                    host.setViewPagerPosition(tempPosition);
                }
            }
            host.setViewPagerPosition(newPosition);
        }
        activity.selectTab(newPosition, false);
        host.checkFadeView();
    }

    public static void onTabClicked(MainTabsUiHost host, MainTabsUiState state, int tabType) {
        if (host == null || host.getActivity() == null || state == null || !host.canHandleTabClick()) {
            return;
        }
        MainTabsActivity activity = host.getActivity();
        int position = getPositionForTabType(state, tabType);
        if (position < 0) {
            return;
        }
        if (host.getCurrentViewPagerPosition() == position) {
            host.scrollCurrentTabToTop();
            return;
        }
        activity.selectTab(position, true);
        host.scrollViewPagerToPosition(position);
    }

    public static boolean showNavbarSettingsMenu(MainTabsUiHost host, android.view.View anchor) {
        if (host == null || host.getActivity() == null || host.getActivity().getParentActivity() == null || anchor == null) {
            return false;
        }
        ItemOptions options = ItemOptions.makeOptions(host.getActivity(), anchor);
        options.add(R.drawable.fluffy_settings_icon, LocaleController.getString(R.string.FluffySettings), () ->
                host.getActivity().presentFragment(new FluffySettingsActivity()));
        options.add(R.drawable.msg_settings, LocaleController.getString(R.string.FluffyTabs), () ->
                host.getActivity().presentFragment(new FluffyTabsActivity()));
        host.getActivity().showMainTabsPopup(options);
        return true;
    }

    public static void applySelection(MainTabsActivity activity, MainTabsUiState state, GlassTabView[] tabs, int position, boolean animated) {
        if (tabs == null) {
            return;
        }
        for (int i = 0; i < tabs.length; i++) {
            GlassTabView tab = tabs[i];
            if (tab != null) {
                tab.setSelected(isTabSelected(activity, state, i, position), animated);
            }
        }
    }

    public static void applyGestureSelection(MainTabsActivity activity, MainTabsUiState state, GlassTabView[] tabs, org.telegram.ui.MainTabsLayout tabsView, float animatedPosition, boolean allow) {
        if (tabs == null) {
            return;
        }
        for (int index = 0; index < tabs.length; index++) {
            GlassTabView tab = tabs[index];
            if (tab == null) {
                continue;
            }
            final int position = getPositionForTabType(state, getTabTypeForIndex(index));
            final float visibility = position >= 0 ? Math.max(0, 1f - Math.abs(position - animatedPosition)) : 0f;
            tab.setGestureSelectedOverride(visibility, allow);
        }
        if (tabsView != null) {
            tabsView.invalidate();
        }
    }

    private static void bindUserDialogTab(MainTabsActivity activity, GlassTabView view, long dialogId, String customLabel) {
        TLRPC.User user = MessagesController.getInstance(activity.getCurrentAccount()).getUser(dialogId);
        if (user == null) {
            user = MessagesStorage.getInstance(activity.getCurrentAccount()).getUserSync(dialogId);
        }
        if (user == null) {
            view.setText(getDisplayText(customLabel, String.valueOf(dialogId)));
            return;
        }

        String title = customLabel;
        if (UserObject.isUserSelf(user)) {
            if (isBlank(title)) {
                title = LocaleController.getString(R.string.FluffyTabsMe);
            }
            AvatarDrawable avatarDrawable = new AvatarDrawable();
            avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_SAVED);
            avatarDrawable.setScaleSize(0.40f);
            if (view.getBackupImageView() != null) {
                view.getBackupImageView().setImageDrawable(avatarDrawable);
            }
        } else {
            view.setAttachBotUser(user, activity.getCurrentAccount());
            if (isBlank(title)) {
                title = ContactsController.formatName(user.first_name, user.last_name);
            }
        }
        if (isBlank(title) && !isBlank(user.username)) {
            title = "@" + user.username;
        }
        view.setText(getDisplayText(title, String.valueOf(dialogId)));
    }

    private static void bindChatDialogTab(MainTabsActivity activity, GlassTabView view, long dialogId, String customLabel) {
        long chatId = -dialogId;
        TLRPC.Chat chat = MessagesController.getInstance(activity.getCurrentAccount()).getChat(chatId);
        if (chat == null) {
            chat = MessagesStorage.getInstance(activity.getCurrentAccount()).getChatSync(chatId);
        }
        if (chat == null) {
            view.setText(getDisplayText(customLabel, String.valueOf(dialogId)));
            return;
        }

        String title = isBlank(customLabel) ? chat.title : customLabel;
        view.setText(getDisplayText(title, String.valueOf(dialogId)));
        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(activity.getCurrentAccount(), chat);
        if (view.getBackupImageView() != null) {
            view.getBackupImageView().setForUserOrChat(chat, avatarDrawable);
        }
    }

    private static CharSequence getDisplayText(String value, String fallback) {
        return FluffyTextUtils.truncateLongWords(isBlank(value) ? fallback : value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
