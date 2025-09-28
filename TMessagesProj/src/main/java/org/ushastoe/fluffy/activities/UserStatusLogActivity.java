package org.ushastoe.fluffy.activities;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.storage.UserStatusStorage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UserStatusLogActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private static final int MENU_REFRESH = 1;
    private static final int MAX_ROWS = 200;

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private TextView emptyView;
    private final ArrayList<UserStatusStorage.LogEntry> items = new ArrayList<>();
    private long lastLoadedAt;

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
        ActionBarMenuItem refreshItem = menu.addItem(MENU_REFRESH, R.drawable.menu_browser_refresh);
        refreshItem.setContentDescription(LocaleController.getString("Refresh", R.string.Refresh));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_REFRESH) {
                    reloadData(true);
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
            List<UserStatusStorage.LogEntry> latest = UserStatusStorage.getInstance(ApplicationLoader.applicationContext)
                    .getLatestPerUser(MAX_ROWS);
            AndroidUtilities.runOnUIThread(() -> {
                if (isFinishing() || listAdapter == null) {
                    return;
                }
                items.clear();
                items.addAll(latest);
                listAdapter.notifyDataSetChanged();
                lastLoadedAt = System.currentTimeMillis();
                updateSubtitle();
                if (manualRequest && listView != null && !items.isEmpty()) {
                    listView.smoothScrollToPosition(0);
                }
            });
        });
    }

    private void showHistory(UserStatusStorage.LogEntry entry) {
        Utilities.globalQueue.postRunnable(() -> {
            List<UserStatusStorage.LogEntry> history = UserStatusStorage.getInstance(ApplicationLoader.applicationContext)
                    .getHistoryForUser(entry.userId, entry.accountId, 20);
            AndroidUtilities.runOnUIThread(() -> {
                if (getParentActivity() == null || history.isEmpty()) {
                    return;
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
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("UserStatusLogHistoryTitle", R.string.UserStatusLogHistoryTitle));
                builder.setMessage(message.toString());
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                showDialog(builder.create());
            });
        });
    }

    private void updateSubtitle() {
        if (actionBar == null || lastLoadedAt == 0) {
            return;
        }
        actionBar.setSubtitle(LocaleController.formatString("UserStatusLogLastUpdate", R.string.UserStatusLogLastUpdate,
                formatDateTime(lastLoadedAt)));
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
        return LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime,
                LocaleController.getInstance().getFormatterDayMonth().format(date),
                LocaleController.getInstance().getFormatterDay().format(date));
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
            TextDetailCell cell = new TextDetailCell(context, null, true);
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            TextDetailCell cell = (TextDetailCell) holder.itemView;
            UserStatusStorage.LogEntry entry = items.get(position);
            cell.setTextAndValue(buildTitle(entry), buildValue(entry), position != items.size() - 1);
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
}
