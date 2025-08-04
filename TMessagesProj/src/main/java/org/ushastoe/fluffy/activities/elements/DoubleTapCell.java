/*

 This is the source code of exteraGram for Android.

 We do not and cannot prevent the use of our code,
 but be respectful and credit the original author.

 Copyright @immat0x1, 2023

*/

// Edited by @ushastoe, 2025

package org.ushastoe.fluffy.activities.elements;

import static org.telegram.messenger.LocaleController.getString;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.Easings;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.fluffyConfig;


public class DoubleTapCell extends LinearLayout {

    private final RectF rect = new RectF();
    private final Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Theme.MessageDrawable[] messages = new Theme.MessageDrawable[]{
            new Theme.MessageDrawable(Theme.MessageDrawable.TYPE_TEXT, false, false),
            new Theme.MessageDrawable(Theme.MessageDrawable.TYPE_TEXT, true, false)
    };

    private static final int[] doubleTapIcons = new int[]{
            R.drawable.msg_block,
            R.drawable.msg_reactions,
            R.drawable.msg_reply_small,
            R.drawable.msg_copy,
            R.drawable.msg_forward,
            R.drawable.msg_edit,
            R.drawable.msg_saved,
            R.drawable.msg_delete
    };

    private static final int[] ICON_WIDTH = new int[]{AndroidUtilities.dp(12), AndroidUtilities.dp(12)};

    private final ValueAnimator[] animator = new ValueAnimator[2];
    private final float[] iconChangingProgress = new float[2];
    private final int[] actionIcon = new int[2];

    private final FrameLayout preview;

