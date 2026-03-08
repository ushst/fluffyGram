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
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.FluffyLocalLogHook;

import java.util.ArrayList;

public class FluffyDebugActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;

    private static final int ROW_DEBUG_HEADER = 0;
    private static final int ROW_SAVE_LOG = 1;
    private static final int ROW_GOOGLE_CLOUD_HEADER = 2;
    private static final int ROW_GOOGLE_CLOUD_STATUS = 3;
    private static final int ROW_GOOGLE_CLOUD_SERVICES = 4;
    private static final int ROW_GOOGLE_CLOUD_PUSH_TYPE = 5;
    private static final int ROW_GOOGLE_CLOUD_TOKEN = 6;
    private static final int ROW_GOOGLE_CLOUD_STATE = 7;

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
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_GOOGLE_CLOUD_HEADER,
                LocaleController.getString(R.string.FluffyGoogleCloudSection), null));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GOOGLE_CLOUD_STATUS,
                LocaleController.getString(R.string.FluffyGoogleCloudStatus), getGoogleCloudStatusValue()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GOOGLE_CLOUD_SERVICES,
                LocaleController.getString(R.string.FluffyGoogleCloudServices), getGoogleCloudServicesValue()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GOOGLE_CLOUD_PUSH_TYPE,
                LocaleController.getString(R.string.FluffyGoogleCloudPushType), getGoogleCloudPushTypeValue()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GOOGLE_CLOUD_TOKEN,
                LocaleController.getString(R.string.FluffyGoogleCloudToken), getGoogleCloudTokenValue()));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_GOOGLE_CLOUD_STATE,
                LocaleController.getString(R.string.FluffyGoogleCloudState), getGoogleCloudStateValue()));
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

    private String getGoogleCloudServicesValue() {
        boolean hasServices = PushListenerController.GooglePushListenerServiceProvider.INSTANCE.hasServices();
        return hasServices
                ? LocaleController.getString(R.string.FluffyStatusAvailable)
                : LocaleController.getString(R.string.FluffyStatusUnavailable);
    }

    private String getGoogleCloudPushTypeValue() {
        if (SharedConfig.pushType == PushListenerController.PUSH_TYPE_FIREBASE) {
            return "Firebase";
        }
        if (SharedConfig.pushType == PushListenerController.PUSH_TYPE_HUAWEI) {
            return "Huawei";
        }
        return String.valueOf(SharedConfig.pushType);
    }

    private String getGoogleCloudTokenValue() {
        return TextUtils.isEmpty(SharedConfig.pushString)
                ? LocaleController.getString(R.string.FluffyGoogleCloudTokenMissing)
                : maskToken(SharedConfig.pushString);
    }

    private String getGoogleCloudStateValue() {
        return TextUtils.isEmpty(SharedConfig.pushStringStatus)
                ? LocaleController.getString(R.string.FluffyGoogleCloudStateEmpty)
                : SharedConfig.pushStringStatus;
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
            int position = holder.getAdapterPosition();
            return position != RecyclerView.NO_POSITION && items.get(position).id == ROW_SAVE_LOG;
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
            } else {
                TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                if (TextUtils.isEmpty(item.value)) {
                    cell.setText(item.text, false);
                } else {
                    cell.setTextAndValue(item.text, item.value, false);
                }
            }
        }
    }
}
