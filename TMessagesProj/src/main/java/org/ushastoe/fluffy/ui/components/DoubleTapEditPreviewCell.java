package org.ushastoe.fluffy.ui.components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Easings;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;

public class DoubleTapEditPreviewCell extends FrameLayout {

    public interface OnBubbleClickListener {
        void onBubbleClick(boolean outgoing);
    }

    private static final int[] ACTION_ICONS = new int[] {
            R.drawable.msg_block,
            R.drawable.msg_reactions,
            R.drawable.msg_reply_small,
            R.drawable.msg_copy,
            R.drawable.msg_forward,
            R.drawable.msg_edit,
            R.drawable.msg_saved,
            R.drawable.msg_delete
    };

    private final RectF bubbleRect = new RectF();
    private final Rect drawRect = new Rect();
    private final Paint bubblePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Theme.MessageDrawable[] bubbles = new Theme.MessageDrawable[] {
            new Theme.MessageDrawable(Theme.MessageDrawable.TYPE_TEXT, false, false),
            new Theme.MessageDrawable(Theme.MessageDrawable.TYPE_TEXT, true, false)
    };
    private final float[] iconProgress = new float[] {1.0f, 1.0f};
    private final int[] actions = new int[] {
            AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REACTION,
            AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REACTION
    };
    private final RectF[] bubbleBounds = new RectF[] {new RectF(), new RectF()};

    private OnBubbleClickListener onBubbleClickListener;

    public DoubleTapEditPreviewCell(Context context) {
        super(context);
        setWillNotDraw(false);
        setClickable(true);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(8), AndroidUtilities.dp(13), AndroidUtilities.dp(12));

        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(Math.max(2, AndroidUtilities.dp(1f)));
        syncTheme();
    }

    public void setOnBubbleClickListener(OnBubbleClickListener listener) {
        onBubbleClickListener = listener;
    }

    public void setActions(int incomingAction, int outgoingAction, boolean animateIncoming, boolean animateOutgoing) {
        setAction(0, incomingAction, animateIncoming);
        setAction(1, outgoingAction, animateOutgoing);
    }

    public void syncTheme() {
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        outlinePaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_switchTrack), 0x3F));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(170), MeasureSpec.EXACTLY)
        );
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int left = getPaddingLeft();
        int top = getPaddingTop();
        int width = getMeasuredWidth() - getPaddingLeft() - getPaddingRight();

        float stroke = outlinePaint.getStrokeWidth() * 0.5f;
        float bubbleTop = top + AndroidUtilities.dp(10);
        float halfWidth = width * 0.5f;

        bubbleBounds[0].set(
                left + stroke + AndroidUtilities.dp(8),
                bubbleTop + stroke,
                left + halfWidth - AndroidUtilities.dp(8) - stroke,
                bubbleTop + AndroidUtilities.dp(64) - stroke
        );
        bubbleBounds[1].set(
                left + halfWidth + AndroidUtilities.dp(8) + stroke,
                bubbleTop + AndroidUtilities.dp(76) + stroke,
                left + width - AndroidUtilities.dp(8) - stroke,
                bubbleTop + AndroidUtilities.dp(140) - stroke
        );

        drawBubble(canvas, 0);
        drawBubble(canvas, 1);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            boolean outgoing = bubbleBounds[1].contains(event.getX(), event.getY());
            if (bubbleBounds[0].contains(event.getX(), event.getY()) || outgoing) {
                performBubbleClick(outgoing);
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void performBubbleClick(boolean outgoing) {
        animateAction(outgoing ? 1 : 0);
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
        performClick();
        if (onBubbleClickListener != null) {
            onBubbleClickListener.onBubbleClick(outgoing);
        }
    }

    private void drawBubble(Canvas canvas, int index) {
        bubbleRect.set(bubbleBounds[index]);
        bubbleRect.round(drawRect);
        bubbles[index].setBounds(drawRect);
        bubblePaint.setColor(Theme.getColor(index == 1 ? Theme.key_chat_outBubble : Theme.key_chat_inBubble));
        bubbles[index].draw(canvas, bubblePaint);
        bubbles[index].draw(canvas, outlinePaint);

        Drawable icon = ContextCompat.getDrawable(getContext(), ACTION_ICONS[actions[index]]);
        if (icon == null) {
            return;
        }
        float centerX = bubbleRect.centerX();
        float centerY = bubbleRect.centerY();
        int halfSize = AndroidUtilities.dp(12);
        int extra = AndroidUtilities.dp(4 - 4 * iconProgress[index]);
        icon.setBounds((int) centerX - halfSize - extra, (int) centerY - halfSize - extra, (int) centerX + halfSize + extra, (int) centerY + halfSize + extra);
        icon.setColorFilter(new PorterDuffColorFilter(
                ColorUtils.blendARGB(0x00000000, Theme.getColor(Theme.key_chats_menuItemIcon), iconProgress[index]),
                PorterDuff.Mode.MULTIPLY
        ));
        icon.draw(canvas);
    }

    private void setAction(int index, int action, boolean animate) {
        int clampedAction = clampAction(action);
        if (actions[index] == clampedAction && !animate) {
            return;
        }
        if (!animate) {
            actions[index] = clampedAction;
            iconProgress[index] = 1.0f;
            invalidate();
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.0f);
        animator.setDuration(180L);
        animator.setInterpolator(Easings.easeInOutQuad);
        animator.addUpdateListener(animation -> {
            iconProgress[index] = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                actions[index] = clampedAction;
                ValueAnimator showAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                showAnimator.setDuration(180L);
                showAnimator.setInterpolator(Easings.easeInOutQuad);
                showAnimator.addUpdateListener(show -> {
                    iconProgress[index] = (float) show.getAnimatedValue();
                    invalidate();
                });
                showAnimator.start();
            }
        });
        animator.start();
    }

    private void animateAction(int index) {
        ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.72f, 1.0f);
        animator.setDuration(220L);
        animator.setInterpolator(Easings.easeInOutQuad);
        animator.addUpdateListener(animation -> {
            iconProgress[index] = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private int clampAction(int action) {
        if (action < AppearanceSettingsPatch.DOUBLE_TAP_ACTION_NONE || action > AppearanceSettingsPatch.DOUBLE_TAP_ACTION_DELETE) {
            return AppearanceSettingsPatch.DOUBLE_TAP_ACTION_REACTION;
        }
        return action;
    }

}
