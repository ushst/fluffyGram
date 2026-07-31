package org.ushastoe.fluffy.ui;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralStringComma;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.HashtagSearchController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ChatActivityContainer;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PostsSearchContainer;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.Stories.DarkThemeResourceProvider;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;
import org.ushastoe.fluffy.hooks.PostsBlacklistHook;
import org.ushastoe.fluffy.hooks.PostsFeedHook;
import org.ushastoe.fluffy.hooks.PostsSearchHook;

/**
 * Search tab that shows found public posts as a feed of whole messages, the way a channel renders
 * them, instead of the shortened one-line rows of the regular posts tab.
 *
 * Paid/limited queries are gated by the same tile the regular posts tab shows, because both tabs
 * share one daily quota of channels.searchPosts.
 */
public class PostsFeedContainer extends FrameLayout {

    private static final int SEARCH_DELAY = 400;

    private final BaseFragment fragment;
    private final int currentAccount;

    private final LinearLayout tileView;
    private final TextView tileTitleView;
    private final TextView tileTextView;
    private final ButtonWithCounterView tileButton;
    private final TextView tileUnderButtonView;
    private final TextView hiddenChannelsButton;
    private final PostsProjectsView projectsView;

    private ChatActivityContainer chatContainer;

    private String query;
    private String appliedQuery;
    private Runnable searchRunnable;

    private TLRPC.SearchPostsFlood flood;
    private int floodRequestId = -1;

    private int pagesPaddingTop, pagesPaddingBottom;
    private int keyboardHeight;

    private ColoredImageSpan searchSpan;
    private ColoredImageSpan arrowSpan;
    private PostsSearchContainer.ForegroundColorAlphaSpan colorSpan;
    private final ColoredImageSpan[] starSpan = new ColoredImageSpan[1];
    private final Runnable updateTileRunnable = this::updateTile;