    public DoubleTapCell(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        setPadding(AndroidUtilities.dp(13), 0, AndroidUtilities.dp(13), AndroidUtilities.dp(10));



        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setColor(ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_switchTrack), 0x3F));
        outlinePaint.setStrokeWidth(Math.max(2, AndroidUtilities.dp(1f)));

        preview = new FrameLayout(context) {
            @SuppressLint("DrawAllocation")
            @Override
            protected void onDraw(@NonNull Canvas canvas) {

                @SuppressLint("DrawAllocation") Rect rect1 = new Rect();

                float stroke = outlinePaint.getStrokeWidth() / 2;

                for (int i = 0; i < 2; i++) {
                    if (i == 0) {
                        rect.set(stroke + AndroidUtilities.dp(8), stroke + AndroidUtilities.dp(10), getMeasuredWidth() / 2 - AndroidUtilities.dp(8) - stroke, AndroidUtilities.dp(75) - stroke);
                    } else {
                        canvas.translate(0, AndroidUtilities.dp(80));
                        rect.set(stroke + getMeasuredWidth() / 2 + AndroidUtilities.dp(8), stroke + AndroidUtilities.dp(5), getMeasuredWidth() - AndroidUtilities.dp(8) - stroke, AndroidUtilities.dp(70) - stroke);
                    }
                    rect.round(rect1);
                    messages[i].setBounds(rect1);
                    messages[i].draw(canvas, Theme.dialogs_onlineCirclePaint);
                    messages[i].draw(canvas, outlinePaint);

                    Drawable icon = ContextCompat.getDrawable(context, actionIcon[i]);
                    if (i == 0)
                        icon.setBounds(getMeasuredWidth() / 4 - ICON_WIDTH[i], (int) (getMeasuredHeight() / 4 - ICON_WIDTH[i] + AndroidUtilities.dpf2(3f)), getMeasuredWidth() / 4 + ICON_WIDTH[i], (int) (getMeasuredHeight() / 4 + ICON_WIDTH[i] + AndroidUtilities.dpf2(3f)));
                    else
                        icon.setBounds(3 * getMeasuredWidth() / 4 - ICON_WIDTH[i], (int) (getMeasuredHeight() / 4 - ICON_WIDTH[i] - AndroidUtilities.dpf2(2f)), 3 * getMeasuredWidth() / 4 + ICON_WIDTH[i], (int) (getMeasuredHeight() / 4 + ICON_WIDTH[i] - AndroidUtilities.dpf2(2f)));

                    icon.setBounds(
                            icon.getBounds().left - AndroidUtilities.dp(4 - 4 * iconChangingProgress[i]),
                            icon.getBounds().top - AndroidUtilities.dp(4 - 4 * iconChangingProgress[i]),
                            icon.getBounds().right + AndroidUtilities.dp(4 - 4 * iconChangingProgress[i]),
                            icon.getBounds().bottom + AndroidUtilities.dp(4 - 4 * iconChangingProgress[i])
                    );
                    icon.setColorFilter(new PorterDuffColorFilter(ColorUtils.blendARGB(0x00, Theme.getColor(Theme.key_chats_menuItemIcon), iconChangingProgress[i]), PorterDuff.Mode.MULTIPLY));
                    icon.draw(canvas);
                }
            }
        };
        preview.setWillNotDraw(false);
        addView(preview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        updateIcons(0, false);
    }

    @SuppressLint("Recycle")
    public void updateIcons(int inv, boolean animate) {
        for (int i = 0; i < 2; i++) {
            if (i == 0 && inv == 2 || i == 1 && inv == 1) continue;
            if (animate) {
                int finalI = i;

                animator[i] = ValueAnimator.ofFloat(1f, 0f).setDuration(250);
                animator[i].setInterpolator(Easings.easeInOutQuad);
                animator[i].addUpdateListener(animation -> {
                    iconChangingProgress[finalI] = (Float) animation.getAnimatedValue();
                    invalidate();
                });
                animator[i].addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        actionIcon[finalI] = finalI == 0 ? doubleTapIcons[fluffyConfig.doubleTapInAction] : doubleTapIcons[fluffyConfig.doubleTapOutAction];
                        animator[finalI].setFloatValues(0f, 1f);
                        animator[finalI].removeAllListeners();
                        animator[finalI].addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                            }
                        });
                        animator[finalI].start();
                    }
                });
                animator[i].start();
            } else {
                iconChangingProgress[i] = 1f;
                actionIcon[i] = i == 0 ? doubleTapIcons[fluffyConfig.doubleTapInAction] : doubleTapIcons[fluffyConfig.doubleTapOutAction];
                invalidate();
            }
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        preview.invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(170), MeasureSpec.EXACTLY));
    }

    public static class SetReactionCell extends FrameLayout {

        private final AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable imageDrawable;

        public SetReactionCell(Context context) {
            super(context);

            setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

            TextView textView = new TextView(context);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            textView.setText(getString(R.string.DoubleTapSetting));
            addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.FILL_HORIZONTAL, 20, 0, 48, 0));

            imageDrawable = new AnimatedEmojiDrawable.SwapAnimatedEmojiDrawable(this, AndroidUtilities.dp(24));
        }

        public void update(boolean animated) {
            String reactionString = MediaDataController.getInstance(UserConfig.selectedAccount).getDoubleTapReaction();
            if (reactionString != null && reactionString.startsWith("animated_")) {
                try {
                    long documentId = Long.parseLong(reactionString.substring(9));
                    imageDrawable.set(documentId, animated);
                    return;
                } catch (Exception ignore) {
                }
            }
            TLRPC.TL_availableReaction reaction = MediaDataController.getInstance(UserConfig.selectedAccount).getReactionsMap().get(reactionString);
            if (reaction != null) {
                imageDrawable.set(reaction.static_icon, animated);
            }
        }

        public void updateImageBounds() {
            imageDrawable.setBounds(
                    getWidth() - imageDrawable.getIntrinsicWidth() - AndroidUtilities.dp(21),
                    (getHeight() - imageDrawable.getIntrinsicHeight()) / 2,
                    getWidth() - AndroidUtilities.dp(21),
                    (getHeight() + imageDrawable.getIntrinsicHeight()) / 2
            );
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            updateImageBounds();
            imageDrawable.draw(canvas);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY)
            );
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            imageDrawable.detach();
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            imageDrawable.attach();
        }

//        public static SelectAnimatedEmojiDialog.SelectAnimatedEmojiDialogWindow selectAnimatedEmojiDialog;
//
//        public static int getDialogHeight() {
//            return selectAnimatedEmojiDialog.getHeight();
//        }
    }
}
