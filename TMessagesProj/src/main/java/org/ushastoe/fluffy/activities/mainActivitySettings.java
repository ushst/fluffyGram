package org.ushastoe.fluffy.activities;

import android.app.Dialog;
import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.messenger.UserConfig;
import org.ushastoe.fluffy.activities.elements.BaseFluffySettingsActivity;
import org.ushastoe.fluffy.activities.elements.headerSettingsCell;
import org.ushastoe.fluffy.activities.elements.FluffyDialogUtils;
import org.ushastoe.fluffy.activities.elements.FluffySettingsScaffold;
import org.ushastoe.fluffy.helpers.SecretSettingsHelper;
import org.ushastoe.fluffy.activities.secretSettingsActivity;

public class mainActivitySettings extends BaseFluffySettingsActivity {
  private LinearLayout primaryMenuContainer;

  private int secretTapsCount;
  private final Runnable resetSecretTapRunnable = () -> secretTapsCount = 0;

  @Override
  public boolean onFragmentCreate() {
    super.onFragmentCreate();
    DownloadController.getInstance(currentAccount).loadAutoDownloadConfig(true);
    return true;
  }

  @Override
  public void onFragmentDestroy() {
    super.onFragmentDestroy();
    CacheControlActivity.canceled = true;
  }

  @Override
  public View createView(Context context) {
    actionBar.setBackButtonImage(R.drawable.ic_ab_back);
    actionBar.setAllowOverlayTitle(true);
    actionBar.setUseContainerForTitles();
    actionBar.setTitle(l10n("fluffy", R.string.fluffy));
    actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
      @Override
      public void onItemClick(int id) {
        if (id == -1) {
          finishFragment();
        }
      }
    });

    ScrollView scrollView = createSettingsScrollView(context);
    fragmentView = scrollView;

    LinearLayout content = createSettingsContent(context, scrollView);

    content.addView(createHeroCard(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

    content.addView(createSectionTitle(context, l10n("Categories", R.string.Categories)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
    int sideInset = getSettingsSideInset();
    FrameLayout primaryCard = createCardContainer(context);
    primaryMenuContainer = new LinearLayout(context);
    primaryMenuContainer.setOrientation(LinearLayout.VERTICAL);
    primaryCard.addView(primaryMenuContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    content.addView(primaryCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, sideInset, 0, sideInset, 24));

    populatePrimaryItems(context);

    content.addView(createSectionTitle(context, l10n("Links", R.string.Links)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));
    FrameLayout linksCard = createCardContainer(context);
    LinearLayout linksContainer = new LinearLayout(context);
    linksContainer.setOrientation(LinearLayout.VERTICAL);
    linksCard.addView(linksContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    addLinkRows(context, linksContainer);
    content.addView(linksCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, sideInset, 0, sideInset, 20));

    TextView info = new TextView(context);
    info.setText(l10n("FluffySettingsInfo", R.string.FluffySettingsInfo));
    FluffySettingsScaffold.styleInfoBlock(info);
    content.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

    return fragmentView;
  }

  @Override
  protected void onDialogDismiss(Dialog dialog) {
    DownloadController.getInstance(currentAccount).checkAutodownloadSettings();
  }

  @Override
  public void onResume() {
    super.onResume();
    if (primaryMenuContainer != null && getContext() != null) {
      populatePrimaryItems(getContext());
    }
  }

  private void onSecretTap() {
    if (SecretSettingsHelper.isSecretSettingsUnlocked(getCurrentUserId())) {
      if (getParentActivity() != null) {
        Toast.makeText(getParentActivity(), R.string.SuperSecretSettingsAlreadyUnlocked,
                       Toast.LENGTH_SHORT)
            .show();
      }
      return;
    }
    if (secretTapsCount == 0) {
      AndroidUtilities.cancelRunOnUIThread(resetSecretTapRunnable);
      AndroidUtilities.runOnUIThread(resetSecretTapRunnable, 1500);
    }
    secretTapsCount++;
    if (secretTapsCount >= 7) {
      secretTapsCount = 0;
      AndroidUtilities.cancelRunOnUIThread(resetSecretTapRunnable);
      showSecretDialog();
    }
  }

  private void showSecretDialog() {
    if (getParentActivity() == null) {
      return;
    }
    EditText editText = new EditText(getParentActivity());
    editText.setHint(R.string.SuperSecretSettingsCodeHint);
    editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    FluffyDialogUtils.styleTextInput(editText, resourceProvider);
    AlertDialog.Builder builder =
        FluffyDialogUtils.themedBuilder(getParentActivity());
    builder.setTitle(LocaleController.getString(R.string.SuperSecretSettingsTitle));
    builder.setMessage(LocaleController.getString(R.string.SuperSecretSettingsDescription));
    builder.setView(FluffyDialogUtils.wrapWithStandardPadding(editText));
    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
    builder.setPositiveButton(LocaleController.getString("Done", R.string.Done),
                              (dialog, which) -> handleSecretCode(editText.getText().toString()));
    AlertDialog dialog = builder.create();
    FluffyDialogUtils.applyWindowStyling(dialog);
    showDialog(dialog);
  }

  private void handleSecretCode(String code) {
    long userId = getCurrentUserId();
    if (userId == 0) {
      return;
    }
    String trimmed = code == null ? "" : code.trim();
    if (trimmed.isEmpty()) {
      Toast.makeText(getParentActivity(), R.string.SuperSecretSettingsCodeError,
                     Toast.LENGTH_SHORT)
          .show();
      return;
    }
    if (SecretSettingsHelper.verifyUnlockCode(userId, trimmed)) {
      SecretSettingsHelper.setSecretSettingsUnlocked(userId, true);
      if (getContext() != null) {
        populatePrimaryItems(getContext());
      }
      Toast.makeText(getParentActivity(), R.string.SuperSecretSettingsSuccess,
                     Toast.LENGTH_SHORT)
          .show();
    } else {
      Toast.makeText(getParentActivity(), R.string.SuperSecretSettingsInvalid,
                     Toast.LENGTH_SHORT)
          .show();
    }
  }

  private long getCurrentUserId() {
    return UserConfig.getInstance(currentAccount).getClientUserId();
  }

  private static class MenuEntry {
    final String key;
    final int resId;
    final int iconResId;
    final Runnable action;

    MenuEntry(String key, int resId, int iconResId, Runnable action) {
      this.key = key;
      this.resId = resId;
      this.iconResId = iconResId;
      this.action = action;
    }
  }
  private View createHeroCard(Context context) {
    FrameLayout card = new FrameLayout(context);
    card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(26), Theme.getColor(Theme.key_windowBackgroundWhite)));
    card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));
    headerSettingsCell hero = new headerSettingsCell(context);
    hero.setOnLogoClickListener(v -> onSecretTap());
    hero.setBackgroundColor(0x00000000);
    card.addView(hero, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
        Gravity.TOP | Gravity.FILL_HORIZONTAL,
        AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4)));
    return card;
  }

  private TextView createSectionTitle(Context context, CharSequence text) {
    return createSettingsSectionTitle(context, text);
  }

  private FrameLayout createCardContainer(Context context) {
    FrameLayout card = new FrameLayout(context);
    FluffySettingsScaffold.styleCardContainer(card);
    return card;
  }

  private void populatePrimaryItems(Context context) {
    if (primaryMenuContainer == null) {
      return;
    }
    primaryMenuContainer.removeAllViews();
    ArrayList<MenuEntry> entries = new ArrayList<>();
    entries.add(new MenuEntry("General", R.string.General, R.drawable.msg_media,
        () -> presentFragment(new generalActivitySettings())));
    if (SecretSettingsHelper.isSecretSettingsUnlocked(getCurrentUserId())) {
      entries.add(new MenuEntry("SuperSecretSettings", R.string.SuperSecretSettings, R.drawable.msg_secret,
          () -> presentFragment(new secretSettingsActivity())));
    }
    entries.add(new MenuEntry("Appearance", R.string.Appearance, R.drawable.msg_theme,
        () -> presentFragment(new appearanceActivitySettings())));

    for (int i = 0; i < entries.size(); i++) {
      MenuEntry entry = entries.get(i);
      TextCell cell = new TextCell(context);
      int radius = FluffySettingsScaffold.getCardRadius();
      int topRadius = i == 0 ? radius : 0;
      int bottomRadius = i == entries.size() - 1 ? radius : 0;
      int selectorColor = Theme.getColor(Theme.key_listSelector);
      cell.setBackground(Theme.createSimpleSelectorRoundRectDrawable(topRadius, topRadius, bottomRadius, bottomRadius, 0, selectorColor, selectorColor));
      cell.setTextAndIcon(l10n(entry.key, entry.resId), entry.iconResId, i != entries.size() - 1);
      cell.setOnClickListener(v -> entry.action.run());
      primaryMenuContainer.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }
  }

  private void addLinkRows(Context context, LinearLayout container) {
    TextCell channelCell = new TextCell(context);
    int radius = FluffySettingsScaffold.getCardRadius();
    int selectorColor = Theme.getColor(Theme.key_listSelector);
    channelCell.setBackground(Theme.createSimpleSelectorRoundRectDrawable(radius, radius, 0, 0, 0, selectorColor, selectorColor));
    channelCell.setTextAndValueAndIcon(l10n("ProfileChannel", R.string.ProfileChannel), l10n("fluffy_channel_link", R.string.fluffy_channel_link), R.drawable.msg_channel, true);
    channelCell.setOnClickListener(v -> MessagesController.getInstance(currentAccount).openByUserName("fluffyGram", this, 1));
    container.addView(channelCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

    TextCell githubCell = new TextCell(context);
    githubCell.setBackground(Theme.createSimpleSelectorRoundRectDrawable(0, 0, radius, radius, 0, selectorColor, selectorColor));
    githubCell.setTextAndValueAndIcon(l10n("SourceCode", R.string.SourceCode), l10n("fluffy_github_link", R.string.fluffy_github_link), R.drawable.msg_delete, false);
    githubCell.setOnClickListener(v -> Browser.openUrl(getParentActivity(), "https://github.com/krolchonok/Telegram"));
    container.addView(githubCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
  }

  private String l10n(String key, int resId) {
    return LocaleController.getString(key, resId);
  }

  @Override
  public ArrayList<ThemeDescription> getThemeDescriptions() {
    ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

    themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
    themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
    return themeDescriptions;
  }
}
