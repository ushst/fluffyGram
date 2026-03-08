package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.PushListenerController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.FluffyLocalLogHook;

import java.util.ArrayList;

public class FluffyDebugActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_INFO = 2;

    private static final int ROW_DEBUG_HEADER = 0;
    private static final int ROW_SAVE_LOG = 1;
    private static final int ROW_SAVE_LOG_INFO = 2;
    private static final int ROW_GOOGLE_CLOUD_HEADER = 3;
    private static final int ROW_GOOGLE_CLOUD_STATUS = 4;
    private static final int ROW_GOOGLE_CLOUD_INFO = 5;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyDebug));
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
            if (item.id == ROW_SAVE_LOG) {
                FluffyLocalLogHook.onSaveLogClicked(this);
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
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_DEBUG_HEADER,
                LocaleController.getString(R.string.FluffyDebugSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SAVE_LOG,
                LocaleController.getString(R.string.FluffySaveLog), null));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_SAVE_LOG_INFO,
                LocaleController.getString(R.string.FluffySaveLogInfo), null));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_GOOGLE_CLOUD_HEADER,
                LocaleController.getString(R.string.FluffyGoogleCloudSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GOOGLE_CLOUD_STATUS,
                LocaleController.getString(R.string.FluffyGoogleCloudStatus), getGoogleCloudStatusValue()));
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_GOOGLE_CLOUD_INFO,
                getGoogleCloudStatusDetails(), null));
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private String getGoogleCloudStatusValue() {
        boolean hasServices = PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices();
        if (!hasServices) {
            return LocaleController.getString(R.string.FluffyStatusUnavailable);
        }
        if (SharedConfig.pushType != PushListenerController.PUSH_TYPE_FIREBASE) {
            return LocaleController.getString(R.string.FluffyStatusInactive);
        }
        if (!TextUtils.isEmpty(SharedConfig.pushString)) {
            return LocaleController.getString(R.string.FluffyStatusConnected);
        }
        if (TextUtils.equals(SharedConfig.pushStringStatus, "__FIREBASE_FAILED__")) {
            return LocaleController.getString(R.string.FluffyStatusError);
        }
        if (!TextUtils.isEmpty(SharedConfig.pushStringStatus) && SharedConfig.pushStringStatus.contains("GENERATING")) {
            return LocaleController.getString(R.string.FluffyStatusConnecting);
        }
        return LocaleController.getString(R.string.FluffyStatusWaiting);
    }

    private CharSequence getGoogleCloudStatusDetails() {
        boolean hasServices = PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices();
        String pushType;
        if (SharedConfig.pushType == PushListenerController.PUSH_TYPE_FIREBASE) {
            pushType = "Firebase";
        } else if (SharedConfig.pushType == PushListenerController.PUSH_TYPE_HUAWEI) {
            pushType = "Huawei";
        } else {
            pushType = String.valueOf(SharedConfig.pushType);
        }

        String token = TextUtils.isEmpty(SharedConfig.pushString)
                ? LocaleController.getString(R.string.FluffyGoogleCloudTokenMissing)
                : maskToken(SharedConfig.pushString);
        String state = TextUtils.isEmpty(SharedConfig.pushStringStatus)
                ? LocaleController.getString(R.string.FluffyGoogleCloudStateEmpty)
                : SharedConfig.pushStringStatus;

        return String.format(
                java.util.Locale.US,
                "%s: %s\n%s: %s\n%s: %s\n%s: %s",
                LocaleController.getString(R.string.FluffyGoogleCloudServices),
                hasServices ? LocaleController.getString(R.string.FluffyStatusAvailable) : LocaleController.getString(R.string.FluffyStatusUnavailable),
                LocaleController.getString(R.string.FluffyGoogleCloudPushType),
                pushType,
                LocaleController.getString(R.string.FluffyGoogleCloudToken),
                token,
                LocaleController.getString(R.string.FluffyGoogleCloudState),
                state
        );
    }

    private String maskToken(String token) {
        if (TextUtils.isEmpty(token) || token.length() <= 16) {
            return token;
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 8);
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final CharSequence value;

        ItemInner(int viewType, int id, CharSequence text, CharSequence value) {
            this.viewType = viewType;
            this.id = id;
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
            return holder.getItemViewType() == VIEW_TYPE_TEXT;
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
            } else if (viewType == VIEW_TYPE_TEXT) {
                view = new TextSettingsCell(parent.getContext());
                view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            } else {
                view = new TextInfoPrivacyCell(parent.getContext());
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
                ((HeaderCell) holder.itemView).setText(item.text);
            } else if (holder.getItemViewType() == VIEW_TYPE_TEXT) {
                ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, item.value, false);
            } else {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setFixedSize(0);
                cell.setText(item.text);
            }
        }
    }
}
