package org.ushastoe.fluffy.activities;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.ushastoe.fluffy.fluffyConfig;

public class ghostModeActivitySettings extends BaseFragment {

  @Override
  public boolean onFragmentCreate() {
    super.onFragmentCreate();
    return true;
  }

  @Override
  public View createView(Context context) {
    fragmentView = new FrameLayout(context);
    fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

    // Content container
    LinearLayout content = new LinearLayout(context);
    content.setOrientation(LinearLayout.VERTICAL);
    content.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

    // Disable story view (don't mark stories as seen)
    final TextCheckCell storyViewCell = new TextCheckCell(context);
    storyViewCell.setTextAndCheckAndIcon(LocaleController.getString("GhostModeDisableStoryView", R.string.GhostModeDisableStoryView), fluffyConfig.disableStoryView, R.drawable.msg_stories_stealth, true);

    // Disable typing indicator
    final TextCheckCell typingCell = new TextCheckCell(context);
    typingCell.setTextAndCheckAndIcon(LocaleController.getString("GhostModeDisableTyping", R.string.GhostModeDisableTyping), fluffyConfig.disableTypingIndicator, 0, true);

    // Disable emoji indicator
    final TextCheckCell emojiCell = new TextCheckCell(context);
    emojiCell.setTextAndCheckAndIcon(LocaleController.getString("GhostModeDisableEmoji", R.string.GhostModeDisableEmoji), fluffyConfig.disableEmojiIndicator, 0, true);

    // Wire toggles to fluffyConfig helper methods and update UI
    storyViewCell.setOnClickListener(v -> {
      fluffyConfig.toggleDisableStoryView();
      storyViewCell.setChecked(fluffyConfig.disableStoryView);
    });

    typingCell.setOnClickListener(v -> {
      fluffyConfig.toggleDisableTypingIndicator();
      typingCell.setChecked(fluffyConfig.disableTypingIndicator);
    });

    emojiCell.setOnClickListener(v -> {
      fluffyConfig.toggleDisableEmojiIndicator();
      emojiCell.setChecked(fluffyConfig.disableEmojiIndicator);
    });

    // Add cells to content
    content.addView(storyViewCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    content.addView(typingCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    content.addView(emojiCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

    fragmentView.addView(content, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

    return fragmentView;
  }
}
