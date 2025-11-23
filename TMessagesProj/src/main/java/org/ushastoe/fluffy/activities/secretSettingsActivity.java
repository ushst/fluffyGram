package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class secretSettingsActivity extends BaseFragment {

  private RecyclerListView listView;
  private ListAdapter listAdapter;
  private final List<Row> rows = new ArrayList<>();

  private enum RowType { HEADER, TEXT_INFO }

  private enum RowIdentifier { SECRET_HEADER, SECRET_DESCRIPTION }

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
    if (listAdapter != null) {
      listAdapter.notifyDataSetChanged();
    }
  }

  @Override
  public View createView(Context context) {
    actionBar.setBackButtonImage(R.drawable.ic_ab_back);
    actionBar.setAllowOverlayTitle(true);
    actionBar.setTitle(getString(R.string.SuperSecretSettings));

    fragmentView = new FrameLayout(context);
    fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
    FrameLayout frameLayout = (FrameLayout)fragmentView;

    listAdapter = new ListAdapter(context);
    listView = new RecyclerListView(context);
    listView.setLayoutManager(new LinearLayoutManager(context));
    listView.setAdapter(listAdapter);
    listView.setVerticalScrollBarEnabled(false);

    frameLayout.addView(listView,
                        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                                 LayoutHelper.MATCH_PARENT));
    return fragmentView;
  }

  @Override
  public boolean canBeginSlide() {
    return false;
  }

  private class ListAdapter extends RecyclerListView.SelectionAdapter {

    private final Context context;

    ListAdapter(Context context) { this.context = context; }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
      return false;
    }

    @Override
    public int getItemCount() {
      return rows.size();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      Row row = rows.get(position);
      switch (row.type) {
      case HEADER:
        HeaderCell headerCell = (HeaderCell)holder.itemView;
        headerCell.setText(row.textResId);
        break;
      case TEXT_INFO:
      default:
        TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell)holder.itemView;
        infoCell.setText(row.textResId);
        infoCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        break;
      }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View view;
      if (RowType.values()[viewType] == RowType.HEADER) {
        view = new HeaderCell(context);
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
                                          new Class[] {HeaderCell.class, TextInfoPrivacyCell.class},
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

    return descriptions;
  }
}
