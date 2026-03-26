package org.ushastoe.fluffy.patches;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.ChatAvatarContainer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

public final class ChatHeaderCenteringPatch {

    private static final Field parentFragmentField = findField("parentFragment");
    private static final Field titleTextViewField = findField("titleTextView");
    private static final Field subtitleTextViewField = findField("subtitleTextView");
    private static final Field animatedSubtitleTextViewField = findField("animatedSubtitleTextView");
    private static final Field leftPaddingField = findField("leftPadding");
    private static final Field rightAvatarPaddingField = findField("rightAvatarPadding");
    private static final Field occupyStatusBarField = findField("occupyStatusBar");
    private static final Field lastWidthField = findField("lastWidth");
    private static final Field largerWidthField = findField("largerWidth");
    private static final Field timeItemField = findField("timeItem");
    private static final Field starBgItemField = findField("starBgItem");
    private static final Field starFgItemField = findField("starFgItem");
    private static final Field titleCopyRefField = findField("titleTextLargerCopyView");
    private static final Field subtitleCopyRefField = findField("subtitleTextLargerCopyView");
    private static final Method fadeOutToLessWidthMethod = findMethod("fadeOutToLessWidth", int.class);
    private static final Method setMeasuredDimensionMethod = findViewMethod("setMeasuredDimension", int.class, int.class);

    private ChatHeaderCenteringPatch() {
    }

    public static void applyMode(ChatAvatarContainer container) {
        SimpleTextView title = getSimpleTextView(container, titleTextViewField);
        if (title == null) {
            return;
        }
        boolean centered = shouldCenter(container);
        title.setGravity(centered ? Gravity.CENTER_HORIZONTAL : Gravity.LEFT);
        title.setRightDrawableOutside(!centered);
        title.setScrollNonFitText(centered);

        SimpleTextView subtitle = getSimpleTextView(container, subtitleTextViewField);
        if (subtitle != null) {
            subtitle.setGravity(centered ? Gravity.CENTER_HORIZONTAL : Gravity.LEFT);
            subtitle.setPadding(centered ? dp(10) : 0, 0, dp(10), 0);
        }
        AnimatedTextView animatedSubtitle = getAnimatedSubtitle(container);
        if (animatedSubtitle != null) {
            animatedSubtitle.setGravity(centered ? Gravity.CENTER_HORIZONTAL : Gravity.LEFT);
            animatedSubtitle.setPadding(centered ? dp(10) : 0, 0, dp(10), 0);
        }
    }

    public static void onTitleChanged(ChatAvatarContainer container) {
        applyMode(container);
    }

    public static void applyFadeCopyMode(ChatAvatarContainer container, SimpleTextView titleCopy, SimpleTextView subtitleCopy) {
        boolean centered = shouldCenter(container);
        if (titleCopy != null) {
            titleCopy.setGravity(centered ? Gravity.CENTER_HORIZONTAL : Gravity.LEFT);
            titleCopy.setRightDrawableOutside(!centered);
        }
        if (subtitleCopy != null) {
            subtitleCopy.setGravity(centered ? Gravity.CENTER_HORIZONTAL : Gravity.LEFT);
        }
    }

