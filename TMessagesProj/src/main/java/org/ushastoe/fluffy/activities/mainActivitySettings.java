package org.ushastoe.fluffy.activities;

import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.activities.elements.headerSettingsCell;

public class mainActivitySettings extends BaseFragment {
  private ListAdapter listAdapter;
  private RecyclerListView listView;
  private LinearLayoutManager layoutManager;

  private headerSettingsCell headerSettingsCell;
  private View actionBarBackground;
  private AnimatorSet actionBarAnimator;

  private int[] location = new int[2];
  private final List<Row> rows = new ArrayList<>();

  private enum RowType { CUSTOM_HEADER, DIVIDER, HEADER, TEXT_CELL }

  private enum RowIdentifier {
    ABOUT_FLUFFY,
    CATEGORY_DIVIDER,
    CATEGORY_HEADER,
    GENERAL,
    APPEARANCE,
    ABOUT_DIVIDER,
    LINKS_HEADER,
    CHANNEL,
    GITHUB
  }

  private static class Row {
    final RowIdentifier id;
    final RowType type;
    Integer textResId;
    Integer valueResId;
    Integer iconResId;
    boolean hasDivider;

    Row(RowIdentifier id, RowType type) {
      this.id = id;
      this.type = type;
    }

    Row(RowIdentifier id, RowType type, int textResId) {
      this(id, type);
      this.textResId = textResId;
    }

    Row(RowIdentifier id, RowType type, int textResId, int iconResId,
        boolean hasDivider) {
      this(id, type, textResId);
      this.iconResId = iconResId;
      this.hasDivider = hasDivider;
    }

    Row(RowIdentifier id, RowType type, int textResId, int valueResId,
        int iconResId, boolean hasDivider) {
      this(id, type, textResId, iconResId, hasDivider);
      this.valueResId = valueResId;
    }
  }

  @Override
  public boolean onFragmentCreate() {
    super.onFragmentCreate();
    DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
    updateRows();
    return true;
  }

  private void updateRows() {
    rows.clear();

    rows.add(new Row(RowIdentifier.ABOUT_FLUFFY, RowType.CUSTOM_HEADER));
    rows.add(new Row(RowIdentifier.CATEGORY_DIVIDER, RowType.DIVIDER));
    rows.add(new Row(RowIdentifier.CATEGORY_HEADER, RowType.HEADER,
                     R.string.Categories));
    rows.add(new Row(RowIdentifier.GENERAL, RowType.TEXT_CELL, R.string.General,
                     R.drawable.msg_media, false));
    rows.add(new Row(RowIdentifier.APPEARANCE, RowType.TEXT_CELL,
                     R.string.Appearance, R.drawable.msg_theme, true));
    rows.add(new Row(RowIdentifier.ABOUT_DIVIDER, RowType.DIVIDER));
    rows.add(
        new Row(RowIdentifier.LINKS_HEADER, RowType.HEADER, R.string.Links));
    rows.add(new Row(RowIdentifier.CHANNEL, RowType.TEXT_CELL,
                     R.string.ProfileChannel, R.string.fluffy_channel_link,
                     R.drawable.msg_channel, true));
    rows.add(new Row(RowIdentifier.GITHUB, RowType.TEXT_CELL,
                     R.string.SourceCode, R.string.fluffy_github_link,
                     R.drawable.msg_delete, false));

    if (listAdapter != null) {
      listAdapter.notifyDataSetChanged();
    }
  }

  @Override
  public void onFragmentDestroy() {
    super.onFragmentDestroy();
    CacheControlActivity.canceled = true;
  }

