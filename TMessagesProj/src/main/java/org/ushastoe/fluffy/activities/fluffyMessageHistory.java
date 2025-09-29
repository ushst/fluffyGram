package org.ushastoe.fluffy.activities;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.DialogObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.helpers.MessageHelper;
import org.telegram.ui.Components.AvatarDrawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class fluffyMessageHistory extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private final Map<Long, String> messages;
    private final List<Long> messageIds;
    private final int rowCount;
    private final long dialogId;
    private final int messageId;
    private RecyclerListView listView;
    private ChatAvatarContainer avatarContainer;
    private final MessageObject originalMessage;
    private final TLRPC.User historyUser;
    private final TLRPC.Chat historyChat;

    public fluffyMessageHistory(MessageObject messageObject) {
        setCurrentAccount(messageObject.currentAccount);

        originalMessage = messageObject;
        dialogId = messageObject.messageOwner.dialog_id;
        messageId = messageObject.messageOwner.id;

        messages = getMessagesStorage().loadFlHistory(dialogId, messageId);
        messageIds = new ArrayList<>(messages.keySet());

        long currentMessageKey = System.currentTimeMillis() / 1000;
        messages.put(currentMessageKey, MessageHelper.encodeBase64(messageObject.messageOwner.message));

        messageIds.add(0, currentMessageKey);

        rowCount = messages.size();

        MessagesController controller = getMessagesController();
        TLRPC.User resolvedUser = null;
        TLRPC.Chat resolvedChat = null;
        if (messageObject.messageOwner != null) {
            TLRPC.Peer fromPeer = messageObject.messageOwner.from_id;
            if (fromPeer instanceof TLRPC.TL_peerUser) {
                resolvedUser = controller.getUser(((TLRPC.TL_peerUser) fromPeer).user_id);
            } else if (fromPeer instanceof TLRPC.TL_peerChat) {
                resolvedChat = controller.getChat(((TLRPC.TL_peerChat) fromPeer).chat_id);
            } else if (fromPeer instanceof TLRPC.TL_peerChannel) {
                resolvedChat = controller.getChat(((TLRPC.TL_peerChannel) fromPeer).channel_id);
            }
        }
        historyUser = resolvedUser;
        historyChat = resolvedChat;
    }

    @Override
    public View createView(Context context) {
        Object peer = getAccountInstance().getMessagesController().getUserOrChat(dialogId);

        avatarContainer = new ChatAvatarContainer(context, null, false);
        avatarContainer.setOccupyStatusBar(!AndroidUtilities.isTablet());
        actionBar.addView(avatarContainer, 0, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 56, 0, 40, 0));

        String name = "";
        if (peer == null) {
            name = "?"; // wtf
        } else if (peer instanceof TLRPC.User) {
            avatarContainer.setUserAvatar(((TLRPC.User) peer));
            name = ((TLRPC.User) peer).first_name;
        } else if (peer instanceof TLRPC.Chat) {
            avatarContainer.setChatAvatar(((TLRPC.Chat) peer));
            name = ((TLRPC.Chat) peer).title;
        } else {
            name = LocaleController.getString(R.string.EditsHistoryTitle);
        }
        avatarContainer.setTitle(name);
        avatarContainer.setSubtitle(String.valueOf(messageId));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        LinearLayoutManager layoutManager;
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, true) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setVerticalScrollBarEnabled(true);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        ListAdapter adapter;
        listView.setAdapter(adapter = new ListAdapter(context));

        if (!messageIds.isEmpty()) {
            listView.scrollToPosition(messageIds.size() - 1); // Прокручиваем к последнему элементу
        }

        boolean isChatMode = true;

        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child instanceof ChatMessageCell) {
                ((ChatMessageCell) child).isChat = isChatMode;
            }
        }

        if (messageIds.size() > 0) {
            listView.scrollToPosition(0);
        }

        return fragmentView;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{HeaderCell.class, TextCheckCell.class, TextDetailSettingsCell.class, TextSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ChatMessageCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ChatMessageCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ChatActionCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{ChatActionCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LINKCOLOR, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));

        return themeDescriptions;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        // todo: update list in real time
    }

    private TLRPC.Peer clonePeer(TLRPC.Peer peer, long fallbackDialogId) {
        if (peer instanceof TLRPC.TL_peerUser) {
            TLRPC.TL_peerUser copy = new TLRPC.TL_peerUser();
            copy.user_id = ((TLRPC.TL_peerUser) peer).user_id;
            return copy;
        } else if (peer instanceof TLRPC.TL_peerChat) {
            TLRPC.TL_peerChat copy = new TLRPC.TL_peerChat();
            copy.chat_id = ((TLRPC.TL_peerChat) peer).chat_id;
            return copy;
        } else if (peer instanceof TLRPC.TL_peerChannel) {
            TLRPC.TL_peerChannel copy = new TLRPC.TL_peerChannel();
            copy.channel_id = ((TLRPC.TL_peerChannel) peer).channel_id;
            return copy;
        }

        if (fallbackDialogId != 0) {
            if (DialogObject.isEncryptedDialog(fallbackDialogId)) {
                TLRPC.TL_peerUser copy = new TLRPC.TL_peerUser();
                copy.user_id = UserConfig.getInstance(currentAccount).getClientUserId();
                return copy;
            } else if (fallbackDialogId > 0) {
                TLRPC.TL_peerUser copy = new TLRPC.TL_peerUser();
                copy.user_id = (int) fallbackDialogId;
                return copy;
            } else {
                long absId = -fallbackDialogId;
                if (absId != 0) {
                    TLRPC.TL_peerChannel copy = new TLRPC.TL_peerChannel();
                    copy.channel_id = (int) absId;
                    return copy;
                }
            }
        }
        return null;
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        public ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 1) {
                view = new ChatMessageCell(context, currentAccount);
            } else {
                view = null;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1:
                    ChatMessageCell fluffyMessageDetailCell = (ChatMessageCell) holder.itemView;

                    long messageKey = messageIds.get(position);
                    String messageText = messages.get(messageKey);
                    messageText = MessageHelper.decodeBase64(messageText);

                    TLRPC.TL_message msg = new TLRPC.TL_message();
                    TLRPC.Message original = originalMessage.messageOwner;

                    msg.message = messageText;
                    msg.date = (int) messageKey;
                    msg.dialog_id = dialogId;
                    msg.flags = original != null ? original.flags | TLRPC.MESSAGE_FLAG_HAS_FROM_ID : TLRPC.MESSAGE_FLAG_HAS_FROM_ID;
                    msg.flags &= ~TLRPC.MESSAGE_FLAG_HAS_MARKUP;
                    msg.flags &= ~TLRPC.MESSAGE_FLAG_HAS_BOT_ID;
                    msg.flags &= ~TLRPC.MESSAGE_FLAG_HAS_VIEWS;
                    msg.id = Utilities.random.nextInt();

                    msg.out = false;
                    msg.post = original != null && original.post;
                    msg.legacy = original != null && original.legacy;
                    msg.mentioned = original != null && original.mentioned;
                    msg.silent = original != null && original.silent;
                    msg.ttl = original != null ? original.ttl : 0;
                    msg.ttl_period = original != null ? original.ttl_period : 0;
                    msg.fwd_from = original != null ? original.fwd_from : null;
                    msg.reply_to = original != null ? original.reply_to : null;
                    msg.via_bot_id = 0;
                    msg.reply_markup = null;
                    msg.views = 0;
                    msg.entities = original != null && original.entities != null ? new ArrayList<>(original.entities) : null;
                    if (msg.entities != null) {
                        msg.flags |= TLRPC.MESSAGE_FLAG_HAS_ENTITIES;
                    }

                    msg.media = new TLRPC.TL_messageMediaEmpty();
                    msg.flags |= TLRPC.MESSAGE_FLAG_HAS_MEDIA;

                    msg.peer_id = clonePeer(original != null ? original.peer_id : null, dialogId);
                    msg.from_id = clonePeer(original != null ? original.from_id : null, dialogId);

                    MessageObject historyMessageObject = new MessageObject(currentAccount, msg, false, false);
                    historyMessageObject.forceAvatar = true;
                    if (historyUser != null && historyUser.photo == null) {
                        AvatarDrawable placeholder = new AvatarDrawable();
                        placeholder.setInfo(currentAccount, historyUser);
                        historyMessageObject.customAvatarDrawable = placeholder;
                    } else if (historyChat != null && historyChat.photo == null) {
                        AvatarDrawable placeholder = new AvatarDrawable();
                        placeholder.setInfo(currentAccount, historyChat);
                        historyMessageObject.customAvatarDrawable = placeholder;
                    } else if (historyUser == null && historyChat == null) {
                        AvatarDrawable placeholder = new AvatarDrawable();
                        if (msg.from_id instanceof TLRPC.TL_peerUser) {
                            long uid = ((TLRPC.TL_peerUser) msg.from_id).user_id;
                            placeholder.setInfo(uid, null, null, null, null, null);
                        } else if (msg.from_id instanceof TLRPC.TL_peerChat) {
                            long chatId = ((TLRPC.TL_peerChat) msg.from_id).chat_id;
                            placeholder.setInfo(-chatId, null, null, null, null, null);
                        } else if (msg.from_id instanceof TLRPC.TL_peerChannel) {
                            long channelId = ((TLRPC.TL_peerChannel) msg.from_id).channel_id;
                            placeholder.setInfo(-channelId, null, null, null, null, null);
                        }
                        historyMessageObject.customAvatarDrawable = placeholder;
                    }

                    fluffyMessageDetailCell.setMessageObject(historyMessageObject, null, false, false, false);
                    fluffyMessageDetailCell.isChat = true;
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position >= 0 && position < messages.size() ? 1 : 0;
        }
    }
}
