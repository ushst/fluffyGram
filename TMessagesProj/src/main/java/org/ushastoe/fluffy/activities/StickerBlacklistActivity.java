package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
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

    listAdapter = new ListAdapter(context);

    fragmentView = new FrameLayout(context);
    fragmentView.setBackgroundColor(
        Theme.getColor(Theme.key_windowBackgroundGray));

    listView = new RecyclerListView(context);
    listView.setLayoutManager(new LinearLayoutManager(context));
    listView.setVerticalScrollBarEnabled(false);
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

  private static class ListAdapter extends RecyclerListView.SelectionAdapter {

    private final Context context;

    ListAdapter(Context context) { this.context = context; }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
      return true;
    }

    @Override
    public int getItemCount() {
      return fluffyConfig.blockSticker.size();
    }

    @Override
    public RecyclerListView.Holder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
      TextCell cell = new TextCell(context);
      cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
      return new RecyclerListView.Holder(cell);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      TextCell cell = (TextCell)holder.itemView;
      long id = fluffyConfig.blockSticker.get(position);
      cell.setText(String.valueOf(id), position != getItemCount() - 1);
    }
  }

  @Override
  public ArrayList<ThemeDescription> getThemeDescriptions() {
    ArrayList<ThemeDescription> descriptions = new ArrayList<>();
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
