package org.ushastoe.fluffy.activities.elements;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TypefaceSpan;
import org.ushastoe.fluffy.quickreplies.FluffyQuickReply;

/**
 * Ячейка для отображения кастомных быстрых команд в меню fluffy.
 */
public class FluffyQuickReplyCell extends FrameLayout {

    private final TextView titleView;
    private final TextView messageView;
    private boolean needDivider;
    private final Theme.ResourcesProvider resourcesProvider;
    private final AvatarDrawable avatarDrawable;
    private final ImageReceiver avatarImageReceiver;

    public FluffyQuickReplyCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        setWillNotDraw(false);

        titleView = new TextView(context);
        titleView.setSingleLine();
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL, LocaleController.isRTL ? 20 : 72, 10, LocaleController.isRTL ? 72 : 20, 0));

        messageView = new TextView(context);
        messageView.setSingleLine();
        messageView.setEllipsize(TextUtils.TruncateAt.END);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        messageView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
        addView(messageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL, LocaleController.isRTL ? 20 : 72, 34, LocaleController.isRTL ? 72 : 20, 0));

        avatarDrawable = new AvatarDrawable(resourcesProvider);
        avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_REPLIES);
        avatarImageReceiver = new ImageReceiver(this);
        avatarImageReceiver.setRoundRadius(AndroidUtilities.dp(36));
    }

    public void set(FluffyQuickReply reply, char prefix, String highlight, boolean divider) {
        if (reply == null) {
            titleView.setText(null);
            messageView.setText(null);
            avatarImageReceiver.setImageBitmap((Drawable) null);
            needDivider = divider;
            invalidate();
            return;
        }
        String commandText = prefix + reply.command;
        SpannableStringBuilder builder = new SpannableStringBuilder(commandText);
        builder.setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider)), 0, builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (!TextUtils.isEmpty(highlight)) {
            String normalized = highlight;
            if (normalized.charAt(0) != prefix) {
                normalized = prefix + normalized;
            }
            int end = Math.min(normalized.length(), builder.length());
            if (end > 0) {
                builder.setSpan(new ForegroundColorSpan(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2, resourcesProvider)), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        titleView.setText(builder);

        CharSequence message = reply.message == null ? "" : reply.message;
        message = Emoji.replaceEmoji(message, messageView.getPaint().getFontMetricsInt(), false);
        messageView.setText(message);

        updateAvatar(reply);
        needDivider = divider;
        invalidate();
    }

    private void updateAvatar(FluffyQuickReply reply) {
        if (reply == null) {
            avatarImageReceiver.setImageBitmap((Drawable) null);
            return;
        }
        String nameSource = !TextUtils.isEmpty(reply.command) ? reply.command : reply.message;
        if (TextUtils.isEmpty(nameSource)) {
            nameSource = String.valueOf(reply.prefix);
        }
        long colorSeed = reply.id != 0 ? reply.id : nameSource.hashCode();
        avatarDrawable.setInfo(colorSeed, nameSource, null);
        avatarImageReceiver.setImageBitmap(avatarDrawable);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        avatarImageReceiver.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        avatarImageReceiver.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int avatarLeft = LocaleController.isRTL ? getMeasuredWidth() - AndroidUtilities.dp(20 + 36) : AndroidUtilities.dp(20);
        avatarImageReceiver.setImageCoords(avatarLeft, AndroidUtilities.dp(12), AndroidUtilities.dp(36), AndroidUtilities.dp(36));
        avatarImageReceiver.draw(canvas);
        if (needDivider) {
            Paint paint = Theme.getThemePaint(Theme.key_paint_divider, resourcesProvider);
            if (paint == null) {
                paint = Theme.dividerPaint;
            }
            canvas.drawRect(
                    AndroidUtilities.dp(LocaleController.isRTL ? 0 : 72),
                    getMeasuredHeight() - 1,
                    getMeasuredWidth() - AndroidUtilities.dp(LocaleController.isRTL ? 72 : 0),
                    getMeasuredHeight(),
                    paint
            );
        }
    }
}
