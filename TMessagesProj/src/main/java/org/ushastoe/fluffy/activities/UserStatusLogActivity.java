package org.ushastoe.fluffy.activities;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ProfileActivity;
import org.ushastoe.fluffy.storage.UserStatusStorage;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserStatusLogActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int MENU_REFRESH = 1;
    private static final int MENU_CLEAR = 2;
    private static final int MAX_ROWS = 200;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private TextView emptyView;
    private final ArrayList<UserStatusStorage.LogEntry> items = new ArrayList<>();
    private long lastLoadedAt;
    private long lastRowCount;
    private AlertDialog historyDialog;
    private TextView historyDialogMessageView;
    private long historyDialogUserId;
    private int historyDialogAccountId;
    private String historyDialogTitle;
    private BackupImageView historyDialogAvatarView;
    private final AvatarDrawable historyDialogAvatarDrawable = new AvatarDrawable();
    private TextView historyDialogNameView;
    private UserStatusStorage.LogEntry historyDialogInitialEntry;


    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.userStatusLogUpdated);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.userStatusLogUpdated);
        super.onFragmentDestroy();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("UserStatusLogTitle", R.string.UserStatusLogTitle));
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem optionsItem = menu.addItem(0, R.drawable.ic_ab_other);
        optionsItem.setContentDescription(LocaleController.getString("AccDescrMoreOptions", R.string.AccDescrMoreOptions));
        optionsItem.addSubItem(MENU_REFRESH, R.drawable.menu_browser_refresh, LocaleController.getString("Refresh", R.string.Refresh));
        ActionBarMenuSubItem clearSubItem = optionsItem.addSubItem(MENU_CLEAR, R.drawable.msg_delete, LocaleController.getString("UserStatusLogClear", R.string.UserStatusLogClear));
        clearSubItem.setIconColor(Theme.getColor(Theme.key_text_RedRegular));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_REFRESH) {
                    reloadData(true);
                } else if (id == MENU_CLEAR) {
                    showClearDialog();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = frameLayout;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < items.size()) {
                showHistory(items.get(position));
            }
        });

        emptyView = new TextView(context);
        emptyView.setText(LocaleController.getString("UserStatusLogEmpty", R.string.UserStatusLogEmpty));
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        emptyView.setTextSize(16);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setTypeface(Typeface.DEFAULT_BOLD);
        emptyView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24), AndroidUtilities.dp(24));
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setEmptyView(emptyView);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        reloadData(false);
        return fragmentView;
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.userStatusLogUpdated) {
            reloadData(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSubtitle();
    }

    private void reloadData(boolean manualRequest) {
        Utilities.globalQueue.postRunnable(() -> {
            UserStatusStorage storage = UserStatusStorage.getInstance(ApplicationLoader.applicationContext);
            List<UserStatusStorage.LogEntry> latest = storage.getLatestPerUser(MAX_ROWS);
            long totalCount = storage.getEntryCount();
            AndroidUtilities.runOnUIThread(() -> {
                if (isFinishing() || listAdapter == null) {
                    return;
                }
                items.clear();
                items.addAll(latest);
                listAdapter.notifyDataSetChanged();
                lastLoadedAt = System.currentTimeMillis();
                lastRowCount = totalCount;
                updateSubtitle();
                if (manualRequest && listView != null && !items.isEmpty()) {
                    listView.smoothScrollToPosition(0);
                }
                refreshHistoryDialog();
            });
        });
    }


    private void showClearDialog() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("UserStatusLogClearTitle", R.string.UserStatusLogClearTitle));
        builder.setMessage(LocaleController.getString("UserStatusLogClearText", R.string.UserStatusLogClearText));
        builder.setPositiveButton(LocaleController.getString("Clear", R.string.Clear), (dialog, which) -> clearLog());
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void clearLog() {
        if (historyDialog != null) {
            historyDialog.dismiss();
        }
        Utilities.globalQueue.postRunnable(() -> {
            UserStatusStorage.getInstance(ApplicationLoader.applicationContext).clearAll();
            AndroidUtilities.runOnUIThread(() -> reloadData(false));
        });
    }

    private void showHistory(UserStatusStorage.LogEntry entry) {
        if (getParentActivity() == null) {
            return;
        }
        historyDialogUserId = entry.userId;
        historyDialogAccountId = entry.accountId;
        historyDialogInitialEntry = entry;
        String baseTitle = LocaleController.getString("UserStatusLogHistoryTitle", R.string.UserStatusLogHistoryTitle);
        historyDialogTitle = baseTitle + " - " + buildTitle(entry);

        Context context = getParentActivity();
        ScrollView scrollView = new ScrollView(context);
        scrollView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(16));

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(LocaleController.isRTL ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        header.setPadding(0, 0, 0, AndroidUtilities.dp(12));

        historyDialogAvatarView = new BackupImageView(context);
        historyDialogAvatarView.setRoundRadius(AndroidUtilities.dp(24));
        historyDialogAvatarView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48));
        if (LocaleController.isRTL) {
            avatarParams.leftMargin = AndroidUtilities.dp(12);
        } else {
            avatarParams.rightMargin = AndroidUtilities.dp(12);
        }
        header.addView(historyDialogAvatarView, avatarParams);

        historyDialogNameView = new TextView(context);
        historyDialogNameView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        historyDialogNameView.setTextSize(16);
        historyDialogNameView.setTypeface(Typeface.DEFAULT_BOLD);
        historyDialogNameView.setSingleLine(true);
        historyDialogNameView.setEllipsize(TextUtils.TruncateAt.END);
        historyDialogNameView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(historyDialogNameView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        historyDialogAvatarView.setOnClickListener(v -> {
            if (historyDialogUserId != 0) {
                openProfile(historyDialogUserId, historyDialogAccountId);
            }
        });

        container.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        historyDialogMessageView = new TextView(context);
        historyDialogMessageView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        historyDialogMessageView.setTextSize(16);
        historyDialogMessageView.setLineSpacing(AndroidUtilities.dp(2), 1.1f);
        historyDialogMessageView.setTextIsSelectable(true);
        historyDialogMessageView.setMovementMethod(new ScrollingMovementMethod());
        historyDialogMessageView.setText(LocaleController.getString("Loading", R.string.Loading));
        container.addView(historyDialogMessageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        scrollView.addView(container, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(historyDialogTitle);
        builder.setView(scrollView);
        builder.setPositiveButton(LocaleController.getString("UserStatusLogOpenProfile", R.string.UserStatusLogOpenProfile), (dialog, which) -> openProfile(historyDialogUserId, historyDialogAccountId));
        builder.setNegativeButton(LocaleController.getString("Close", R.string.Close), null);

        AlertDialog dialog = builder.create();
        dialog.setOnDismissListener(dismissDialog -> {
            if (historyDialog == dialog) {
                historyDialog = null;
                historyDialogMessageView = null;
                historyDialogUserId = 0;
                historyDialogAccountId = 0;
                historyDialogTitle = null;
                historyDialogAvatarView = null;
                historyDialogNameView = null;
                historyDialogInitialEntry = null;
            }
        });
        historyDialog = dialog;
        showDialog(dialog);
        bindHistoryHeader(entry);
        refreshHistoryDialog();
    }

    private void refreshHistoryDialog() {
        if (historyDialog == null || historyDialogUserId == 0) {
            return;
        }
        final long userId = historyDialogUserId;
        final int accountId = historyDialogAccountId;
        Utilities.globalQueue.postRunnable(() -> {
            List<UserStatusStorage.LogEntry> history = UserStatusStorage.getInstance(ApplicationLoader.applicationContext)
                    .getHistoryForUser(userId, accountId, 20);
            AndroidUtilities.runOnUIThread(() -> {
                if (historyDialog == null || historyDialogUserId != userId || historyDialogAccountId != accountId) {
                    return;
                }
                String title;
                if (!history.isEmpty()) {
                    title = LocaleController.getString("UserStatusLogHistoryTitle", R.string.UserStatusLogHistoryTitle) + " - " + buildTitle(history.get(0));
                    historyDialogTitle = title;
                    bindHistoryHeader(history.get(0));
                } else {
                    title = historyDialogTitle != null ? historyDialogTitle
                            : LocaleController.getString("UserStatusLogHistoryTitle", R.string.UserStatusLogHistoryTitle);
                    bindHistoryHeader(historyDialogInitialEntry);
                }
                historyDialog.setTitle(title);
                String message = buildHistoryMessage(history);
                if (historyDialogMessageView != null) {
                    historyDialogMessageView.setText(message);
                } else {
                    historyDialog.setMessage(message);
                }
            });
        });
    }

    private String buildHistoryMessage(List<UserStatusStorage.LogEntry> history) {
        if (history == null || history.isEmpty()) {
            return LocaleController.getString("UserStatusLogHistoryEmpty", R.string.UserStatusLogHistoryEmpty);
        }
        StringBuilder message = new StringBuilder();
        for (UserStatusStorage.LogEntry item : history) {
            if (message.length() > 0) {
                message.append('\n');
            }
            String status = buildStatusLine(item);
            message.append(LocaleController.formatString("UserStatusLogHistoryLine", R.string.UserStatusLogHistoryLine,
                    formatDateTime(item.updatedAt), status));
        }
        return message.toString();
    }


    private void openProfile(long userId, int accountId) {
        if (userId == 0 || getParentActivity() == null) {
            return;
        }
        Bundle args = new Bundle();
        args.putLong("user_id", userId);
        ProfileActivity fragment = new ProfileActivity(args);
        fragment.setCurrentAccount(accountId);
        presentFragment(fragment);
    }

    private void updateSubtitle() {
        if (actionBar == null) {
            return;
        }
        if (lastLoadedAt == 0) {
            actionBar.setSubtitle(null);
            return;
        }
        String timeText = formatDateTime(lastLoadedAt);
        String countText = LocaleController.formatNumber(lastRowCount, ' ');
        actionBar.setSubtitle(LocaleController.formatString("UserStatusLogSubtitle", R.string.UserStatusLogSubtitle, timeText, countText));
    }


    private String buildStatusLine(UserStatusStorage.LogEntry entry) {
        String status = entry.statusText != null ? entry.statusText :
                LocaleController.getString("UserStatusLogLastSeenUnknown", R.string.UserStatusLogLastSeenUnknown);
        if (!TextUtils.isEmpty(entry.actionText)) {
            status = LocaleController.formatString("UserStatusLogStatusWithAction", R.string.UserStatusLogStatusWithAction,
                    status, entry.actionText);
        }
        return status;
    }

    private CharSequence buildValue(UserStatusStorage.LogEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(buildStatusLine(entry));
        builder.append('\n');
        builder.append(LocaleController.formatString("UserStatusLogUpdatedAt", R.string.UserStatusLogUpdatedAt,
                formatDateTime(entry.updatedAt)));
        if (entry.isOnline && entry.statusExpiresAt > 0) {
            builder.append('\n');
            builder.append(LocaleController.formatString("UserStatusLogStatusExpires", R.string.UserStatusLogStatusExpires,
                    formatDateTime(entry.statusExpiresAt)));
        } else if (entry.lastSeenAt > 0) {
            builder.append('\n');
            builder.append(LocaleController.formatString("UserStatusLogLastSeenAt", R.string.UserStatusLogLastSeenAt,
                    formatDateTime(entry.lastSeenAt)));
        }
        builder.append('\n');
        builder.append(LocaleController.formatString("UserStatusLogAccountLabel", R.string.UserStatusLogAccountLabel,
                entry.accountId + 1));
        if (!TextUtils.isEmpty(entry.statusClass)) {
            builder.append('\n');
            builder.append(LocaleController.formatString("UserStatusLogStatusClass", R.string.UserStatusLogStatusClass,
                    entry.statusClass));
        }
        builder.append('\n');
        builder.append("ID: ").append(entry.userId);
        builder.append("  •  mask: ").append(entry.updateMask);
        return builder.toString();
    }

    private String buildTitle(UserStatusStorage.LogEntry entry) {
        String name = entry.userName;
        if (TextUtils.isEmpty(name)) {
            name = LocaleController.formatString("UserStatusLogAccountLabel", R.string.UserStatusLogAccountLabel,
                    entry.accountId + 1);
        }
        return name;
    }

    private String formatDateTime(long millis) {
        Date date = new Date(millis);
        CharSequence time = DateFormat.format("HH:mm", date);
        return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime,
                LocaleController.getInstance().getFormatterDayMonth().format(date),
                time.toString());
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context context;

        ListAdapter(Context context) {
            this.context = context;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LogEntryCell cell = new LogEntryCell(context);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            LogEntryCell cell = (LogEntryCell) holder.itemView;
            UserStatusStorage.LogEntry entry = items.get(position);
            cell.setTextAndValue(buildTitle(entry), buildValue(entry), position != items.size() - 1);
            cell.bind(entry);
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> descriptions = new ArrayList<>();
        descriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR,
                new Class[]{TextDetailCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        descriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null,
                Theme.key_windowBackgroundGray));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null,
                Theme.key_actionBarDefault));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null,
                Theme.key_actionBarDefaultTitle));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null,
                Theme.key_actionBarDefaultIcon));
        descriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null,
                Theme.key_actionBarDefaultSelector));
        descriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null,
                Theme.key_actionBarDefault));
        descriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null,
                Theme.key_windowBackgroundWhiteGrayText2));
        return descriptions;
    }

    private class LogEntryCell extends TextDetailCell {

        private final BackupImageView avatarImageView;
        private final AvatarDrawable avatarDrawable = new AvatarDrawable();
        private long boundUserId;
        private int boundAccountId;

        LogEntryCell(Context context) {
            super(context, null, true);
            avatarImageView = new BackupImageView(context);
            avatarImageView.setRoundRadius(AndroidUtilities.dp(24));
            avatarImageView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(avatarImageView, buildAvatarLayoutParams());

            if (getImageView() != null) {
                getImageView().setVisibility(GONE);
            }

            avatarImageView.setOnClickListener(v -> {
                if (boundUserId != 0) {
                    openProfile(boundUserId, boundAccountId);
                }
            });

            adjustTextInsets();
        }

        void bind(UserStatusStorage.LogEntry entry) {
            boundUserId = entry.userId;
            boundAccountId = entry.accountId;

            adjustForLocale();

            TLRPC.User user = MessagesController.getInstance(boundAccountId).getUser(boundUserId);
            if (user != null) {
                avatarDrawable.setInfo(boundAccountId, user);
                avatarImageView.setForUserOrChat(user, avatarDrawable, user);
            } else {
                String displayName = entry.userName;
                String firstName = null;
                String lastName = null;
                if (!TextUtils.isEmpty(displayName)) {
                    String[] parts = displayName.split(" ", 2);
                    firstName = parts[0];
                    if (parts.length > 1) {
                        lastName = parts[1];
                    }
                }
                avatarDrawable.setInfo(entry.userId, firstName, lastName);
                avatarImageView.setImageDrawable(avatarDrawable);
            }

            if (!TextUtils.isEmpty(entry.userName)) {
                avatarImageView.setContentDescription(entry.userName);
            } else {
                avatarImageView.setContentDescription(String.valueOf(entry.userId));
            }
        }

        private void adjustForLocale() {
            FrameLayout.LayoutParams avatarParams = buildAvatarLayoutParams();
            avatarImageView.setLayoutParams(avatarParams);
            adjustTextInsets();
        }

        private void adjustTextInsets() {
            int extra = AndroidUtilities.dp(48 + 12);
            int baseTitle = AndroidUtilities.dp(23 - 6);
            int baseValue = AndroidUtilities.dp(23);
            FrameLayout.LayoutParams titleParams = (FrameLayout.LayoutParams) textView.getLayoutParams();
            FrameLayout.LayoutParams valueParams = (FrameLayout.LayoutParams) valueTextView.getLayoutParams();
            if (LocaleController.isRTL) {
                titleParams.rightMargin = baseTitle + extra;
                titleParams.leftMargin = baseTitle;
                valueParams.rightMargin = baseValue + extra;
                valueParams.leftMargin = baseValue;
            } else {
                titleParams.leftMargin = baseTitle + extra;
                titleParams.rightMargin = baseTitle;
                valueParams.leftMargin = baseValue + extra;
                valueParams.rightMargin = baseValue;
            }
            textView.setLayoutParams(titleParams);
            valueTextView.setLayoutParams(valueParams);
        }

        private FrameLayout.LayoutParams buildAvatarLayoutParams() {
            float start = LocaleController.isRTL ? 0 : 23f;
            float end = LocaleController.isRTL ? 23f : 0;
            return LayoutHelper.createFrameRelatively(48, 48,
                    (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL,
                    start, 6f, end, 0f);
        }
    }

    private void bindHistoryHeader(UserStatusStorage.LogEntry entry) {
        if (historyDialogAvatarView == null || historyDialogNameView == null) {
            return;
        }
        if (entry == null) {
            historyDialogAvatarView.setImageDrawable(null);
            historyDialogNameView.setText(null);
            historyDialogAvatarView.setContentDescription(null);
            return;
        }
        historyDialogNameView.setText(buildTitle(entry));

        CharSequence contentDescription = !TextUtils.isEmpty(entry.userName) ? entry.userName : String.valueOf(entry.userId);
        historyDialogAvatarView.setContentDescription(contentDescription);

        TLRPC.User user = MessagesController.getInstance(entry.accountId).getUser(entry.userId);
        if (user != null) {
            historyDialogAvatarDrawable.setInfo(entry.accountId, user);
            historyDialogAvatarView.setForUserOrChat(user, historyDialogAvatarDrawable, user);
        } else {
            String displayName = entry.userName;
            String firstName = null;
            String lastName = null;
            if (!TextUtils.isEmpty(displayName)) {
                String[] parts = displayName.split(" ", 2);
                firstName = parts[0];
                if (parts.length > 1) {
                    lastName = parts[1];
                }
            }
            historyDialogAvatarDrawable.setInfo(entry.userId, firstName, lastName);
            historyDialogAvatarView.setImageDrawable(historyDialogAvatarDrawable);
        }
    }
}
