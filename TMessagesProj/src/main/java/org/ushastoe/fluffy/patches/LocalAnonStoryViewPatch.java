package org.ushastoe.fluffy.patches;

import android.os.SystemClock;
import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Stories.DialogStoriesCell;
import org.telegram.ui.Stories.DialogStoriesCell.StoryCell;

import java.util.concurrent.ConcurrentHashMap;

public final class LocalAnonStoryViewPatch {
    private static final long PENDING_TTL_MS = 15_000L;
    private static final ConcurrentHashMap<Integer, SessionState> sessionStates = new ConcurrentHashMap<>();

    private LocalAnonStoryViewPatch() {
    }

    public static void addDialogStoryOption(ItemOptions options, DialogStoriesCell dialogStoriesCell, View view, int currentAccount, long dialogId, boolean isUnread, boolean isLive) {
        if (options == null || dialogStoriesCell == null || !(view instanceof StoryCell) || !PremiumSettingsPatch.useLocalAnonymousStoryView()) {
            return;
        }
        if (!isUnread || isLive) {
            return;
        }
        options.add(
                R.drawable.msg_stories_stealth2,
                LocaleController.getString(R.string.FluffyViewAnonLocal),
                Theme.key_actionBarDefaultSubmenuItemIcon,
                Theme.key_actionBarDefaultSubmenuItem,
                () -> {
                    armNextStoryOpen(currentAccount, dialogId);
                    dialogStoriesCell.openStoryForCell((StoryCell) view);
                }
        ).makeMultiline(false);
    }

    public static void onStoryViewerOpen(int currentAccount, long dialogId) {
        SessionState sessionState = getSessionState(currentAccount);
        long now = SystemClock.elapsedRealtime();
        synchronized (sessionState) {
            if (sessionState.pendingUntilMs > 0 && now > sessionState.pendingUntilMs) {
                clearPending(sessionState);
            }
            if (sessionState.pendingDialogId == dialogId && sessionState.pendingUntilMs > 0) {
                sessionState.active = true;
                clearPending(sessionState);
            }
        }
    }

    public static void onStoryViewerClosed(int currentAccount) {
        SessionState sessionState = getSessionState(currentAccount);
        synchronized (sessionState) {
            sessionState.active = false;
            clearPending(sessionState);
        }
    }

    public static boolean shouldSendReadStoriesRequest(int currentAccount) {
        return !isActive(currentAccount);
    }

    public static boolean shouldSendIncrementStoryViewsRequest(int currentAccount) {
        return !isActive(currentAccount);
    }

    private static void armNextStoryOpen(int currentAccount, long dialogId) {
        SessionState sessionState = getSessionState(currentAccount);
        synchronized (sessionState) {
            sessionState.pendingDialogId = dialogId;
            sessionState.pendingUntilMs = SystemClock.elapsedRealtime() + PENDING_TTL_MS;
        }
    }

    private static boolean isActive(int currentAccount) {
        SessionState sessionState = getSessionState(currentAccount);
        synchronized (sessionState) {
            return sessionState.active;
        }
    }

    private static SessionState getSessionState(int currentAccount) {
        return sessionStates.computeIfAbsent(currentAccount, ignored -> new SessionState());
    }

    private static void clearPending(SessionState sessionState) {
        sessionState.pendingDialogId = 0L;
        sessionState.pendingUntilMs = 0L;
    }

    private static final class SessionState {
        private long pendingDialogId;
        private long pendingUntilMs;
        private boolean active;
    }
}