  @Override
  public View createView(Context context) {
    actionBar.setBackButtonImage(R.drawable.ic_ab_back);
    actionBar.setBackground(null);
    actionBar.setTitleColor(
        Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
    actionBar.setItemsColor(
        Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);
    actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_listSelector),
                                      false);
    actionBar.setCastShadows(false);
    actionBar.setAddToContainer(false);
    actionBar.setOccupyStatusBar(!AndroidUtilities.isTablet());
    actionBar.setTitle(getString(R.string.fluffy));
    actionBar.getTitleTextView().setAlpha(0.0f);
    actionBar.setActionBarMenuOnItemClick(
        new ActionBar.ActionBarMenuOnItemClick() {
          @Override
          public void onItemClick(int id) {
            if (id == -1) {
              finishFragment();
            }
          }
        });

    fragmentView = new FrameLayout(context) {
      @Override
      protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        FrameLayout.LayoutParams layoutParams =
            (FrameLayout.LayoutParams)actionBarBackground.getLayoutParams();
        layoutParams.height =
            ActionBar.getCurrentActionBarHeight() +
            (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight
                                            : 0) +
            AndroidUtilities.dp(3);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      }

      @Override
      protected void onLayout(boolean changed, int left, int top, int right,
                              int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        checkScroll(false);
      }
    };
    fragmentView.setBackgroundColor(
        Theme.getColor(Theme.key_windowBackgroundGray));
    fragmentView.setTag(Theme.key_windowBackgroundGray);
    FrameLayout frameLayout = (FrameLayout)fragmentView;

    listAdapter = new ListAdapter(context);

    listView = new RecyclerListView(context) {
      @Override
      public Integer getSelectorColor(int position) {
        return getThemedColor(Theme.key_listSelector);
      }
    };
    listView.setVerticalScrollBarEnabled(false);
    listView.setLayoutManager(
        layoutManager = new LinearLayoutManager(
            context, LinearLayoutManager.VERTICAL, false));
    listView.setAdapter(listAdapter);

    DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
    itemAnimator.setDurations(350);
    itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
    itemAnimator.setDelayAnimations(false);
    itemAnimator.setSupportsChangeAnimations(false);
    listView.setItemAnimator(itemAnimator);
    listView.setOnItemClickListener((view, position, x, y) -> {
      Row row = rows.get(position);
      if (row == null)
        return;

      switch (row.id) {
      case GITHUB:
        Browser.openUrl(getParentActivity(),
                        "https://github.com/krolchonok/Telegram");
        break;
      case CHANNEL:
        MessagesController.getInstance(currentAccount)
            .openByUserName("fluffyGram", this, 1);
        break;
      case GENERAL:
        presentFragment(new generalActivitySettings());
        break;
      case APPEARANCE:
        presentFragment(new appearanceActivitySettings());
        break;
      }
    });

    frameLayout.addView(listView,
                        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                                 LayoutHelper.MATCH_PARENT));

    listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        checkScroll(true);
      }
    });

    actionBarBackground = new View(context) {
      private final Paint paint = new Paint();

      @Override
      protected void onDraw(Canvas canvas) {
        paint.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        int h = getMeasuredHeight() - AndroidUtilities.dp(3);
        canvas.drawRect(0, 0, getMeasuredWidth(), h, paint);
        parentLayout.drawHeaderShadow(canvas, h);
      }
    };
    actionBarBackground.setAlpha(0.0f);
    frameLayout.addView(actionBarBackground,
                        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                                 LayoutHelper.WRAP_CONTENT));
    frameLayout.addView(actionBar,
                        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                                                 LayoutHelper.WRAP_CONTENT));

    return fragmentView;
  }

  private void checkScroll(boolean animated) {
    int first = layoutManager.findFirstVisibleItemPosition();
    boolean show;
    if (first != 0) {
      show = true;
    } else {
      RecyclerView.ViewHolder holder =
          listView.findViewHolderForAdapterPosition(first);
      if (holder == null) {
        show = true;
      } else {
        headerSettingsCell = (headerSettingsCell)holder.itemView;
        headerSettingsCell.getLocationOnScreen(location);
        show = location[1] + headerSettingsCell.getMeasuredHeight() <
               actionBar.getBottom();
      }
    }
    boolean visible = actionBarBackground.getTag() == null;
    if (show != visible) {
      actionBarBackground.setTag(show ? null : 1);
      if (actionBarAnimator != null) {
        actionBarAnimator.cancel();
        actionBarAnimator = null;
      }
      if (animated) {
        actionBarAnimator = new AnimatorSet();
        actionBarAnimator.playTogether(
            ObjectAnimator.ofFloat(actionBarBackground, View.ALPHA,
                                   show ? 1.0f : 0.0f),
            ObjectAnimator.ofFloat(actionBar.getTitleTextView(), View.ALPHA,
                                   show ? 1.0f : 0.0f));
        actionBarAnimator.setDuration(250);
        actionBarAnimator.addListener(new AnimatorListenerAdapter() {
          @Override
          public void onAnimationEnd(Animator animation) {
            if (animation.equals(actionBarAnimator)) {
              actionBarAnimator = null;
            }
          }
        });
        actionBarAnimator.start();
      } else {
        actionBarBackground.setAlpha(show ? 1.0f : 0.0f);
        actionBar.getTitleTextView().setAlpha(show ? 1.0f : 0.0f);
      }
    }
  }

  @Override
  protected void onDialogDismiss(Dialog dialog) {
    DownloadController.getInstance(currentAccount).checkAutodownloadSettings();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (listAdapter != null) {
      listAdapter.notifyDataSetChanged();
    }
  }

  private class ListAdapter extends RecyclerListView.SelectionAdapter {

    private final Context mContext;

    public ListAdapter(Context context) { mContext = context; }

    @Override
    public int getItemCount() {
      return rows.size();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
      Row row = rows.get(position);
      switch (row.type) {
      case CUSTOM_HEADER:
        headerSettingsCell = (headerSettingsCell)holder.itemView;
        headerSettingsCell.setPadding(
            0,
            ActionBar.getCurrentActionBarHeight() +
                (actionBar.getOccupyStatusBar()
                     ? AndroidUtilities.statusBarHeight
                     : 0) -
                AndroidUtilities.dp(40),
            0, 0);
        break;
      case HEADER:
        HeaderCell headerCell = (HeaderCell)holder.itemView;
        headerCell.setText(getString(row.textResId));
        break;
      case DIVIDER:
        holder.itemView.setBackground(
            Theme.getThemedDrawable(mContext, R.drawable.greydivider,
                                    Theme.key_windowBackgroundGrayShadow));
        break;
      case TEXT_CELL:
        TextCell textCell = (TextCell)holder.itemView;
        String value =
            (row.valueResId != null) ? getString(row.valueResId) : null;
        if (value != null) {
          textCell.setTextAndValueAndIcon(getString(row.textResId), value,
                                          row.iconResId, row.hasDivider);
        } else {
          textCell.setTextAndIcon(getString(row.textResId), row.iconResId,
                                  row.hasDivider);
        }
        break;
      }
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
      int position = holder.getAdapterPosition();
      if (position < 0 || position >= rows.size()) {
        return false;
      }
      RowType type = rows.get(position).type;
      return type == RowType.TEXT_CELL;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
      View view;
      switch (RowType.values()[viewType]) {
      case DIVIDER:
        view = new ShadowSectionCell(mContext);
        break;
      case CUSTOM_HEADER:
        view = new headerSettingsCell(mContext);
        view.setBackgroundColor(
            Theme.getColor(Theme.key_windowBackgroundWhite));
        break;
      case HEADER:
        view = new HeaderCell(mContext);
        view.setBackgroundColor(
            getThemedColor(Theme.key_windowBackgroundWhite));
        break;
      case TEXT_CELL:
      default:
        view = new TextCell(mContext);
        view.setBackgroundColor(
            Theme.getColor(Theme.key_windowBackgroundWhite));
        break;
      }
      view.setLayoutParams(new RecyclerView.LayoutParams(
          RecyclerView.LayoutParams.MATCH_PARENT,
          RecyclerView.LayoutParams.WRAP_CONTENT));
      return new RecyclerListView.Holder(view);
    }

    @Override
    public int getItemViewType(int position) {
      if (position >= 0 && position < rows.size()) {
        return rows.get(position).type.ordinal();
      }
      return RowType.TEXT_CELL.ordinal();
    }
  }

  @Override
  public ArrayList<ThemeDescription> getThemeDescriptions() {
    ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

    themeDescriptions.add(new ThemeDescription(
        listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR,
        new Class[] {TextSettingsCell.class, TextCheckCell.class,
                     HeaderCell.class, NotificationsCheckCell.class},
        null, null, null, Theme.key_windowBackgroundWhite));
    themeDescriptions.add(new ThemeDescription(
        fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null,
        Theme.key_windowBackgroundGray));

    themeDescriptions.add(
        new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null,
                             null, null, null, Theme.key_actionBarDefault));
    themeDescriptions.add(new ThemeDescription(
        listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null,
        Theme.key_actionBarDefault));
    themeDescriptions.add(new ThemeDescription(
        actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null,
        Theme.key_actionBarDefaultIcon));
    themeDescriptions.add(new ThemeDescription(
        actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null,
        Theme.key_actionBarDefaultTitle));
    themeDescriptions.add(new ThemeDescription(
        actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null,
        null, Theme.key_actionBarDefaultSelector));

    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {NotificationsCheckCell.class},
        new String[] {"textView"}, null, null, null,
        Theme.key_windowBackgroundWhiteBlackText));
    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {NotificationsCheckCell.class},
        new String[] {"valueTextView"}, null, null, null,
        Theme.key_windowBackgroundWhiteGrayText2));
    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {NotificationsCheckCell.class},
        new String[] {"checkBox"}, null, null, null, Theme.key_switchTrack));
    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {NotificationsCheckCell.class},
        new String[] {"checkBox"}, null, null, null,
        Theme.key_switchTrackChecked));

    themeDescriptions.add(
        new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null,
                             null, null, null, Theme.key_listSelector));

    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {View.class}, Theme.dividerPaint, null, null,
        Theme.key_divider));

    themeDescriptions.add(
        new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER,
                             new Class[] {ShadowSectionCell.class}, null, null,
                             null, Theme.key_windowBackgroundGrayShadow));

    themeDescriptions.add(
        new ThemeDescription(listView, 0, new Class[] {TextSettingsCell.class},
                             new String[] {"textView"}, null, null, null,
                             Theme.key_windowBackgroundWhiteBlackText));
    themeDescriptions.add(
        new ThemeDescription(listView, 0, new Class[] {TextSettingsCell.class},
                             new String[] {"valueTextView"}, null, null, null,
                             Theme.key_windowBackgroundWhiteValueText));

    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {HeaderCell.class}, new String[] {"textView"},
        null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

    themeDescriptions.add(
        new ThemeDescription(listView, 0, new Class[] {TextCheckCell.class},
                             new String[] {"textView"}, null, null, null,
                             Theme.key_windowBackgroundWhiteBlackText));
    themeDescriptions.add(
        new ThemeDescription(listView, 0, new Class[] {TextCheckCell.class},
                             new String[] {"valueTextView"}, null, null, null,
                             Theme.key_windowBackgroundWhiteGrayText2));
    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {TextCheckCell.class},
        new String[] {"checkBox"}, null, null, null, Theme.key_switchTrack));
    themeDescriptions.add(
        new ThemeDescription(listView, 0, new Class[] {TextCheckCell.class},
                             new String[] {"checkBox"}, null, null, null,
                             Theme.key_switchTrackChecked));

    themeDescriptions.add(
        new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER,
                             new Class[] {TextInfoPrivacyCell.class}, null,
                             null, null, Theme.key_windowBackgroundGrayShadow));
    themeDescriptions.add(new ThemeDescription(
        listView, 0, new Class[] {TextInfoPrivacyCell.class},
        new String[] {"textView"}, null, null, null,
        Theme.key_windowBackgroundWhiteGrayText4));

    return themeDescriptions;
  }
}