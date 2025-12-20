package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.StickerCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.BulletinHelper;
import org.ushastoe.fluffy.fluffyConfig;

public class StickerBlacklistActivity extends BaseFragment {

  private RecyclerListView listView;
  private ListAdapter listAdapter;

  @Override
  public View createView(Context context) {
    actionBar.setBackButtonImage(R.drawable.ic_ab_back);
    actionBar.setTitle(getString(R.string.StickerBlacklist));
    actionBar.setActionBarMenuOnItemClick(
        new ActionBar.ActionBarMenuOnItemClick() {
          @Override
          public void onItemClick(int id) {
            if (id == -1) {
              finishFragment();
            }
          }
        });

    listAdapter = new ListAdapter(context, getResourceProvider());

    fragmentView = new FrameLayout(context);
    fragmentView.setBackgroundColor(
        Theme.getColor(Theme.key_windowBackgroundGray));

    listView = new RecyclerListView(context);
    final int spanCount = calculateSpanCount();
    GridLayoutManager layoutManager = new GridLayoutManager(context, spanCount);
    layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      @Override
      public int getSpanSize(int position) {
        if (listAdapter == null || spanCount <= 1) {
          return 1;
        }
        return listAdapter.getItemViewType(position) ==
                       ListAdapter.VIEW_TYPE_PLACEHOLDER
                   ? spanCount
                   : 1;
      }
    });
    listView.setLayoutManager(layoutManager);
    listView.setVerticalScrollBarEnabled(false);
    int padding = AndroidUtilities.dp(8);
    listView.setPadding(padding, padding, padding, padding);
    listView.setClipToPadding(false);
    listView.setItemAnimator(null);
    ((FrameLayout)fragmentView)
        .addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                                    LayoutHelper.MATCH_PARENT));
    listView.setAdapter(listAdapter);

    listView.setOnItemClickListener((view, position) -> {
      long id = fluffyConfig.blockSticker.get(position);
      fluffyConfig.removeBlockedSticker(id);
      listAdapter.notifyDataSetChanged();
      BulletinHelper.showSimpleBulletin(
          this, getString(R.string.StickerRemovedFromBlacklist), null);
    });

    return fragmentView;
  }

  private int calculateSpanCount() {
    int available = AndroidUtilities.displaySize.x - AndroidUtilities.dp(16);
    int cellSize = AndroidUtilities.dp(92);
    if (cellSize <= 0) {
      cellSize = AndroidUtilities.dp(72);
    }
    return Math.max(1, available / cellSize);
  }

  private static class ListAdapter extends RecyclerListView.SelectionAdapter {

    static final int VIEW_TYPE_STICKER = 0;
    static final int VIEW_TYPE_PLACEHOLDER = 1;

    private final Context context;
    private final Theme.ResourcesProvider resourcesProvider;

    ListAdapter(Context context, Theme.ResourcesProvider resourcesProvider) {
      this.context = context;
      this.resourcesProvider = resourcesProvider;
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
      return true;
    }

    @Override
    public int getItemCount() {
      return fluffyConfig.blockSticker.size();
    }

    @Override
    public int getItemViewType(int position) {
      long id = fluffyConfig.blockSticker.get(position);
      return fluffyConfig.getBlockedStickerDocument(id) != null
                 ? VIEW_TYPE_STICKER
                 : VIEW_TYPE_PLACEHOLDER;
    }

    @Override
    public RecyclerListView.Holder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
      if (viewType == VIEW_TYPE_STICKER) {
        StickerCell cell = new StickerCell(context, resourcesProvider);
        cell.setBackground(Theme.getSelectorDrawable(false, resourcesProvider));
        RecyclerView.LayoutParams params =
            new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                          AndroidUtilities.dp(96));
        cell.setLayoutParams(params);
        cell.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));
        return new RecyclerListView.Holder(cell);
      } else {
        TextCell cell = new TextCell(context);
        cell.setBackgroundColor(
            Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider));
        return new RecyclerListView.Holder(cell);
      }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      long id = fluffyConfig.blockSticker.get(position);
      if (holder.getItemViewType() == VIEW_TYPE_STICKER) {
        StickerCell cell = (StickerCell)holder.itemView;
        TLRPC.Document document = fluffyConfig.getBlockedStickerDocument(id);
        cell.setSticker(document, null);
      } else {
        TextCell cell = (TextCell)holder.itemView;
        cell.setTextAndValue(
            getString(R.string.StickerBlacklistMissingData),
            String.valueOf(id), position != getItemCount() - 1);
      }
    }
  }

  @Override
  public ArrayList<ThemeDescription> getThemeDescriptions() {
    ArrayList<ThemeDescription> descriptions = new ArrayList<>();
    descriptions.add(new ThemeDescription(
        listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR,
        new Class[] {StickerCell.class}, null, null, null,
        Theme.key_windowBackgroundWhite));
    descriptions.add(new ThemeDescription(
        listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR,
        new Class[] {TextCell.class}, null, null, null,
        Theme.key_windowBackgroundWhite));
    descriptions.add(new ThemeDescription(
        fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null,
        Theme.key_windowBackgroundGray));
    descriptions.add(
        new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null,
                             null, null, null, Theme.key_actionBarDefault));
    descriptions.add(new ThemeDescription(
        actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null,
        Theme.key_actionBarDefaultIcon));
    descriptions.add(new ThemeDescription(
        actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null,
        Theme.key_actionBarDefaultTitle));
    descriptions.add(new ThemeDescription(
        actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null,
        null, Theme.key_actionBarDefaultSelector));
    return descriptions;
  }
}
