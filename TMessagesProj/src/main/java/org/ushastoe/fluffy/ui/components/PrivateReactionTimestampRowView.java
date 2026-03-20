package org.ushastoe.fluffy.ui.components;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.hooks.AppFontHook;

public final class PrivateReactionTimestampRowView extends FrameLayout {

    public PrivateReactionTimestampRowView(Context context, CharSequence text, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        ImageView iconView = new ImageView(context);
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.msg_reactions).mutate();
        drawable.setColorFilter(new PorterDuffColorFilter(
                Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon, resourcesProvider),
                PorterDuff.Mode.MULTIPLY));
        iconView.setImageDrawable(drawable);
        addView(iconView, LayoutHelper.createFrame(24, 24, Gravity.LEFT | Gravity.CENTER_VERTICAL, 11, 0, 0, 0));

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setText(text);
        AppFontHook.applyToTextView(textView);
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT,
                Gravity.LEFT | Gravity.CENTER_VERTICAL, 40, -1, 8, 0));

        setMinimumHeight(dp(36));
    }
}
