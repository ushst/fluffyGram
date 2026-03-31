package org.ushastoe.fluffy.patches;

import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.AboutLinkCell;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public final class AboutLinkCellPatch {

    private AboutLinkCellPatch() {
    }

    public static boolean shouldDisableMoreButtonInProfile(int position, int bioRow) {
        return position != bioRow;
    }

    public static void updateShowMoreText(AboutLinkCell cell, TextView textView, boolean expanded) {
        if (cell == null || textView == null || textView.getVisibility() != View.VISIBLE) {
            return;
        }
        CharSequence target;
        if (expanded) {
            target = LocaleController.getString(R.string.ShowLess).toLowerCase(Locale.getDefault());
        } else {
            target = LocaleController.getString(R.string.DescriptionMore);
        }
        if (!TextUtils.equals(target, textView.getText())) {
            textView.setText(target);
        }
    }

    public static void setupShowMoreControl(AboutLinkCell cell, View backgroundView, TextView textView) {
        if (cell == null || backgroundView == null || textView == null) {
            return;
        }
        Log.e("FluffyAboutMore", "setup");
        backgroundView.setContentDescription("FluffyAboutMoreButton");
        View.OnClickListener expandClickListener = v -> {
            Log.e("FluffyAboutMore", "click");
            toggleExpand(cell, textView);
        };
        backgroundView.setOnClickListener(expandClickListener);
        textView.setOnClickListener(expandClickListener);
        backgroundView.setOnTouchListener((v, event) -> {
            if (backgroundView.getVisibility() != View.VISIBLE || textView.getVisibility() != View.VISIBLE) {
                return false;
            }
            Log.e("FluffyAboutMore", "touch action=" + event.getActionMasked() + " width=" + cell.getMeasuredWidth());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    return true;
                case MotionEvent.ACTION_UP:
                    prepareExpand(cell, textView);
                    cell.updateCollapse(true, false);
                    cell.requestLayout();
                    cell.invalidate();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
                default:
                    return true;
            }
        });
        backgroundView.setClickable(true);
        backgroundView.setLongClickable(true);
        backgroundView.setOnLongClickListener(v -> true);
        textView.setClickable(true);
        textView.setLongClickable(false);
    }

    public static boolean handleShowMoreDispatchTouch(AboutLinkCell cell, MotionEvent event, View backgroundView, TextView textView) {
        if (cell == null || event == null || backgroundView == null || textView == null) {
            return false;
        }
        if (backgroundView.getVisibility() != View.VISIBLE || textView.getVisibility() != View.VISIBLE) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        boolean inside = x >= backgroundView.getLeft() && x <= backgroundView.getRight() && y >= backgroundView.getTop() && y <= backgroundView.getBottom();
        if (!inside) {
            return false;
        }
        Log.e("FluffyAboutMore", "dispatch action=" + event.getActionMasked() + " x=" + x + " y=" + y);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                toggleExpand(cell, textView);
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return true;
        }
    }

    public static boolean handleShowMoreTouch(AboutLinkCell cell, MotionEvent event, int x, int y, View backgroundView, TextView textView) {
        if (cell == null || event == null || backgroundView == null || textView == null) {
            return false;
        }
        boolean visible = backgroundView.getVisibility() == View.VISIBLE && textView.getVisibility() == View.VISIBLE;
        boolean inside = x >= backgroundView.getLeft() && x <= backgroundView.getRight() && y >= backgroundView.getTop() && y <= backgroundView.getBottom();
        Log.e("FluffyAboutMore", "root touch action=" + event.getActionMasked() + " visible=" + visible + " inside=" + inside + " x=" + x + " y=" + y + " rect=[" + backgroundView.getLeft() + "," + backgroundView.getTop() + "," + backgroundView.getRight() + "," + backgroundView.getBottom() + "]");
        if (!visible || !inside) {
            return false;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP:
                toggleExpand(cell, textView);
                return true;
            case MotionEvent.ACTION_CANCEL:
                return true;
            default:
                return true;
        }
    }

    private static void prepareExpand(AboutLinkCell cell, TextView textView) {
        try {
            Method checkTextLayout = AboutLinkCell.class.getDeclaredMethod("checkTextLayout", int.class, boolean.class);
            checkTextLayout.setAccessible(true);
            int maxWidth = Math.max(1, cell.getMeasuredWidth() - AndroidUtilities.dp(36));
            checkTextLayout.invoke(cell, maxWidth, true);

            Field shouldExpandField = AboutLinkCell.class.getDeclaredField("shouldExpand");
            shouldExpandField.setAccessible(true);
            boolean shouldExpand = shouldExpandField.getBoolean(cell);

            Log.e("FluffyAboutMore", "prepare width=" + cell.getMeasuredWidth() + " maxWidth=" + maxWidth + " shouldExpand=" + shouldExpand + " moreVisible=" + (textView.getVisibility() == View.VISIBLE));
            FileLog.d("FluffyAboutMore: width=" + cell.getMeasuredWidth() + " maxWidth=" + maxWidth + " shouldExpand=" + shouldExpand + " moreVisible=" + (textView.getVisibility() == View.VISIBLE));

            if (!shouldExpand && textView.getVisibility() == View.VISIBLE) {
                shouldExpandField.setBoolean(cell, true);
                Log.e("FluffyAboutMore", "forced shouldExpand=true fallback");
                FileLog.d("FluffyAboutMore: forced shouldExpand=true fallback");
            }
        } catch (Throwable t) {
            Log.e("FluffyAboutMore", "prepare failed", t);
            FileLog.e(t);
        }
    }

    private static void toggleExpand(AboutLinkCell cell, TextView textView) {
        if (cell == null || textView == null) {
            return;
        }
        try {
            Field expandTField = AboutLinkCell.class.getDeclaredField("expandT");
            expandTField.setAccessible(true);
            float expandT = expandTField.getFloat(cell);
            boolean expanded = expandT > 0.5f;
            if (!expanded) {
                prepareExpand(cell, textView);
            }
            cell.updateCollapse(!expanded, false);
            cell.requestLayout();
            cell.invalidate();
            Log.e("FluffyAboutMore", "toggle expanded=" + expanded + " -> " + (!expanded));
        } catch (Throwable t) {
            Log.e("FluffyAboutMore", "toggle failed", t);
            FileLog.e(t);
        }
    }
}
