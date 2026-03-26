package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Stories.recorder.PreviewView;
import org.ushastoe.fluffy.patches.LocalMessageArchivePatch;
import org.ushastoe.fluffy.utils.LocalMessageArchiveStore;

import java.util.ArrayList;

public class FluffyLocalMessageHistoryActivity extends BaseFragment {

    private static final int VIEW_TYPE_ACTION = 0;
    private static final int VIEW_TYPE_MESSAGE = 1;

    private final ArrayList<ItemInner> items = new ArrayList<>();
    private final MessageObject sourceMessage;
    private RecyclerListView listView;
    private ListAdapter adapter;

    public FluffyLocalMessageHistoryActivity(Bundle args, MessageObject sourceMessage) {
        super(args);
        this.sourceMessage = sourceMessage;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyLocalMessageHistoryTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        Drawable background = PreviewView.getBackgroundDrawable(null, getCurrentAccount(), getHistoryDialogId(), Theme.isCurrentThemeDark());
        frameLayout.setBackground(background);

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();
        fragmentView = frameLayout;
        return frameLayout;
    }

    private void updateItems() {
        items.clear();
        if (sourceMessage != null) {
            items.add(new ItemInner(LocaleController.getString(R.string.FluffyLocalMessageHistoryCurrent), cloneForHistory(sourceMessage, LocalMessageArchivePatch.getCurrentText(sourceMessage), sourceMessage.messageOwner != null ? sourceMessage.messageOwner.date : 0)));
        }

        long dialogId = getHistoryDialogId();
        int messageId = getHistoryMessageId();
        ArrayList<LocalMessageArchiveStore.Entry> history = LocalMessageArchiveStore.getHistory(dialogId, messageId);
        if (history.isEmpty()) {
            items.add(new ItemInner(LocaleController.getString(R.string.FluffyLocalMessageHistoryEmpty), cloneForHistory(sourceMessage, LocaleController.getString(R.string.FluffyLocalMessageHistoryEmptyInfo), sourceMessage != null && sourceMessage.messageOwner != null ? sourceMessage.messageOwner.date : 0)));
        } else {
            for (int i = 0; i < history.size(); i++) {
                LocalMessageArchiveStore.Entry entry = history.get(i);
                items.add(new ItemInner(LocalMessageArchivePatch.getArchiveEntryTitle(entry), cloneForHistory(sourceMessage, getDisplayText(entry.text), entry.savedAt)));
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private long getHistoryDialogId() {
        return getArguments() != null ? getArguments().getLong("fluffy_history_dialog_id", 0L) : 0L;
    }

    private int getHistoryMessageId() {
        return getArguments() != null ? getArguments().getInt("fluffy_history_message_id", 0) : 0;
    }

    private String getDisplayText(String text) {
        return TextUtils.isEmpty(text) ? LocaleController.getString(R.string.FluffyLocalMessageHistoryNoText) : text;
    }

    private MessageObject cloneForHistory(MessageObject template, String text, int date) {
        if (template == null || template.messageOwner == null) {
            TLRPC.TL_message message = new TLRPC.TL_message();
            message.message = getDisplayText(text);
            message.date = date;
            message.dialog_id = getHistoryDialogId();
            message.id = getHistoryMessageId();
            message.entities = new ArrayList<>();
            return new MessageObject(getCurrentAccount(), message, false, false);
        }
        TLRPC.Message copy = cloneMessage(template.messageOwner);
        copy.message = getDisplayText(text);
        copy.date = date != 0 ? date : template.messageOwner.date;
        copy.edit_date = 0;
        copy.edit_hide = true;
        copy.flags &= ~TLRPC.MESSAGE_FLAG_EDITED;
        copy.entities = new ArrayList<>();
        return new MessageObject(template.currentAccount, copy, false, false);
    }

    private TLRPC.Message cloneMessage(TLRPC.Message source) {
        try {
            SerializedData data = new SerializedData(source.getObjectSize());
            source.serializeToStream(data);
            SerializedData reader = new SerializedData(data.toByteArray());
            TLRPC.Message copy = TLRPC.Message.TLdeserialize(reader, reader.readInt32(true), true);
            data.cleanup();
            reader.cleanup();
            return copy != null ? copy : source;
        } catch (Exception ignore) {
            return source;
        }
    }

    private static class ItemInner {
        final CharSequence title;
        final MessageObject messageObject;

        ItemInner(CharSequence title, MessageObject messageObject) {
            this.title = title;
            this.messageObject = messageObject;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return items.size() * 2;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return false;
        }

        @Override
        public int getItemViewType(int position) {
            return position % 2 == 0 ? VIEW_TYPE_ACTION : VIEW_TYPE_MESSAGE;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_ACTION) {
                ChatActionCell actionCell = new ChatActionCell(parent.getContext(), false, getResourceProvider());
                actionCell.setDelegate(new ChatActionCell.ChatActionCellDelegate() {
                });
                view = actionCell;
            } else {
                ChatMessageCell messageCell = new ChatMessageCell(parent.getContext(), getCurrentAccount()) {
                    @Override
                    public boolean isDrawSelectionBackground() {
                        return false;
                    }
                };
                messageCell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                    @Override
                    public boolean canPerformActions() {
                        return false;
                    }
                });
                view = messageCell;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position / 2);
            if (holder.getItemViewType() == VIEW_TYPE_ACTION) {
                ((ChatActionCell) holder.itemView).setCustomText(item.title);
            } else {
                ((ChatMessageCell) holder.itemView).setMessageObject(item.messageObject, null, false, false, false);
            }
        }
    }
}