    public static boolean onMeasure(ChatAvatarContainer container, int widthMeasureSpec, int heightMeasureSpec) {
        if (!shouldCenter(container)) {
            return false;
        }
        SimpleTextView title = getSimpleTextView(container, titleTextViewField);
        if (title == null) {
            return false;
        }
        View avatar = container.avatarImageView;
        SimpleTextView subtitle = getSimpleTextView(container, subtitleTextViewField);
        AnimatedTextView animatedSubtitle = getAnimatedSubtitle(container);
        ImageView timeItem = getImageView(container, timeItemField);
        ImageView starBgItem = getImageView(container, starBgItemField);
        ImageView starFgItem = getImageView(container, starFgItemField);
        int largerWidth = getInt(container, largerWidthField);

        int width = View.MeasureSpec.getSize(widthMeasureSpec + dp(40)) + title.getPaddingRight();
        int centeredTextWidth = Math.max(0, width - dp(108));

        avatar.measure(View.MeasureSpec.makeMeasureSpec(dp(36), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(36), View.MeasureSpec.EXACTLY));
        title.measure(View.MeasureSpec.makeMeasureSpec(centeredTextWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(32) + title.getPaddingRight(), View.MeasureSpec.AT_MOST));
        if (subtitle != null) {
            subtitle.measure(View.MeasureSpec.makeMeasureSpec(centeredTextWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(20), View.MeasureSpec.AT_MOST));
        } else if (animatedSubtitle != null) {
            animatedSubtitle.measure(View.MeasureSpec.makeMeasureSpec(centeredTextWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(20), View.MeasureSpec.AT_MOST));
        }
        if (timeItem != null) {
            timeItem.measure(View.MeasureSpec.makeMeasureSpec(dp(34), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(34), View.MeasureSpec.EXACTLY));
        }
        if (starBgItem != null) {
            starBgItem.measure(View.MeasureSpec.makeMeasureSpec(dp(20), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(20), View.MeasureSpec.EXACTLY));
        }
        if (starFgItem != null) {
            starFgItem.measure(View.MeasureSpec.makeMeasureSpec(dp(20), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(20), View.MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(container, width, View.MeasureSpec.getSize(heightMeasureSpec));

        int lastWidth = getInt(container, lastWidthField);
        if (lastWidth != -1 && lastWidth != width && lastWidth > width) {
            invokeFadeOutToLessWidth(container, lastWidth);
        }

        SimpleTextView titleCopy = getTitleCopy(container);
        if (titleCopy != null && largerWidth > 0) {
            int largerTextWidth = Math.max(0, largerWidth - dp(108));
            titleCopy.measure(View.MeasureSpec.makeMeasureSpec(largerTextWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(dp(24), View.MeasureSpec.AT_MOST));
        }
        setInt(container, lastWidthField, width);
        return true;
    }

    public static boolean onLayout(ChatAvatarContainer container, boolean changed, int left, int top, int right, int bottom) {
        if (!shouldCenter(container)) {
            return false;
        }
        SimpleTextView title = getSimpleTextView(container, titleTextViewField);
        if (title == null) {
            return false;
        }
        View avatar = container.avatarImageView;
        SimpleTextView subtitle = getSimpleTextView(container, subtitleTextViewField);
        AnimatedTextView animatedSubtitle = getAnimatedSubtitle(container);
        ImageView timeItem = getImageView(container, timeItemField);
        ImageView starBgItem = getImageView(container, starBgItemField);
        ImageView starFgItem = getImageView(container, starFgItemField);
        SimpleTextView titleCopy = getTitleCopy(container);
        SimpleTextView subtitleCopy = getSubtitleCopy(container);

        int leftPadding = getInt(container, leftPaddingField);
        int rightAvatarPadding = getInt(container, rightAvatarPaddingField);
        boolean occupyStatusBar = getBoolean(container, occupyStatusBarField);
        int actionBarHeight = ActionBar.getCurrentActionBarHeight();
        int viewTop = (actionBarHeight - dp(42)) / 2 + (Build.VERSION.SDK_INT >= 21 && occupyStatusBar ? AndroidUtilities.statusBarHeight : 0);
        int avatarLeft = container.getWidth() - dp(42);
        avatar.layout(avatarLeft, viewTop + 1, avatarLeft + dp(36), viewTop + dp(37));

        int contentLeft = dp(54) + rightAvatarPadding;
        int contentRight = avatarLeft - dp(12);
        int contentWidth = Math.max(0, contentRight - contentLeft);
        if (container.getSubtitleTextView().getVisibility() != View.GONE) {
            title.layout(contentLeft, viewTop + dp(1.3f) - title.getPaddingTop(), contentLeft + contentWidth, viewTop + title.getTextHeight() + dp(1.3f) - title.getPaddingTop() + title.getPaddingBottom());
            if (titleCopy != null) {
                titleCopy.layout(contentLeft, viewTop + dp(1.3f), contentLeft + contentWidth, viewTop + titleCopy.getTextHeight() + dp(1.3f));
            }
        } else {
            title.layout(contentLeft, viewTop + dp(11) - title.getPaddingTop(), contentLeft + contentWidth, viewTop + title.getTextHeight() + dp(11) - title.getPaddingTop() + title.getPaddingBottom());
            if (titleCopy != null) {
                titleCopy.layout(contentLeft, viewTop + dp(11), contentLeft + contentWidth, viewTop + titleCopy.getTextHeight() + dp(11));
            }
        }

        if (timeItem != null) {
            timeItem.layout((container.getWidth() - leftPadding) - dp(60), viewTop + dp(5), (container.getWidth() - leftPadding) - dp(26), viewTop + dp(49));
        }
        if (starBgItem != null) {
            starBgItem.layout(leftPadding + dp(28), viewTop + dp(24), leftPadding + dp(28) + starBgItem.getMeasuredWidth(), viewTop + dp(24) + starBgItem.getMeasuredHeight());
        }
        if (starFgItem != null) {
            starFgItem.layout(leftPadding + dp(28), viewTop + dp(24), leftPadding + dp(28) + starFgItem.getMeasuredWidth(), viewTop + dp(24) + starFgItem.getMeasuredHeight());
        }
        if (subtitle != null) {
            subtitle.layout(contentLeft, viewTop + dp(24), contentLeft + contentWidth, viewTop + subtitle.getTextHeight() + dp(24));
        } else if (animatedSubtitle != null) {
            animatedSubtitle.layout(contentLeft, viewTop + dp(24), contentLeft + contentWidth, viewTop + animatedSubtitle.getTextHeight() + dp(24));
        }
        if (subtitleCopy != null) {
            subtitleCopy.layout(contentLeft, viewTop + dp(24), contentLeft + contentWidth, viewTop + subtitleCopy.getTextHeight() + dp(24));
        }
        return true;
    }

    public static int getAvatarContainerRightMargin(ChatActivity chatActivity, int defaultMargin) {
        if (!shouldCenter(chatActivity)) {
            return defaultMargin;
        }
        if (UserObject.isReplyUser(chatActivity.getCurrentUser()) || chatActivity.isComments) {
            return dp(40);
        }
        return 0;
    }

    public static boolean resolveShowAudioCallAsIcon(ChatActivity chatActivity, boolean defaultValue) {
        return shouldCenter(chatActivity) ? false : defaultValue;
    }

    public static void onHeaderItemCreated(ChatActivity chatActivity) {
        if (chatActivity == null) {
            return;
        }
        ActionBarMenuItem headerItem = chatActivity.getHeaderItem();
        ChatAvatarContainer avatarContainer = chatActivity.getAvatarContainer();
        if (headerItem == null || avatarContainer == null || avatarContainer.avatarImageView == null) {
            return;
        }
        boolean centered = shouldCenter(chatActivity);
        headerItem.getIconView().setVisibility(centered ? View.INVISIBLE : View.VISIBLE);
        if (avatarContainer.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            int defaultMargin = ((ViewGroup.MarginLayoutParams) avatarContainer.getLayoutParams()).rightMargin;
            ((ViewGroup.MarginLayoutParams) avatarContainer.getLayoutParams()).rightMargin = getAvatarContainerRightMargin(chatActivity, defaultMargin);
            avatarContainer.requestLayout();
        }
        if (centered) {
            avatarContainer.avatarImageView.setOnClickListener(v -> headerItem.performClick());
        }
    }

    private static boolean shouldCenter(ChatAvatarContainer container) {
        return shouldCenter(getParentFragment(container));
    }

    private static boolean shouldCenter(ChatActivity parentFragment) {
        if (!AppearanceSettingsPatch.isCenterChatHeaderEnabled() || parentFragment == null) {
            return false;
        }
        if (parentFragment.isReplyChatComment()) {
            return false;
        }
        long dialogId = parentFragment.getDialogId();
        if (dialogId == 0 || dialogId == UserObject.REPLY_BOT) {
            return false;
        }
        return true;
    }

    private static ChatActivity getParentFragment(ChatAvatarContainer container) {
        Object value = getObject(container, parentFragmentField);
        return value instanceof ChatActivity ? (ChatActivity) value : null;
    }

    private static SimpleTextView getSimpleTextView(ChatAvatarContainer container, Field field) {
        Object value = getObject(container, field);
        return value instanceof SimpleTextView ? (SimpleTextView) value : null;
    }

    private static AnimatedTextView getAnimatedSubtitle(ChatAvatarContainer container) {
        Object value = getObject(container, animatedSubtitleTextViewField);
        return value instanceof AnimatedTextView ? (AnimatedTextView) value : null;
    }

    private static ImageView getImageView(ChatAvatarContainer container, Field field) {
        Object value = getObject(container, field);
        return value instanceof ImageView ? (ImageView) value : null;
    }

    @SuppressWarnings("unchecked")
    private static SimpleTextView getTitleCopy(ChatAvatarContainer container) {
        Object value = getObject(container, titleCopyRefField);
        if (value instanceof AtomicReference) {
            Object copy = ((AtomicReference<?>) value).get();
            return copy instanceof SimpleTextView ? (SimpleTextView) copy : null;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static SimpleTextView getSubtitleCopy(ChatAvatarContainer container) {
        Object value = getObject(container, subtitleCopyRefField);
        if (value instanceof AtomicReference) {
            Object copy = ((AtomicReference<?>) value).get();
            return copy instanceof SimpleTextView ? (SimpleTextView) copy : null;
        }
        return null;
    }

    private static Object getObject(ChatAvatarContainer container, Field field) {
        if (container == null || field == null) {
            return null;
        }
        try {
            return field.get(container);
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static int getInt(ChatAvatarContainer container, Field field) {
        if (container == null || field == null) {
            return 0;
        }
        try {
            return field.getInt(container);
        } catch (Throwable ignore) {
            return 0;
        }
    }

    private static boolean getBoolean(ChatAvatarContainer container, Field field) {
        if (container == null || field == null) {
            return false;
        }
        try {
            return field.getBoolean(container);
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static void setInt(ChatAvatarContainer container, Field field, int value) {
        if (container == null || field == null) {
            return;
        }
        try {
            field.setInt(container, value);
        } catch (Throwable ignore) {
        }
    }

    private static void invokeFadeOutToLessWidth(ChatAvatarContainer container, int largerWidth) {
        if (fadeOutToLessWidthMethod == null || container == null) {
            return;
        }
        try {
            fadeOutToLessWidthMethod.invoke(container, largerWidth);
        } catch (Throwable ignore) {
        }
    }

    private static void setMeasuredDimension(ChatAvatarContainer container, int width, int height) {
        if (setMeasuredDimensionMethod == null || container == null) {
            return;
        }
        try {
            setMeasuredDimensionMethod.invoke(container, width, height);
        } catch (Throwable ignore) {
        }
    }

    private static Field findField(String name) {
        try {
            Field field = ChatAvatarContainer.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static Method findMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = ChatAvatarContainer.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignore) {
            return null;
        }
    }

    private static Method findViewMethod(String name, Class<?>... parameterTypes) {
        try {
            Method method = View.class.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignore) {
            return null;
        }
    }
}
