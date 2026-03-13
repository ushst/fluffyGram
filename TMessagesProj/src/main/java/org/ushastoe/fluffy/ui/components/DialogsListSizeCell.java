package org.ushastoe.fluffy.ui.components;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;

public class DialogsListSizeCell extends FrameLayout {

    public interface Callback {
        void onScaleChanged(int scale, boolean finalChange);
    }

    private final TextView titleView;
    private final TextView valueView;
    private final SeekBarView seekBarView;
    private Callback callback;

    public DialogsListSizeCell(Context context) {
        super(context);
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        titleView = new TextView(context);
        titleView.setText(LocaleController.getString(R.string.FluffyDialogsListSize));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setSingleLine(true);
        titleView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        addView(titleView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 21, 12, 90, 0));

        valueView = new TextView(context);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        valueView.setSingleLine(true);
        valueView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        addView(valueView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.TOP, 90, 14, 21, 0));

        seekBarView = new SeekBarView(context, true, null) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return super.onTouchEvent(event);
            }
        };
        seekBarView.setReportChanges(true);
        seekBarView.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                int scale = progressToScale(progress);
                setScaleValue(scale, true);
                if (callback != null) {
                    callback.onScaleChanged(scale, stop);
                }
            }

            @Override
            public CharSequence getContentDescription() {
                return valueView.getText();
            }
        });
        addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 14, 32, 14, 0));

        syncTheme();
    }

    public void syncTheme() {
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        valueView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        seekBarView.setInnerColor(Theme.getColor(Theme.key_switchTrack));
        seekBarView.setOuterColor(Theme.getColor(Theme.key_switchTrackChecked));
    }

    public void bind(int scale, Callback callback) {
        this.callback = callback;
        syncTheme();
        setScaleValue(scale, false);
    }

    private void setScaleValue(int scale, boolean fromUser) {
        valueView.setText(scale + "%");
        if (!fromUser) {
            seekBarView.setProgress(scaleToProgress(scale));
        }
    }

    private int progressToScale(float progress) {
        int min = AppearanceSettingsPatch.DIALOGS_LIST_SCALE_MIN;
        int max = AppearanceSettingsPatch.DIALOGS_LIST_SCALE_MAX;
        int scale = min + Math.round((max - min) * progress);
        return Math.max(min, Math.min(max, scale));
    }

    private float scaleToProgress(int scale) {
        int min = AppearanceSettingsPatch.DIALOGS_LIST_SCALE_MIN;
        int max = AppearanceSettingsPatch.DIALOGS_LIST_SCALE_MAX;
        return (scale - min) / (float) (max - min);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(80), MeasureSpec.EXACTLY)
        );
    }
}
