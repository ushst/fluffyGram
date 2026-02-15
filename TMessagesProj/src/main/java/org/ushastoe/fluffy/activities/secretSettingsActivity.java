package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.messenger.UserConfig;
import org.ushastoe.fluffy.helpers.SecretSettingsHelper;
import org.ushastoe.fluffy.activities.ghostModeActivitySettings;
import org.ushastoe.fluffy.activities.elements.BaseFluffySettingsActivity;
import org.ushastoe.fluffy.activities.elements.FluffySettingsScaffold;

public class secretSettingsActivity extends BaseFluffySettingsActivity {

  private RecyclerListView listView;
  private ListAdapter listAdapter;
  private final List<Row> rows = new ArrayList<>();

  private enum RowType { HEADER, TEXT_INFO, TEXT_CELL }

  private enum RowIdentifier { SECRET_HEADER, SECRET_DESCRIPTION, SECRET_GHOST_MODE, SECRET_DISABLE }

  private static class Row {
    final RowIdentifier id;
    final RowType type;
    final int textResId;

    Row(RowIdentifier id, RowType type, int textResId) {
      this.id = id;
      this.type = type;
      this.textResId = textResId;
    }
  }

  @Override
  public boolean onFragmentCreate() {
    super.onFragmentCreate();
    updateRows();
    return true;
  }

  private void updateRows() {
    rows.clear();
    rows.add(new Row(RowIdentifier.SECRET_HEADER, RowType.HEADER,
                     R.string.SuperSecretSettings));
    rows.add(new Row(RowIdentifier.SECRET_DESCRIPTION, RowType.TEXT_INFO,
                     R.string.SuperSecretSettingsPlaceholder));
    rows.add(new Row(RowIdentifier.SECRET_GHOST_MODE, RowType.TEXT_CELL,
                     R.string.GhostMode));
    rows.add(new Row(RowIdentifier.SECRET_DISABLE, RowType.TEXT_CELL,
                     R.string.SuperSecretSettingsDisable));
    if (listAdapter != null) {
      listAdapter.notifyDataSetChanged();
    }
  }

  @Override
  public View createView(Context context) {
    actionBar.setBackButtonImage(R.drawable.ic_ab_back);
    actionBar.setAllowOverlayTitle(true);
    actionBar.setTitle(getString(R.string.SuperSecretSettings));
    actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
      @Override
      public void onItemClick(int id) {
        if (id == -1) {
          finishFragment();
        }
      }
    });

    fragmentView = new FrameLayout(context);
    fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
    FrameLayout frameLayout = (FrameLayout)fragmentView;

    listAdapter = new ListAdapter(context);
    listView = new RecyclerListView(context);
    listView.setSelectorType(9);
    listView.setSelectorDrawableColor(0);
    listView.setLayoutManager(new LinearLayoutManager(context));
    int padding = FluffySettingsScaffold.getListOuterPadding();
    listView.setPadding(padding, padding, padding, padding);
    listView.setClipToPadding(false);
    listView.addItemDecoration(FluffySettingsScaffold.createCardDecoration(this::isCardRow));
    listView.setAdapter(listAdapter);
    listView.setVerticalScrollBarEnabled(false);
    listView.setOnItemClickListener((view, position) -> {
      Row row = rows.get(position);
      if (row.id == RowIdentifier.SECRET_GHOST_MODE) {
        presentFragment(new ghostModeActivitySettings());
      } else if (row.id == RowIdentifier.SECRET_DISABLE) {
        long userId = UserConfig.getInstance(currentAccount).getClientUserId();
        SecretSettingsHelper.setSecretSettingsUnlocked(userId, false);
        Toast.makeText(getParentActivity(), R.string.SuperSecretSettingsDisabled,
                       Toast.LENGTH_SHORT)
            .show();
        finishFragment();
      }
    });

    frameLayout.addView(listView,
                        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                                 LayoutHelper.MATCH_PARENT));
    return fragmentView;
  }

  @Override
  public boolean canBeginSlide() {
    return true;
  }

  private boolean isCardRow(int position) {
    if (position < 0 || position >= rows.size()) {
      return false;
    }
    RowType type = rows.get(position).type;
    return type == RowType.TEXT_INFO || type == RowType.TEXT_CELL;
  }

  private class ListAdapter extends RecyclerListView.SelectionAdapter {

    private final Context context;

    ListAdapter(Context context) { this.context = context; }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
      int position = holder.getAdapterPosition();
      if (position < 0 || position >= rows.size()) {
        return false;
      }
      return rows.get(position).type == RowType.TEXT_CELL;
    }

    @Override
    public int getItemCount() {
      return rows.size();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      Row row = rows.get(position);
      boolean isCardRow = isCardRow(position);
      boolean top = isCardRow && !isCardRow(position - 1);
      boolean bottom = isCardRow && !isCardRow(position + 1);
      FluffySettingsScaffold.applyCardRowStyle(holder.itemView, isCardRow, top, bottom, row.type == RowType.TEXT_CELL);
      switch (row.type) {
      case HEADER:
        HeaderCell headerCell = (HeaderCell)holder.itemView;
        headerCell.setText(getString(row.textResId));
        break;
      case TEXT_INFO:
      default:
        TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell)holder.itemView;
        infoCell.setText(getString(row.textResId));
        infoCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        break;
      case TEXT_CELL:
        TextCell textCell = (TextCell)holder.itemView;
        int icon = row.id == RowIdentifier.SECRET_DISABLE ? R.drawable.msg_delete : R.drawable.msg_secret;
        textCell.setTextAndIcon(getString(row.textResId), icon, position < rows.size() - 1 && rows.get(position + 1).type == RowType.TEXT_CELL);
        break;
      }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view;
      if (RowType.values()[viewType] == RowType.HEADER) {
        view = new HeaderCell(context);
      } else if (RowType.values()[viewType] == RowType.TEXT_CELL) {
        view = new TextCell(context);
      } else {
        view = new TextInfoPrivacyCell(context);
      }
      view.setLayoutParams(new RecyclerView.LayoutParams(
          RecyclerView.LayoutParams.MATCH_PARENT,
          RecyclerView.LayoutParams.WRAP_CONTENT));
      return new RecyclerListView.Holder(view);
    }

    @Override
    public int getItemViewType(int position) {
      return rows.get(position).type.ordinal();
    }
  }

  @Override
  public ArrayList<ThemeDescription> getThemeDescriptions() {
    ArrayList<ThemeDescription> descriptions = new ArrayList<>();

    descriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND,
                                          null, null, null, null,
                                          Theme.key_windowBackgroundGray));
    descriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR,
                                          new Class[] {HeaderCell.class, TextInfoPrivacyCell.class, TextCell.class},
                                          null, null, null,
                                          Theme.key_windowBackgroundWhite));
    descriptions.add(new ThemeDescription(listView, 0,
                                          new Class[] {HeaderCell.class},
                                          new String[] {"textView"}, null, null, null,
                                          Theme.key_windowBackgroundWhiteBlueHeader));
    descriptions.add(new ThemeDescription(listView, 0,
                                          new Class[] {TextInfoPrivacyCell.class},
                                          new String[] {"textView"}, null, null, null,
                                          Theme.key_windowBackgroundWhiteGrayText2));
    descriptions.add(new ThemeDescription(listView, 0,
                                          new Class[] {TextCell.class},
                                          new String[] {"textView"}, null, null, null,
                                          Theme.key_windowBackgroundWhiteBlackText));

    return descriptions;
  }
}
