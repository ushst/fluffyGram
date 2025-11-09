package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.activities.elements.FluffyQuickReplyCell;
import org.ushastoe.fluffy.fluffyConfig;
import org.ushastoe.fluffy.quickreplies.FluffyQuickRepliesManager;
import org.ushastoe.fluffy.quickreplies.FluffyQuickReply;

import java.util.ArrayList;

/**
 * Экран управления кастомными быстрыми командами fluffy.
 */
public class FluffyQuickRepliesActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter adapter;

    private int headerRow;
    private int infoRow;
    private int addRow;
    private int repliesStartRow;
    private int repliesEndRow;
    private int shadowRow;
    private int rowsCount;

    private final ArrayList<FluffyQuickReply> replies = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        replies.clear();
        replies.addAll(FluffyQuickRepliesManager.getInstance().getReplies());
        rowsCount = 0;
        headerRow = rowsCount++;
        infoRow = rowsCount++;
        addRow = rowsCount++;
        repliesStartRow = rowsCount;
        rowsCount += replies.size();
        repliesEndRow = rowsCount;
        shadowRow = rowsCount++;
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.FG_CustomQuickRepliesTitle));
        actionBar.setActionBarMenuOnItemClick(id -> {
            if (id == -1) {
                finishFragment();
            }
        });

        listView = new RecyclerListView(context, resourceProvider);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setVerticalScrollBarEnabled(false);
        listView.setPadding(0, 0, 0, dp(16));
        listView.setClipToPadding(false);
        adapter = new ListAdapter(context);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((view, position) -> {
            if (position == addRow) {
                openEditor(null);
            } else if (position >= repliesStartRow && position < repliesEndRow) {
                FluffyQuickReply reply = replies.get(position - repliesStartRow);
                openEditor(reply);
            }
        });

        fragmentView = new LinearLayout(context);
        ((LinearLayout) fragmentView).setOrientation(LinearLayout.VERTICAL);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        ((LinearLayout) fragmentView).addView(listView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void openEditor(@Nullable FluffyQuickReply existing) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(20);
        container.setPadding(padding, dp(8), padding, 0);

        EditTextBoldCursor prefixField = new EditTextBoldCursor(context);
        prefixField.setHint(LocaleController.getString(R.string.FG_QuickReplyPrefixHint));
        prefixField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider));
        prefixField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourceProvider));
        prefixField.setCursorSize(dp(20));
        prefixField.setTextSize(16);
        prefixField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        prefixField.setSingleLine(true);
        prefixField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
        prefixField.setKeyListener(DigitsKeyListener.getInstance(".!*"));
        container.addView(prefixField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        EditTextBoldCursor commandField = new EditTextBoldCursor(context);
        commandField.setHint(LocaleController.getString(R.string.FG_QuickReplyCommandHint));
        commandField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider));
        commandField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourceProvider));
        commandField.setCursorSize(dp(20));
        commandField.setTextSize(16);
        commandField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        commandField.setSingleLine(true);
        commandField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(32)});
        container.addView(commandField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, dp(12), 0, 0));

        EditTextBoldCursor messageField = new EditTextBoldCursor(context);
        messageField.setHint(LocaleController.getString(R.string.FG_QuickReplyMessageHint));
        messageField.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourceProvider));
        messageField.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourceProvider));
        messageField.setCursorSize(dp(20));
        messageField.setTextSize(16);
        messageField.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        messageField.setMinLines(3);
        messageField.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2048)});
        container.addView(messageField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, dp(12), 0, 0));

        if (existing != null) {
            prefixField.setText(String.valueOf(existing.prefix));
            commandField.setText(existing.command);
            messageField.setText(existing.message);
        } else {
            prefixField.setText(".");
        }
        prefixField.setSelection(prefixField.length());

        AlertDialog dialog = new AlertDialog.Builder(context, resourceProvider)
                .setTitle(existing == null ? LocaleController.getString(R.string.FG_QuickReplyAddTitle) : LocaleController.getString(R.string.FG_QuickReplyEditTitle))
                .setView(container)
                .setPositiveButton(LocaleController.getString(R.string.Save), null)
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .setNeutralButton(existing == null ? 0 : R.string.Delete, (d, which) -> {
                    if (existing != null) {
                        FluffyQuickRepliesManager.getInstance().delete(existing.id);
                        updateRows();
                    }
                })
                .create();

        dialog.setOnShowListener(di -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String prefixText = prefixField.getText().toString().trim();
                if (TextUtils.isEmpty(prefixText)) {
                    prefixText = existing != null ? String.valueOf(existing.prefix) : ".";
                }
                char prefix = prefixText.charAt(0);
                if (!FluffyQuickRepliesManager.isSupportedPrefix(prefix)) {
                    BulletinHelper.showSimpleBulletin(this, LocaleController.getString(R.string.FG_QuickReplyPrefixError), null);
                    return;
                }

                String command = commandField.getText().toString().trim();
                if (TextUtils.isEmpty(command)) {
                    BulletinHelper.showSimpleBulletin(this, LocaleController.getString(R.string.FG_QuickReplyCommandError), null);
                    return;
                }
                if (!command.matches("[\\p{L}\\p{N}_-]+")) {
                    BulletinHelper.showSimpleBulletin(this, LocaleController.getString(R.string.FG_QuickReplyCommandAllowedChars), null);
                    return;
                }

                String message = messageField.getText().toString();
                if (TextUtils.isEmpty(message.trim())) {
                    BulletinHelper.showSimpleBulletin(this, LocaleController.getString(R.string.FG_QuickReplyMessageError), null);
                    return;
                }

                FluffyQuickRepliesManager manager = FluffyQuickRepliesManager.getInstance();
                int excludeId = existing != null ? existing.id : 0;
                if (manager.hasDuplicate(excludeId, prefix, command)) {
                    BulletinHelper.showSimpleBulletin(this, LocaleController.getString(R.string.FG_QuickReplyDuplicateError), null);
                    return;
                }

                FluffyQuickReply reply = existing != null ? existing.copy() : new FluffyQuickReply();
                reply.prefix = prefix;
                reply.command = command;
                reply.message = message;
                manager.addOrUpdate(reply);
                if (!fluffyConfig.enableCustomQuickReplies) {
                    fluffyConfig.toggleCustomQuickReplies();
                }
                dialog.dismiss();
                updateRows();
            });
        });

        showDialog(dialog);
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return rowsCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return false;
            }
            return position == addRow || (position >= repliesStartRow && position < repliesEndRow);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == headerRow) {
                HeaderCell cell = (HeaderCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.FG_CustomQuickRepliesHeader));
            } else if (position == infoRow) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setText(LocaleController.getString(R.string.FG_CustomQuickRepliesInfo));
                cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (position == addRow) {
                TextCell cell = (TextCell) holder.itemView;
                cell.setTextAndIcon(LocaleController.getString(R.string.FG_QuickReplyAddButton), R.drawable.msg_add, false);
                cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (position >= repliesStartRow && position < repliesEndRow) {
                FluffyQuickReplyCell cell = (FluffyQuickReplyCell) holder.itemView;
                int index = position - repliesStartRow;
                boolean divider = index != replies.size() - 1;
                cell.set(replies.get(index), replies.get(index).prefix, null, divider);
            } else if (position == shadowRow) {
                holder.itemView.setBackground(Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            if (viewType == 0) {
                view = new HeaderCell(context);
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else if (viewType == 1) {
                view = new TextInfoPrivacyCell(context);
            } else if (viewType == 2) {
                view = new TextCell(context);
            } else if (viewType == 3) {
                view = new FluffyQuickReplyCell(context, resourceProvider);
            } else {
                view = new ShadowSectionCell(context);
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == headerRow) {
                return 0;
            } else if (position == infoRow) {
                return 1;
            } else if (position == addRow) {
                return 2;
            } else if (position >= repliesStartRow && position < repliesEndRow) {
                return 3;
            } else {
                return 4;
            }
        }
    }
}
