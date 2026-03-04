package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public final class LoginSmsWarningPatch {
    private LoginSmsWarningPatch() {
    }

    public static void addSmsOnlyOfficialWarning(LinearLayout target, Context context) {
        TextView warningView = new TextView(context);
        warningView.setText(R.string.SmsOnlyOfficialWarning);
        warningView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14f);
        warningView.setTextColor(Theme.getColor(Theme.key_text_RedRegular));
        warningView.setGravity(Gravity.CENTER);
        warningView.setLineSpacing((float) AndroidUtilities.dp(2f), 1.0f);
        warningView.setPadding(0, AndroidUtilities.dp(8f), 0, 0);
        target.addView(
                warningView,
                LayoutHelper.createLinear(
                        LayoutHelper.MATCH_PARENT,
                        LayoutHelper.WRAP_CONTENT,
                        Gravity.CENTER_HORIZONTAL,
                        32,
                        0,
                        32,
                        0
                )
        );
    }
}
