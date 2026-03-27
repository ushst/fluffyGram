package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatEnterTemplateFillPatch {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\[([^\\[\\]\\n]+)]");

    private ChatEnterTemplateFillPatch() {
    }

    public static void maybeAddMenuItem(Context context,
            Theme.ResourcesProvider resourcesProvider,
            ActionBarPopupWindow.ActionBarPopupWindowLayout layout,
            int itemHeightDp,
            int itemMinWidthDp,
            Runnable dismissAction,
            EditTextCaption editText) {
        if (context == null || layout == null || dismissAction == null || editText == null) {
            return;
        }
        ArrayList<String> placeholders = collectPlaceholders(editText.getText());
        if (placeholders.isEmpty()) {
            return;
        }

        ActionBarMenuSubItem item = new ActionBarMenuSubItem(context, false, true, resourcesProvider);
        item.setTextAndIcon(LocaleController.getString(R.string.FluffyTemplateFillAction), R.drawable.msg_edit);
        item.setMinimumWidth(AndroidUtilities.dp(itemMinWidthDp));
        item.setOnClickListener(v -> {
            dismissAction.run();
            showFillDialog(context, resourcesProvider, editText, placeholders);
        });
        layout.addView(item, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, itemHeightDp));
    }

    private static void showFillDialog(Context context, Theme.ResourcesProvider resourcesProvider, EditTextCaption editText, ArrayList<String> placeholders) {
        ArrayList<EditTextBoldCursor> edits = new ArrayList<>();

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        scrollView.addView(container, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView info = new TextView(context);
        info.setText(LocaleController.getString(R.string.FluffyTemplateFillInfo));
        info.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        container.addView(info, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 0, 0, 12));

        for (int i = 0; i < placeholders.size(); i++) {
            String placeholder = placeholders.get(i);

            TextView label = new TextView(context);
            label.setText("[" + placeholder + "]");
            label.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            container.addView(label, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, i == 0 ? 0 : 8, 0, 4));

            EditTextBoldCursor edit = new EditTextBoldCursor(context);
            edit.setBackground(null);
            edit.setLineColors(Theme.getColor(Theme.key_dialogInputField),
                    Theme.getColor(Theme.key_dialogInputFieldActivated),
                    Theme.getColor(Theme.key_text_RedBold));
            edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
            edit.setHint(LocaleController.getString(R.string.FluffyTemplateFillValueHint));
            edit.setSingleLine(true);
            edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            edit.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            edit.setCursorSize(AndroidUtilities.dp(20));
            edit.setCursorWidth(1.5f);
            edit.setPadding(0, AndroidUtilities.dp(4), 0, 0);
            edits.add(edit);
            container.addView(edit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.FluffyTemplateFillAction));
        builder.setView(scrollView);
        builder.setPositiveButton(LocaleController.getString(R.string.Done), (dialog, which) -> applyReplacements(editText, placeholders, edits));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> {
            if (!edits.isEmpty()) {
                edits.get(0).requestFocus();
                AndroidUtilities.showKeyboard(edits.get(0));
            }
        }, 50));
        dialog.show();
    }

    private static void applyReplacements(EditTextCaption editText, ArrayList<String> placeholders, ArrayList<EditTextBoldCursor> edits) {
        CharSequence current = editText.getText();
        if (current == null) {
            return;
        }
        String updated = current.toString();
        for (int i = 0; i < placeholders.size(); i++) {
            String replacement = edits.get(i).getText().toString();
            updated = updated.replace("[" + placeholders.get(i) + "]", replacement);
        }
        editText.setText(updated);
        editText.setSelection(updated.length());
        AndroidUtilities.showKeyboard(editText);
    }

    private static ArrayList<String> collectPlaceholders(CharSequence text) {
        ArrayList<String> result = new ArrayList<>();
        if (TextUtils.isEmpty(text)) {
            return result;
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (!TextUtils.isEmpty(key)) {
                unique.add(key.trim());
            }
        }
        result.addAll(unique);
        return result;
    }
}
