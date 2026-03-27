package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.hooks.GoogleAiSettingsHook;
import org.ushastoe.fluffy.utils.GoogleAiClient;

import java.util.ArrayList;

public final class ChatEnterGoogleAiGeneratePatch {

    private ChatEnterGoogleAiGeneratePatch() {
    }

    public static void maybeAddMenuItem(Context context,
            Theme.ResourcesProvider resourcesProvider,
            ActionBarPopupWindow.ActionBarPopupWindowLayout layout,
            int itemHeightDp,
            int itemMinWidthDp,
            Object owner,
            Runnable dismissAction,
            EditTextCaption editText,
            ChatActivity fragment) {
        if (context == null || layout == null || dismissAction == null || !canShow(editText, fragment)) {
            return;
        }
        ActionBarMenuSubItem generateItem = new ActionBarMenuSubItem(context, false, true, resourcesProvider);
        generateItem.setTextAndIcon(LocaleController.getString(R.string.FluffyGoogleAiGenerateText), R.drawable.msg_bot);
        generateItem.setMinimumWidth(AndroidUtilities.dp(itemMinWidthDp));
        generateItem.setOnClickListener(v -> {
            dismissAction.run();
            showStyleChooser(fragment, editText);
        });
        layout.addView(generateItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, itemHeightDp));
    }

    private static boolean canShow(EditTextCaption editText, ChatActivity fragment) {
        return fragment != null && editText != null && GoogleAiSettingsHook.isEnabled() && GoogleAiSettingsHook.isApiKeyValidated();
    }

    private static void showStyleChooser(ChatActivity fragment, EditTextCaption editText) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        ArrayList<GoogleAiSettingsPatch.PromptPreset> presets = GoogleAiSettingsHook.getComposePromptPresets();
        ArrayList<CharSequence> labels = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < presets.size(); i++) {
            GoogleAiSettingsPatch.PromptPreset preset = presets.get(i);
            if (preset == null || TextUtils.isEmpty(preset.title) || TextUtils.isEmpty(preset.prompt)) {
                continue;
            }
            labels.add(preset.title);
            indexes.add(i);
        }
        labels.add(LocaleController.getString(R.string.FluffyGoogleAiPromptCustomInput));
        indexes.add(-1);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyGoogleAiGenerateText));
        builder.setItems(labels.toArray(new CharSequence[0]), (dialog, which) -> {
            String input = editText.getText() == null ? "" : editText.getText().toString().trim();
            int index = indexes.get(which);
            if (index == -1) {
                showCustomPromptDialog(fragment, editText, input);
                return;
            }
            GoogleAiSettingsPatch.PromptPreset preset = presets.get(index);
            if (TextUtils.isEmpty(input)) {
                showIdeaInputDialog(fragment, editText, preset.title, preset.prompt);
            } else {
                requestGeneration(fragment, editText, preset.title, preset.prompt, input);
            }
        });
        fragment.showDialog(builder.create());
    }

    private static void showCustomPromptDialog(ChatActivity fragment, EditTextCaption editText, String input) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor promptEdit = new EditTextBoldCursor(context);
        promptEdit.setBackground(null);
        promptEdit.setLineColors(Theme.getColor(Theme.key_dialogInputField),
                Theme.getColor(Theme.key_dialogInputFieldActivated),
                Theme.getColor(Theme.key_text_RedBold));
        promptEdit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        promptEdit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        promptEdit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        promptEdit.setHint(LocaleController.getString(R.string.FluffyGoogleAiPromptCustomHint));
        promptEdit.setMinLines(3);
        promptEdit.setMaxLines(8);
        promptEdit.setGravity(Gravity.LEFT | Gravity.TOP);
        promptEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        promptEdit.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        promptEdit.setCursorSize(AndroidUtilities.dp(20));
        promptEdit.setCursorWidth(1.5f);
        promptEdit.setPadding(0, AndroidUtilities.dp(4), 0, 0);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        frameLayout.addView(promptEdit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyGoogleAiGenerateText));
        builder.setView(frameLayout);
        builder.setPositiveButton(LocaleController.getString(R.string.Send), (dialog, which) -> {
            String prompt = promptEdit.getText().toString().trim();
            if (TextUtils.isEmpty(prompt)) {
                AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.FluffyGoogleAiPromptEmpty));
                return;
            }
            if (TextUtils.isEmpty(input)) {
                showIdeaInputDialog(fragment, editText, LocaleController.getString(R.string.FluffyGoogleAiPromptCustomInput), prompt);
            } else {
                requestGeneration(fragment, editText, LocaleController.getString(R.string.FluffyGoogleAiPromptCustomInput), prompt, input);
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> {
            promptEdit.requestFocus();
            AndroidUtilities.showKeyboard(promptEdit);
        }, 50));
        fragment.showDialog(dialog);
    }

    private static void showIdeaInputDialog(ChatActivity fragment, EditTextCaption editText, CharSequence title, String stylePrompt) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor ideaEdit = new EditTextBoldCursor(context);
        ideaEdit.setBackground(null);
        ideaEdit.setLineColors(Theme.getColor(Theme.key_dialogInputField),
                Theme.getColor(Theme.key_dialogInputFieldActivated),
                Theme.getColor(Theme.key_text_RedBold));
        ideaEdit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        ideaEdit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        ideaEdit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        ideaEdit.setHint(LocaleController.getString(R.string.FluffyGoogleAiGenerateIdeaHint));
        ideaEdit.setMinLines(3);
        ideaEdit.setMaxLines(8);
        ideaEdit.setGravity(Gravity.LEFT | Gravity.TOP);
        ideaEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        ideaEdit.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        ideaEdit.setCursorSize(AndroidUtilities.dp(20));
        ideaEdit.setCursorWidth(1.5f);
        ideaEdit.setPadding(0, AndroidUtilities.dp(4), 0, 0);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        frameLayout.addView(ideaEdit, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyGoogleAiGenerateText));
        builder.setView(frameLayout);
        builder.setPositiveButton(LocaleController.getString(R.string.Send), (dialog, which) -> {
            String input = ideaEdit.getText().toString().trim();
            if (TextUtils.isEmpty(input)) {
                AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.FluffyGoogleAiGenerateInputEmpty));
                return;
            }
            requestGeneration(fragment, editText, title, stylePrompt, input);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> {
            ideaEdit.requestFocus();
            AndroidUtilities.showKeyboard(ideaEdit);
        }, 50));
        fragment.showDialog(dialog);
    }

    private static void requestGeneration(ChatActivity fragment, EditTextCaption editText, CharSequence title, String stylePrompt, String input) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        String finalPrompt = stylePrompt.trim()
                + "\n\nUser idea or requirements:\n\"\"\"\n"
                + input.trim()
                + "\n\"\"\"";

        AlertDialog progressDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER, fragment.getResourceProvider());
        progressDialog.setCanCancel(true);
        progressDialog.showDelayed(120);

        GoogleAiClient.generateContent(GoogleAiSettingsHook.getApiBaseUrl(), GoogleAiSettingsHook.getApiKey(), GoogleAiSettingsHook.getModel(), finalPrompt, (responseText, errorMessage) -> {
            try {
                progressDialog.dismissUnless(500);
            } catch (Exception ignore) {
            }
            if (fragment.getParentActivity() == null) {
                return;
            }
            if (!TextUtils.isEmpty(errorMessage)) {
                new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider())
                        .setTitle(LocaleController.getString(R.string.FluffyGoogleAiRequestFailed))
                        .setMessage(errorMessage)
                        .setPositiveButton(LocaleController.getString(R.string.OK), null)
                        .show();
                return;
            }
            String text = responseText == null ? "" : responseText.trim();
            if (TextUtils.isEmpty(text)) {
                AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.FluffyGoogleAiEmptyResponse));
                return;
            }
            editText.setText(text);
            editText.setSelection(text.length());
            AndroidUtilities.showKeyboard(editText);
            BulletinFactory.of(fragment).createSimpleBulletin(R.raw.copy, LocaleController.getString(R.string.FluffyGoogleAiGeneratedApplied)).show();
        });
    }
}
