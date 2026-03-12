package org.ushastoe.fluffy.ui.components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

import java.util.List;

public class FluffyTabsPreviewCell extends FrameLayout {

    private final HorizontalScrollView scrollView;
    private final LinearLayout container;

    public FluffyTabsPreviewCell(Context context) {
        super(context);

        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        scrollView = new HorizontalScrollView(context);
        scrollView.setHorizontalScrollBarEnabled(false);
        scrollView.setOverScrollMode(OVER_SCROLL_NEVER);
        addView(scrollView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        int horizontalPadding = AndroidUtilities.dp(10);
        int verticalPadding = AndroidUtilities.dp(8);
        container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
        scrollView.addView(container, new HorizontalScrollView.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void setItems(List<PreviewItem> items) {
        container.removeAllViews();
        if (items == null || items.isEmpty()) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            PreviewItem item = items.get(i);
            LinearLayout itemLayout = new LinearLayout(getContext());
            itemLayout.setOrientation(LinearLayout.VERTICAL);
            itemLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            itemLayout.setClickable(false);
            itemLayout.setFocusable(false);
            itemLayout.setMinimumWidth(AndroidUtilities.dp(64));
            itemLayout.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(10), AndroidUtilities.dp(8));
            itemLayout.setBackground(Theme.createRoundRectDrawable(
                    AndroidUtilities.dp(18),
                    item.selected ? Theme.getColor(Theme.key_featuredStickers_addButton) : 0
            ));

            ImageView iconView = new ImageView(getContext());
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(24), AndroidUtilities.dp(24));
            iconView.setLayoutParams(iconParams);
            if (item.avatarDrawable != null) {
                item.avatarDrawable.setBounds(0, 0, AndroidUtilities.dp(24), AndroidUtilities.dp(24));
                iconView.setImageDrawable(item.avatarDrawable);
            } else if (item.iconResId != 0) {
                Drawable drawable = getContext().getResources().getDrawable(item.iconResId).mutate();
                int tintColor = item.selected
                        ? Theme.getColor(Theme.key_featuredStickers_buttonText)
                        : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
                drawable.setColorFilter(new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_IN));
                iconView.setImageDrawable(drawable);
            }
            itemLayout.addView(iconView);

            TextView textView = new TextView(getContext());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            textParams.topMargin = AndroidUtilities.dp(4);
            textView.setLayoutParams(textParams);
            textView.setText(item.label);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            textView.setTextColor(item.selected
                    ? Theme.getColor(Theme.key_featuredStickers_buttonText)
                    : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
            itemLayout.addView(textView);

            LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                itemParams.leftMargin = AndroidUtilities.dp(8);
            }
            container.addView(itemLayout, itemParams);
        }
    }

    public static final class PreviewItem {
        public final CharSequence label;
        public final int iconResId;
        public final Drawable avatarDrawable;
        public final boolean selected;

        private PreviewItem(CharSequence label, int iconResId, Drawable avatarDrawable, boolean selected) {
            this.label = label;
            this.iconResId = iconResId;
            this.avatarDrawable = avatarDrawable;
            this.selected = selected;
        }

        public static PreviewItem withIcon(CharSequence label, int iconResId, boolean selected) {
            return new PreviewItem(label, iconResId, null, selected);
        }

        public static PreviewItem withAvatar(CharSequence label, Drawable avatarDrawable, boolean selected) {
            return new PreviewItem(label, 0, avatarDrawable, selected);
        }
    }
}
