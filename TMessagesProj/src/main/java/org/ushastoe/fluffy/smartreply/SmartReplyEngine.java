package org.ushastoe.fluffy.smartreply;

import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.Utilities;

import java.util.ArrayList;
import java.util.Arrays;
import org.ushastoe.fluffy.hooks.AppearanceSettingsHook;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Runs providers in priority order on a background thread; the first ones to answer
 * fill the chip row, generic fallback chips cover the rest.
 */
public final class SmartReplyEngine {

    private static final int TOP_K = 3;

    /** Shown when no provider matches — keeps the bar useful for casual chat. */
    private static final String[][] FALLBACK_SETS = {
            {"Ок", "Ага", "Понял"},
            {"Ок", "Хорошо", "Ясно"},
            {"Ага", "Понял", "Ок"},
            {"Ок", "Ага", "Хорошо"},
    };

    private static volatile SmartReplyEngine instance;

    private final DispatchQueue queue = new DispatchQueue("fluffySmartReply");
    private final LocalReplyCorpus corpus = new LocalReplyCorpus();
    private final EmbeddingRetrievalProvider semantic = new EmbeddingRetrievalProvider(corpus);
    private final List<SmartReplyProvider> providers = Arrays.asList(
            new RuleIntentProvider(),
            new LocalHistoryProvider(corpus),
            semantic,
            new CuratedPairProvider()
    );

    public static SmartReplyEngine getInstance() {
        SmartReplyEngine local = instance;
        if (local == null) {
            synchronized (SmartReplyEngine.class) {
                local = instance;
                if (local == null) {
                    instance = local = new SmartReplyEngine();
                }
            }
        }
        return local;
    }

    private SmartReplyEngine() {
    }

    /** Loads the bundled DB only — safe before accounts and storage are up. */
    public void preloadBundled() {
        queue.postRunnable(() -> CuratedReplyDb.getInstance().ensureLoaded(ApplicationLoader.applicationContext));
    }

    public void preload(int account) {
        queue.postRunnable(() -> {
            for (SmartReplyProvider provider : providers) {
                if (provider.isEnabled()) {
                    prepare(provider, account);
                }
            }
        });
    }

    /** {@code onResult} is invoked on the UI thread. */
    public void request(SmartReplyContext ctx, Utilities.Callback<List<String>> onResult) {
        queue.postRunnable(() -> {
            List<String> chips = compute(ctx);
            AndroidUtilities.runOnUIThread(() -> onResult.run(chips));
        });
    }

    public static final class HistoryEntry {
        public final String incoming;
        public final String reply;
        public final int date;

        private HistoryEntry(LocalReplyCorpus.CorpusPair pair) {
            incoming = pair.context;
            reply = pair.reply;
            date = pair.date;
        }
    }

    public static final class HistoryView {
        public final List<HistoryEntry> entries;
        public final long builtAt;
        public final boolean enabled;

        private HistoryView(List<HistoryEntry> entries, long builtAt, boolean enabled) {
            this.entries = Collections.unmodifiableList(entries);
            this.builtAt = builtAt;
            this.enabled = enabled;
        }
    }

    /** Loads the corpus, collecting it on first use only when history consent is enabled. */
    public void inspectHistory(int account, Utilities.Callback<HistoryView> onResult) {
        queue.postRunnable(() -> {
            boolean enabled = AppearanceSettingsHook.isSmartReplyHistoryEnabled();
            LocalReplyCorpus.Snapshot snapshot = enabled ? corpus.peek(account) : null;
            if (enabled && snapshot == null) {
                // get() also retains small corpora that are below the suggestion threshold.
                corpus.get(account);
                snapshot = corpus.peek(account);
            }
            List<HistoryEntry> entries = new ArrayList<>();
            if (snapshot != null) {
                for (LocalReplyCorpus.CorpusPair pair : snapshot.pairs) {
                    entries.add(new HistoryEntry(pair));
                }
            }
            HistoryView view = new HistoryView(entries, snapshot == null ? 0 : snapshot.builtAt, enabled);
            AndroidUtilities.runOnUIThread(() -> onResult.run(view));
        });
    }

    /** Drops mined chat history, e.g. after the user revokes consent. */
    public void clearLocalHistory() {
        queue.postRunnable(() -> {
            corpus.clear();
            semantic.clearLocal();
        });
    }

    /** Frees the semantic model, e.g. after the user turns the feature off. */
    public void releaseSemantic() {
        queue.postRunnable(semantic::release);
    }

    private List<String> compute(SmartReplyContext ctx) {
        if (TextUtils.isEmpty(ctx.cleanedIncoming)) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (SmartReplyProvider provider : providers) {
            if (out.size() >= TOP_K) {
                break;
            }
            if (!provider.isEnabled() || !prepare(provider, ctx.account)) {
                continue;
            }
            List<String> chips;
            try {
                chips = provider.suggest(ctx, TOP_K);
            } catch (Throwable e) {
                SmartReplyLog.e(provider.id() + " suggest failed", e);
                continue;
            }
            for (String chip : chips) {
                if (TextUtils.isEmpty(chip) || SmartReplyText.isBlocked(chip)) {
                    continue;
                }
                out.putIfAbsent(SmartReplyText.normalizeReply(chip), chip.trim());
                if (out.size() >= TOP_K) {
                    break;
                }
            }
            if (!chips.isEmpty()) {
                SmartReplyLog.d(provider.id() + " → " + chips);
            }
        }
        if (out.isEmpty()) {
            SmartReplyLog.d("no provider hit for \"" + SmartReplyText.preview(ctx.cleanedIncoming) + "\" → fallback");
            String[] set = FALLBACK_SETS[Math.floorMod(ctx.cleanedIncoming.hashCode(), FALLBACK_SETS.length)];
            return Arrays.asList(set);
        }
        return new ArrayList<>(out.values());
    }

    private static boolean prepare(SmartReplyProvider provider, int account) {
        try {
            return provider.prepare(ApplicationLoader.applicationContext, account);
        } catch (Throwable e) {
            SmartReplyLog.e(provider.id() + " prepare failed", e);
            return false;
        }
    }
}
