package org.ushastoe.fluffy.activities;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.BooleanSupplier;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.activities.elements.FluffySettingsScaffold;
import org.ushastoe.fluffy.fluffyConfig;

public class ghostModeActivitySettings extends BaseFragment {

  @Override
  public boolean onFragmentCreate() {
    super.onFragmentCreate();
    return true;
  }

  @Override
  public View createView(Context context) {
    actionBar.setBackButtonImage(R.drawable.ic_ab_back);
    actionBar.setTitle(LocaleController.getString("GhostMode", R.string.GhostMode));

    ScrollView scrollView = new ScrollView(context);
    scrollView.setFillViewport(true);
    scrollView.setVerticalScrollBarEnabled(false);
    scrollView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
    fragmentView = scrollView;

    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setPadding(FluffySettingsScaffold.getListOuterPadding(), FluffySettingsScaffold.getListOuterPadding(), FluffySettingsScaffold.getListOuterPadding(), AndroidUtilities.dp(24));
    scrollView.addView(content, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

    content.addView(createHeroCard(context),
        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 0, 12));

    int sideInset = FluffySettingsScaffold.getCardHorizontalInset();
    FrameLayout togglesCard = new FrameLayout(context);
    togglesCard.setBackground(FluffySettingsScaffold.createCardBackground());
    togglesCard.setPadding(0, 0, 0, 0);
    content.addView(togglesCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, sideInset, 0, sideInset, 16));

    LinearLayout togglesLayout = new LinearLayout(context);
    togglesLayout.setOrientation(LinearLayout.VERTICAL);
    togglesCard.addView(togglesLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

    TextCheckCell storyViewCell =
        buildToggleCell(context, "GhostModeDisableStoryView", R.string.GhostModeDisableStoryView,
            () -> fluffyConfig.disableStoryView, fluffyConfig::toggleDisableStoryView,
            Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon), R.drawable.msg_stories_stealth, true, true, false);
    togglesLayout.addView(storyViewCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

    TextCheckCell typingCell =
        buildToggleCell(context, "GhostModeDisableTyping", R.string.GhostModeDisableTyping,
            () -> fluffyConfig.disableTypingIndicator, fluffyConfig::toggleDisableTypingIndicator,
            Theme.getColor(Theme.key_windowBackgroundWhiteGrayIcon), R.drawable.msg_edit, true, false, false);
    togglesLayout.addView(typingCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

    TextCheckCell emojiCell =
        buildToggleCell(context, "GhostModeDisableEmoji", R.string.GhostModeDisableEmoji,
            () -> fluffyConfig.disableEmojiIndicator, fluffyConfig::toggleDisableEmojiIndicator,
            Theme.getColor(Theme.key_windowBackgroundWhiteGreenText), R.drawable.msg_reactions, false, false, true);
    togglesLayout.addView(emojiCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

    content.addView(createInfoBlock(context),
        LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 0, 0, 0));

    return fragmentView;
  }

  private View createHeroCard(Context context) {
    FrameLayout card = new FrameLayout(context);
    card.setBackground(
        FluffySettingsScaffold.createCardBackground());
    card.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(20), AndroidUtilities.dp(18));

    ImageView icon = new ImageView(context);
    icon.setImageResource(R.drawable.ghost);
    icon.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlueIcon), PorterDuff.Mode.SRC_IN);
    card.addView(icon, LayoutHelper.createFrame(52, 52, Gravity.TOP | Gravity.START));

    TextView title = new TextView(context);
    title.setText(LocaleController.getString("GhostMode", R.string.GhostMode));
    title.setTypeface(AndroidUtilities.bold());
    title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
    title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
    card.addView(title,
        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 64, 0,
            0, 0));

    TextView subtitle = new TextView(context);
    subtitle.setText(LocaleController.getString("GhostModeSubtitle", R.string.GhostModeSubtitle));
    subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
    subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
    subtitle.setLineSpacing(AndroidUtilities.dp(2), 1.1f);
    card.addView(subtitle,
        LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START, 64,
            30, 0, 0));

    return card;
  }

  private TextView createInfoBlock(Context context) {
    TextView info = new TextView(context);
    info.setText(LocaleController.getString("GhostModeInfo", R.string.GhostModeInfo));
    FluffySettingsScaffold.styleInfoBlock(info);
    return info;
  }

  private TextCheckCell buildToggleCell(Context context, String textKey, int textResId, BooleanSupplier stateSupplier,
      Runnable toggleAction, int iconColor, int iconRes, boolean needDivider) {
    return buildToggleCell(context, textKey, textResId, stateSupplier, toggleAction, iconColor, iconRes, needDivider, false, false);
  }

  private TextCheckCell buildToggleCell(Context context, String textKey, int textResId, BooleanSupplier stateSupplier,
      Runnable toggleAction, int iconColor, int iconRes, boolean needDivider, boolean top, boolean bottom) {
    TextCheckCell cell = new TextCheckCell(context);
    cell.setTextAndCheck(LocaleController.getString(textKey, textResId), stateSupplier.getAsBoolean(), needDivider);
    int radius = FluffySettingsScaffold.getCardRadius();
    int topRadius = top ? radius : 0;
    int bottomRadius = bottom ? radius : 0;
    int selectorColor = Theme.getColor(Theme.key_listSelector);
    cell.setBackground(Theme.createSimpleSelectorRoundRectDrawable(topRadius, topRadius, bottomRadius, bottomRadius, 0, selectorColor, selectorColor));
    cell.setColorfullIcon(iconColor, iconRes);
    cell.setOnClickListener(v -> {
      toggleAction.run();
      cell.setChecked(stateSupplier.getAsBoolean());
    });
    return cell;
  }
}