    public PostsFeedContainer(Context context, BaseFragment fragment) {
        super(context);

        this.fragment = fragment;
        this.currentAccount = fragment.getCurrentAccount();

        tileView = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(
                    MeasureSpec.makeMeasureSpec(
                        Math.min(dp(220), MeasureSpec.getSize(widthMeasureSpec)),
                        MeasureSpec.getMode(widthMeasureSpec)
                    ),
                    heightMeasureSpec
                );
            }
        };
        tileView.setOrientation(LinearLayout.VERTICAL);

        tileTitleView = new TextView(context);
        tileTitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        tileTitleView.setTypeface(AndroidUtilities.bold());
        tileTitleView.setGravity(Gravity.CENTER);
        tileTitleView.setSingleLine(false);
        tileTitleView.setMaxLines(4);
        tileView.addView(tileTitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        tileTextView = new TextView(context);
        tileTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tileTextView.setGravity(Gravity.CENTER);
        tileTextView.setSingleLine(false);
        tileTextView.setMaxLines(4);
        tileView.addView(tileTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 9, 0, 0));

        tileButton = new ButtonWithCounterView(context, null).setRound();
        tileView.addView(tileButton, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, Gravity.FILL_HORIZONTAL, 0, 19, 0, 0));

        tileUnderButtonView = new TextView(context);
        tileUnderButtonView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tileUnderButtonView.setGravity(Gravity.CENTER);
        tileView.addView(tileUnderButtonView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 11, 0, 0));

        addView(tileView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 32, 0, 32, 0));

        projectsView = new PostsProjectsView(context, currentAccount, fragment.getResourceProvider());
        addView(projectsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        hiddenChannelsButton = new TextView(context);
        hiddenChannelsButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        hiddenChannelsButton.setTypeface(AndroidUtilities.bold());
        hiddenChannelsButton.setGravity(Gravity.CENTER);
        hiddenChannelsButton.setPadding(dp(12), dp(6), dp(12), dp(6));
        hiddenChannelsButton.setVisibility(View.GONE);
        hiddenChannelsButton.setOnClickListener(v -> new PostsBlacklistSheet(fragment, appliedQuery, this::onBlacklistChanged).show());
        addView(hiddenChannelsButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.RIGHT, 0, 8, 12, 0));

        updateColors();
        updateTile();

        PostsFeedHook.setSearchErrorListener(currentAccount, this::onSearchError);
    }

    public void updateColors() {
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        tileTitleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        tileTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        tileUnderButtonView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        projectsView.updateColors();
        hiddenChannelsButton.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2));
        hiddenChannelsButton.setBackground(Theme.createRoundRectDrawable(dp(14), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2), .12f)));
        colorSpan = null;
    }

    public void setPagesPaddings(int top, int bottom) {
        pagesPaddingTop = top;
        pagesPaddingBottom = bottom;

        projectsView.setPagesPaddings(top, bottom);
        applyChatMargins();
        applyTileTranslation();
    }

    /** Opens a saved search: the query goes back into the search field and runs as usual. */
    public void setOnProjectClick(Utilities.Callback<String> listener) {
        projectsView.setOnProjectClick(listener);
    }

    /**
     * The feed does not live inside a fragment of its own, so nothing shrinks it when the keyboard
     * shows up: the page keeps it above the keyboard itself.
     */
    public void setKeyboardHeight(int height) {
        if (keyboardHeight == height) {
            return;
        }
        keyboardHeight = height;
        // the page is told about the keyboard from a measure pass, so relayout waits for the next frame
        AndroidUtilities.runOnUIThread(this::applyChatMargins);
        applyTileTranslation();
    }

    private void applyTileTranslation() {
        tileView.setTranslationY((pagesPaddingTop - pagesPaddingBottom) / 2f - keyboardHeight / 2f);
    }

    /**
     * The feed is a whole chat, its own paddings are managed by ChatActivity, so the page keeps it
     * below the search tabs by margins instead of fighting over padding.
     */
    private void applyChatMargins() {
        if (chatContainer == null) {
            return;
        }
        final int bottomMargin = pagesPaddingBottom + keyboardHeight;
        final MarginLayoutParams lp = (MarginLayoutParams) chatContainer.getLayoutParams();
        if (lp.topMargin != pagesPaddingTop || lp.bottomMargin != bottomMargin) {
            lp.topMargin = pagesPaddingTop;
            lp.bottomMargin = bottomMargin;
            chatContainer.requestLayout();
        }
    }

    public void search(String q) {
        q = q == null ? "" : q.trim();
        if (TextUtils.equals(query, q)) {
            return;
        }
        query = q;

        cancelSearchRunnable();
        if (TextUtils.isEmpty(q)) {
            appliedQuery = null;
            updateTile();
            return;
        }
        AndroidUtilities.runOnUIThread(searchRunnable = this::checkFlood, SEARCH_DELAY);
    }

    private void cancelSearchRunnable() {
        if (searchRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(searchRunnable);
            searchRunnable = null;
        }
    }

    /**
     * Same gate as the regular posts tab: ask the server whether this exact query is free before
     * spending one of the daily searches on it.
     */
    private void checkFlood() {
        searchRunnable = null;
        if (TextUtils.isEmpty(query) || TextUtils.equals(query, appliedQuery)) {
            return;
        }
        if (floodRequestId >= 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(floodRequestId, true);
            floodRequestId = -1;
        }

        final String requestedQuery = query;
        final TLRPC.TL_channels_checkSearchPostsFlood req = new TLRPC.TL_channels_checkSearchPostsFlood();
        req.flags |= 1;
        req.query = requestedQuery;
        floodRequestId = ConnectionsManager.getInstance(currentAccount).sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            floodRequestId = -1;
            if (!TextUtils.equals(requestedQuery, query)) {
                return;
            }
            if (res instanceof TLRPC.SearchPostsFlood) {
                flood = (TLRPC.SearchPostsFlood) res;
                if (flood.query_is_free) {
                    startFeed(false);
                    return;
                }
            }
            // results of the previous query must not stay on screen pretending to answer this one
            appliedQuery = null;
            updateTile();
        }));
    }

    private void startFeed(boolean pay) {
        if (TextUtils.isEmpty(query)) {
            return;
        }
        if (pay && flood != null) {
            PostsFeedHook.setPendingPaidStars(currentAccount, flood.stars_amount);
        }
        appliedQuery = query;
        PostsSearchHook.saveQuery(currentAccount, appliedQuery);
        if (chatContainer == null) {
            createFeed(appliedQuery);
        } else {
            chatContainer.chatActivity.updateSearchingHashtag(appliedQuery, true);
            showAsFeed();
        }
        updateTile();
    }

    /**
     * Search mode opens its results as a compact list; this tab exists to show them as a channel-like
     * feed of whole posts, so the list overlay is collapsed right away.
     */
    private void showAsFeed() {
        if (chatContainer == null) {
            return;
        }
        chatContainer.chatActivity.showMessagesSearchListView(false);
    }

    private void createFeed(String q) {
        HashtagSearchController.getInstance(currentAccount).clearSearchResults(ChatActivity.SEARCH_PUBLIC_POSTS);

        final Bundle args = new Bundle();
        args.putInt("chatMode", ChatActivity.MODE_SEARCH);
        args.putInt("searchType", ChatActivity.SEARCH_PUBLIC_POSTS);
        args.putString("searchHashtag", q);

        chatContainer = new ChatActivityContainer(getContext(), fragment.getParentLayout(), args) {
            private boolean activityCreated;

            @Override
            protected void initChatActivity() {
                if (activityCreated) {
                    return;
                }
                activityCreated = true;
                super.initChatActivity();
                showAsFeed();
            }
        };
        final FrameLayout.LayoutParams lp = LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL);
        lp.topMargin = pagesPaddingTop;
        lp.bottomMargin = pagesPaddingBottom + keyboardHeight;
        addView(chatContainer, lp);
    }

    private void onSearchError(TLRPC.TL_error error) {
        if (error == null || TextUtils.isEmpty(appliedQuery)) {
            return;
        }
        if (error.text != null && error.text.startsWith("FLOOD_WAIT_") && error.text.contains("_OR_STARS_")) {
            final String[] parts = error.text.split("_");
            try {
                final int waitSeconds = Integer.parseInt(parts[2]);
                final long starsPrice = Long.parseLong(parts[parts.length - 1]);
                if (flood != null) {
                    flood.flags |= 2;
                    flood.wait_till = ConnectionsManager.getInstance(currentAccount).getCurrentTime() + waitSeconds;
                    flood.stars_amount = starsPrice;
                }
            } catch (Exception ignore) {
            }
        } else if ("BALANCE_TOO_LOW".equalsIgnoreCase(error.text)) {
            final long needed = flood != null ? flood.stars_amount : 0;
            StarsController.getInstance(currentAccount).getBalance(true, () -> {
                final Activity activity = AndroidUtilities.getActivity();
                final BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
                final Theme.ResourcesProvider resourcesProvider = PhotoViewer.getInstance().isVisible() || lastFragment != null && lastFragment.hasShownSheet()
                    ? new DarkThemeResourceProvider()
                    : (lastFragment != null ? lastFragment.getResourceProvider() : null);
                new StarsIntroActivity.StarsNeededSheet(activity, resourcesProvider, needed, StarsIntroActivity.StarsNeededSheet.TYPE_SEARCH, "", () -> startFeed(true), 0).show();
            }, true);
        }
        appliedQuery = null;
        updateTile();
    }

    /**
     * Hiding a channel drops its posts from the feed at once, restoring one needs the results back,
     * so the query is searched again: repeating the same query stays free for a while, and if that
     * window is gone the flood check falls back to the tile instead of spending a search silently.
     */
    private void onBlacklistChanged() {
        updateHiddenChannelsButton();
        if (TextUtils.isEmpty(query) || chatContainer == null) {
            return;
        }
        cancelSearchRunnable();
        appliedQuery = null;
        checkFlood();
    }

    private void updateHiddenChannelsButton() {
        final int count = TextUtils.isEmpty(appliedQuery) ? 0 : PostsBlacklistHook.getHiddenCount(currentAccount, appliedQuery);
        if (count <= 0) {
            hiddenChannelsButton.setVisibility(View.GONE);
            return;
        }
        hiddenChannelsButton.setVisibility(View.VISIBLE);
        hiddenChannelsButton.setText(formatString(R.string.FluffyPostsHiddenCount, count));
    }

    private void updateTile() {
        AndroidUtilities.cancelRunOnUIThread(updateTileRunnable);
        updateHiddenChannelsButton();

        final boolean feedShown = !TextUtils.isEmpty(appliedQuery);
        // with nothing being searched the tab is a board of saved searches
        final boolean projectsShown = !feedShown && TextUtils.isEmpty(query);
        if (chatContainer != null) {
            chatContainer.setVisibility(feedShown ? View.VISIBLE : View.GONE);
        }
        projectsView.setVisibility(projectsShown ? View.VISIBLE : View.GONE);
        if (projectsShown) {
            projectsView.update();
        }
        tileView.setVisibility(feedShown || projectsShown ? View.GONE : View.VISIBLE);
        if (feedShown || projectsShown) {
            return;
        }

        tileButton.setLoading(false);
        if (!UserConfig.getInstance(currentAccount).isPremium()) {
            tileTitleView.setText(getString(R.string.SearchPostsTitle));
            tileTextView.setText(getString(R.string.SearchPostsText));
            tileButton.setVisibility(View.VISIBLE);
            tileButton.setText(getString(R.string.SearchPostsButtonPremium), true);
            tileButton.setSubText(null, true);
            tileButton.setOnClickListener(v -> fragment.presentFragment(new PremiumPreviewFragment("search")));
            tileUnderButtonView.setVisibility(View.VISIBLE);
            tileUnderButtonView.setText(getString(R.string.SearchPostsPremium));
            return;
        }

        final int now = ConnectionsManager.getInstance(currentAccount).getCurrentTime();
        if (!TextUtils.isEmpty(query) && flood != null && (flood.flags & 2) != 0 && now < flood.wait_till) {
            tileTitleView.setText(getString(R.string.SearchPostsLimitReached));
            tileTextView.setText(formatPluralStringComma("SearchPostsLimitReachedText", flood.total_daily));

            final int left = flood.wait_till - now;
            final int h = left / 3600;
            final int m = (left - h * 3600) / 60;
            final int s = left - h * 3600 - m * 60;

            tileButton.setVisibility(View.VISIBLE);
            tileButton.setText(StarsIntroActivity.replaceStars(formatPluralStringComma("SearchPostsButtonPay", (int) flood.stars_amount), 1.13f, starSpan), true);
            tileButton.setSubText(formatString(R.string.SearchPostsFreeSearchUnlocksIn, (h > 0 ? h + ":" : "") + (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s)), true);
            tileButton.subText.setHacks(false, true, true);
            tileButton.setOnClickListener(v -> {
                tileButton.setLoading(true);
                startFeed(true);
            });
            tileUnderButtonView.setVisibility(View.GONE);
            AndroidUtilities.runOnUIThread(updateTileRunnable, 1000);
            return;
        }

        tileTitleView.setText(getString(R.string.SearchPostsTitle));
        tileTextView.setText(getString(R.string.SearchPostsText));
        if (TextUtils.isEmpty(query)) {
            tileButton.setVisibility(View.GONE);
        } else {
            final SpannableStringBuilder sb = new SpannableStringBuilder("s ");
            if (searchSpan == null) {
                searchSpan = new ColoredImageSpan(R.drawable.smiles_tab_search);
                searchSpan.setScale(.79f, .79f);
            }
            if (colorSpan == null) {
                colorSpan = new PostsSearchContainer.ForegroundColorAlphaSpan(
                    Theme.blendOver(
                        Theme.getColor(Theme.key_featuredStickers_addButton),
                        Theme.multAlpha(Theme.getColor(Theme.key_featuredStickers_buttonText), .75f)
                    )
                );
            }
            sb.setSpan(searchSpan, 0, 1, 0);
            sb.append(getString(R.string.SearchPostsButton));
            sb.append(" ");
            final int start = sb.length();
            sb.append(TextUtils.ellipsize(query, tileButton.getTextPaint(), dp(100), TextUtils.TruncateAt.END));
            sb.setSpan(colorSpan, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(" >");
            if (arrowSpan == null) {
                arrowSpan = new ColoredImageSpan(R.drawable.msg_mini_forumarrow);
                arrowSpan.setScale(1.05f, 1.05f);
            }
            sb.setSpan(arrowSpan, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tileButton.setVisibility(View.VISIBLE);
            tileButton.setText(sb, true);
            tileButton.text.setHacks(false, true, false);
            tileButton.setSubText(null, true);
            tileButton.setOnClickListener(v -> {
                tileButton.setLoading(true);
                startFeed(false);
            });
        }
        if (flood != null) {
            tileUnderButtonView.setVisibility(View.VISIBLE);
            tileUnderButtonView.setText(formatPluralStringComma("SearchPostsFreeSearches", flood.remains < 1 ? flood.total_daily : flood.remains));
        } else {
            tileUnderButtonView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (chatContainer != null) {
            chatContainer.onResume();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (chatContainer != null) {
            chatContainer.onPause();
        }
    }

    public void destroy() {
        cancelSearchRunnable();
        AndroidUtilities.cancelRunOnUIThread(updateTileRunnable);
        PostsFeedHook.setSearchErrorListener(currentAccount, null);
        if (floodRequestId >= 0) {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(floodRequestId, true);
            floodRequestId = -1;
        }
        if (chatContainer != null) {
            chatContainer.chatActivity.onFragmentDestroy();
            removeView(chatContainer);
            chatContainer = null;
        }
        appliedQuery = null;
        query = null;
    }
}
