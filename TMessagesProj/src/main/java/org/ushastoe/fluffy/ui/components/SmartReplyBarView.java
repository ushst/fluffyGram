package org.ushastoe.fluffy.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick-reply row that lives inside the input island topView —
 * same upward rectangular expansion as the reply panel.
 */
public class SmartReplyBarView extends FrameLayout {

    /** Height of the chips row inside the island. */
    public static final int ROW_HEIGHT_DP = 40;
    /** Stock Telegram reply/edit top panel height. */
    public static final int REPLY_PANEL_BASE_DP = 48;

    public interface Listener {
        void onChipClick(@NonNull String text);
    }

    public interface VisibilityListener {
        void onVisibilityChanged(boolean visible);
    }

    private final LinearLayout container;
    private final Theme.ResourcesProvider resourcesProvider;
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Listener listener;
    private VisibilityListener visibilityListener;
    private final List<String> current = new ArrayList<>();
    private boolean occupyLayout;

    public SmartReplyBarView(@NonNull Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;

        setWillNotDraw(false);
        setClipChildren(true);
        setClipToPadding(true);
        setVisibility(GONE);
        // No own background — the input island glass already paints the expanded rectangle.
        setBackground(null);

        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        dividerPaint.setStrokeWidth(AndroidUtilities.dp(1));
        dividerPaint.setColor(Theme.multAlpha(
                Theme.getColor(Theme.key_chat_replyPanelName, resourcesProvider), 0.22f));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int h = MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(ROW_HEIGHT_DP), MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, h);
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);
        int count = container.getChildCount();
        if (count <= 1) {
            return;
        }
        int dividerH = AndroidUtilities.dp(18);
        float top = (getHeight() - dividerH) / 2f;
        float bottom = top + dividerH;
        for (int i = 1; i < count; i++) {
            android.view.View child = container.getChildAt(i);
            float x = container.getLeft() + child.getLeft();
            canvas.drawLine(x, top, x, bottom, dividerPaint);
        }
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setVisibilityListener(VisibilityListener listener) {
        visibilityListener = listener;
    }

    public boolean isVisibleForLayout() {
        return occupyLayout;
    }

    public void setSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            if (!occupyLayout && current.isEmpty()) {
                return;
            }
            animateHide();
            return;
        }
        if (sameSuggestions(suggestions)) {
            if (!occupyLayout || getVisibility() != VISIBLE) {
                animateShow();
            }
            return;
        }
        current.clear();
        container.removeAllViews();
        for (int i = 0; i < suggestions.size(); i++) {
            String text = suggestions.get(i);
            if (TextUtils.isEmpty(text)) {
                continue;
            }
            current.add(text);
            container.addView(createChip(text), LayoutHelper.createLinear(
                    0, LayoutHelper.MATCH_PARENT, 1f, Gravity.CENTER_VERTICAL));
        }
        if (current.isEmpty()) {
            animateHide();
            return;
        }
        animateShow();
    }

    public boolean hasSuggestions() {
        return !current.isEmpty();
    }

    public int getOccupiedHeightPx() {
        if (!occupyLayout) {
            return 0;
        }
        return AndroidUtilities.dp(ROW_HEIGHT_DP);
    }

    private TextView createChip(String text) {
        TextView chip = new TextView(getContext());
        chip.setText(text);
        chip.setSingleLine(true);
        chip.setEllipsize(TextUtils.TruncateAt.END);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        chip.setTypeface(Typeface.DEFAULT);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10), 0);
        // Same accent family as "В ответ …" title.
        chip.setTextColor(Theme.getColor(Theme.key_chat_replyPanelName, resourcesProvider));
        chip.setBackground(null);
        ScaleStateListAnimator.apply(chip, 0.03f, 1.2f);
        chip.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChipClick(text);
            }
        });
        return chip;
    }

    private void animateShow() {
        boolean wasOccupying = occupyLayout;
        occupyLayout = true;
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
            setAlpha(0f);
        }
        if (!wasOccupying) {
            notifyVisibility(true);
        }
        animate().cancel();
        animate().alpha(1f).setDuration(140).start();
    }

    private void animateHide() {
        boolean wasOccupying = occupyLayout;
        occupyLayout = false;
        if (wasOccupying) {
            notifyVisibility(false);
        }
        if (getVisibility() != VISIBLE) {
            current.clear();
            container.removeAllViews();
            return;
        }
        animate().cancel();
        animate().alpha(0f).setDuration(100).withEndAction(() -> {
            setVisibility(GONE);
            current.clear();
            container.removeAllViews();
        }).start();
    }

    private void notifyVisibility(boolean visible) {
        if (visibilityListener != null) {
            visibilityListener.onVisibilityChanged(visible);
        }
    }

    public void clear() {
        setSuggestions(null);
    }

    private boolean sameSuggestions(List<String> suggestions) {
        if (suggestions.size() != current.size()) {
            return false;
        }
        for (int i = 0; i < suggestions.size(); i++) {
            if (!TextUtils.equals(suggestions.get(i), current.get(i))) {
                return false;
            }
        }
        return true;
    }
}
