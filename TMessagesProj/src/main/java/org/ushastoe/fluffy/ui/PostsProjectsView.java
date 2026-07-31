package org.ushastoe.fluffy.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.patches.PostsBlacklistPatch;
import org.ushastoe.fluffy.patches.PostsSearchHistoryPatch;

import java.util.ArrayList;

/**
 * Saved posts searches shown as tiles: every executed query is kept as a project, so a search can
 * be reopened in one tap together with the channels hidden inside it.
 */
public class PostsProjectsView extends FrameLayout {

    private final int currentAccount;
    private final Theme.ResourcesProvider resourcesProvider;

    private final RecyclerListView listView;
    private final Adapter adapter;
    private final TextView emptyView;

    private final ArrayList<String> projects = new ArrayList<>();

    private Utilities.Callback<String> onProjectClick;

    public PostsProjectsView(Context context, int currentAccount, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        this.currentAccount = currentAccount;
        this.resourcesProvider = resourcesProvider;

        listView = new RecyclerListView(context, resourcesProvider);
        listView.setLayoutManager(new GridLayoutManager(context, 2));
        listView.setClipToPadding(false);
        listView.setPadding(dp(8), dp(8), dp(8), dp(8));
        listView.setAdapter(adapter = new Adapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= projects.size() || onProjectClick == null) {
                return;
            }
            onProjectClick.run(projects.get(position));
        });
        listView.setOnItemLongClickListener((view, position) -> {
            if (position < 0 || position >= projects.size()) {
                return false;
            }
            askRemove(projects.get(position));
            return true;
        });
        addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        emptyView = new TextView(context);
        emptyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        emptyView.setGravity(Gravity.CENTER);
        addView(emptyView, LayoutHelper.createFrame(220, LayoutHelper.WRAP_CONTENT, Gravity.CENTER));

        updateColors();
        update();
    }

    public void setOnProjectClick(Utilities.Callback<String> listener) {
        this.onProjectClick = listener;
    }

    public void updateColors() {
        emptyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        emptyView.setText(getString(R.string.FluffyPostsProjectsEmpty));
        adapter.notifyDataSetChanged();
    }

    public void setPagesPaddings(int top, int bottom) {
        listView.setPadding(dp(8), top + dp(8), dp(8), bottom + dp(8));
        emptyView.setTranslationY((top - bottom) / 2f);
    }

    public void update() {
        projects.clear();
        projects.addAll(PostsSearchHistoryPatch.getQueries(currentAccount));
        adapter.notifyDataSetChanged();
        emptyView.setVisibility(projects.isEmpty() ? View.VISIBLE : View.GONE);
        listView.setVisibility(projects.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void askRemove(String query) {
        new AlertDialog.Builder(getContext(), resourcesProvider)
            .setTitle(getString(R.string.FluffyPostsProjectRemoveTitle))
            .setMessage(formatString(R.string.FluffyPostsProjectRemoveText, query))
            .setPositiveButton(getString(R.string.ClearSearchRemove), (dialog, which) -> {
                PostsSearchHistoryPatch.removeQuery(currentAccount, query);
                update();
            })
            .setNegativeButton(getString(R.string.Cancel), null)
            .show();
    }

    private class Adapter extends RecyclerListView.SelectionAdapter {

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new TileView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((TileView) holder.itemView).set(projects.get(position));
        }

        @Override
        public int getItemCount() {
            return projects.size();
        }
    }

    private class TileView extends LinearLayout {

        private final TextView titleView;
        private final TextView subtitleView;

        public TileView(Context context) {
            super(context);

            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setPadding(dp(14), dp(14), dp(14), dp(14));

            titleView = new TextView(context);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setTypeface(AndroidUtilities.bold());
            titleView.setMaxLines(2);
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            subtitleView = new TextView(context);
            subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            subtitleView.setSingleLine();
            subtitleView.setEllipsize(TextUtils.TruncateAt.END);
            addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

            updateColors();
        }

        private void updateColors() {
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            subtitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            setBackground(Theme.createRoundRectDrawable(dp(12), Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider)));
        }

        public void set(String query) {
            updateColors();
            titleView.setText(query);
            final int hidden = PostsBlacklistPatch.getHidden(currentAccount, query, false).size();
            subtitleView.setText(hidden > 0
                ? formatString(R.string.FluffyPostsHiddenCount, hidden)
                : getString(R.string.FluffyPostsProjectOpen));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(dp(92), MeasureSpec.EXACTLY));
        }

        @Override
        public void setLayoutParams(ViewGroup.LayoutParams params) {
            if (params instanceof MarginLayoutParams) {
                ((MarginLayoutParams) params).setMargins(dp(4), dp(4), dp(4), dp(4));
            }
            super.setLayoutParams(params);
        }
    }
}
