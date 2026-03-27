package org.ushastoe.fluffy.patches;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.ushastoe.fluffy.hooks.GoogleAiSettingsHook;
import org.ushastoe.fluffy.hooks.MessageActionsHook;
import org.ushastoe.fluffy.ui.FluffyGoogleAiActivity;
import org.ushastoe.fluffy.utils.GoogleAiClient;
import org.ushastoe.fluffy.utils.GoogleAiMarkdownFormatter;

import java.util.ArrayList;

public final class GoogleAiMessageMenuPatch {

    public static final int OPTION_GOOGLE_AI = 9997;
    private static final int ACTION_CUSTOM_INPUT = -1;

    private GoogleAiMessageMenuPatch() {
    }

    public static boolean canUseForText(CharSequence text) {
        return MessageActionsHook.isMessageGoogleAiEnabled()
                && GoogleAiSettingsHook.isEnabled()
                && GoogleAiSettingsHook.isApiKeyValidated()
                && !TextUtils.isEmpty(text);
    }

    public static void appendOption(ArrayList<CharSequence> items, ArrayList<Integer> options,
            ArrayList<Integer> icons, MessageObject selectedMessage) {
        if (items == null || options == null || icons == null) {
            return;
        }
        if (selectedMessage == null || selectedMessage.messageOwner == null || !canUseForText(extractMessageText(selectedMessage))) {
            return;
        }
        items.add(LocaleController.getString(R.string.FluffyGoogleAi));
        options.add(OPTION_GOOGLE_AI);
        icons.add(R.drawable.msg_bot);
    }

    public static boolean handleSelectedOption(ChatActivity fragment, MessageObject selectedMessage, int option) {
        if (option != OPTION_GOOGLE_AI || fragment == null || selectedMessage == null) {
            return false;
        }
        if (!GoogleAiSettingsHook.isEnabled()) {
            return true;
        }
        showPromptChooserForText(fragment, extractMessageText(selectedMessage), null);
        return true;
    }

    public static void showPromptChooserForText(ChatActivity fragment, CharSequence sourceText, CharSequence chooserTitle) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        String messageText = sourceText != null ? sourceText.toString().trim() : null;
        if (TextUtils.isEmpty(messageText)) {
            AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.FluffyGoogleAiMessageHasNoText));
            return;
        }

        ArrayList<GoogleAiSettingsPatch.PromptPreset> presets = GoogleAiSettingsHook.getPromptPresets();
        ArrayList<CharSequence> labels = new ArrayList<>();
        ArrayList<Integer> promptIndexes = new ArrayList<>();
        for (int i = 0; i < presets.size(); i++) {
            GoogleAiSettingsPatch.PromptPreset preset = presets.get(i);
            if (preset == null || TextUtils.isEmpty(preset.title) || TextUtils.isEmpty(preset.prompt)) {
                continue;
            }
            labels.add(preset.title);
            promptIndexes.add(i);
        }
        labels.add(LocaleController.getString(R.string.FluffyGoogleAiPromptCustomInput));
        promptIndexes.add(ACTION_CUSTOM_INPUT);

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(TextUtils.isEmpty(chooserTitle) ? LocaleController.getString(R.string.FluffyGoogleAi) : chooserTitle);
        builder.setItems(labels.toArray(new CharSequence[0]), (dialog, which) -> {
            int promptIndex = promptIndexes.get(which);
            if (promptIndex == ACTION_CUSTOM_INPUT) {
                showCustomPromptDialog(fragment, messageText);
                return;
            }
            GoogleAiSettingsPatch.PromptPreset preset = presets.get(promptIndex);
            requestGoogleAi(fragment, preset.title, preset.prompt, messageText);
        });
        fragment.showDialog(builder.create());
    }

    private static void showCustomPromptDialog(ChatActivity fragment, String messageText) {
        Context context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setBackground(null);
        editText.setLineColors(Theme.getColor(Theme.key_dialogInputField),
                Theme.getColor(Theme.key_dialogInputFieldActivated),
                Theme.getColor(Theme.key_text_RedBold));
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setHint(LocaleController.getString(R.string.FluffyGoogleAiPromptCustomHint));
        editText.setMinLines(3);
        editText.setMaxLines(8);
        editText.setGravity(Gravity.LEFT | Gravity.TOP);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        editText.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        editText.setCursorSize(AndroidUtilities.dp(20));
        editText.setCursorWidth(1.5f);
        editText.setPadding(0, AndroidUtilities.dp(4), 0, 0);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(16), AndroidUtilities.dp(24), AndroidUtilities.dp(12));
        frameLayout.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyGoogleAiPromptCustomInput));
        builder.setView(frameLayout);
        builder.setPositiveButton(LocaleController.getString(R.string.Send), (dialog, which) -> {
            String prompt = editText.getText().toString().trim();
            if (TextUtils.isEmpty(prompt)) {
                AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.FluffyGoogleAiPromptEmpty));
                return;
            }
            requestGoogleAi(fragment, LocaleController.getString(R.string.FluffyGoogleAiPromptCustomInput), prompt, messageText);
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 50));
        fragment.showDialog(dialog);
    }

    private static void requestGoogleAi(ChatActivity fragment, CharSequence title, String prompt, String messageText) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        if (!GoogleAiSettingsHook.isApiKeyValidated()) {
            showApiKeyMissingDialog(fragment);
            return;
        }
        if (TextUtils.isEmpty(prompt)) {
            AlertsCreator.showSimpleToast(fragment, LocaleController.getString(R.string.FluffyGoogleAiPromptEmpty));
            return;
        }

        String fullPrompt = prompt.trim() + "\n\nMessage:\n\"\"\"\n" + messageText.trim() + "\n\"\"\"";
        AlertDialog progressDialog = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER, fragment.getResourceProvider());
        progressDialog.setCanCancel(true);
        progressDialog.showDelayed(120);

        GoogleAiClient.generateContent(GoogleAiSettingsHook.getApiBaseUrl(), GoogleAiSettingsHook.getApiKey(), GoogleAiSettingsHook.getModel(), fullPrompt, (responseText, errorMessage) -> {
            try {
                progressDialog.dismissUnless(500);
            } catch (Exception ignore) {
            }
            if (fragment.getParentActivity() == null) {
                return;
            }
            if (!TextUtils.isEmpty(errorMessage)) {
                showErrorDialog(fragment, errorMessage);
                return;
            }
            showResponseDialog(fragment, title, responseText);
        });
    }

    private static void showApiKeyMissingDialog(ChatActivity fragment) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyGoogleAi));
        builder.setMessage(LocaleController.getString(R.string.FluffyGoogleAiApiKeyMissing));
        builder.setPositiveButton(LocaleController.getString(R.string.FluffyOpenSettings), (dialog, which) -> fragment.presentFragment(new FluffyGoogleAiActivity()));
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }

    private static void showErrorDialog(ChatActivity fragment, String errorMessage) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyGoogleAiRequestFailed));
        builder.setMessage(errorMessage);
        builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
        fragment.showDialog(builder.create());
    }

    private static void showResponseDialog(ChatActivity fragment, CharSequence title, String responseText) {
        if (fragment.getParentActivity() == null) {
            return;
        }
        String text = TextUtils.isEmpty(responseText) ? LocaleController.getString(R.string.FluffyGoogleAiEmptyResponse) : responseText;
        CharSequence formattedText = GoogleAiMarkdownFormatter.format(text);
        Context context = fragment.getParentActivity();

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(12), AndroidUtilities.dp(24), AndroidUtilities.dp(6));

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setLinkTextColor(Theme.getColor(Theme.key_dialogTextLink));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setTextIsSelectable(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        textView.setText(formattedText);
        scrollView.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP));

        AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getParentActivity(), fragment.getResourceProvider());
        builder.setTitle(title);
        builder.setView(scrollView);
        builder.setPositiveButton(LocaleController.getString(R.string.Copy), (dialog, which) -> {
            if (AndroidUtilities.addToClipboard(text)) {
                BulletinFactory.of(fragment).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
            }
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Close), null);
        fragment.showDialog(builder.create());
    }

    private static String extractMessageText(MessageObject selectedMessage) {
        if (selectedMessage == null) {
            return null;
        }
        CharSequence text = selectedMessage.messageText;
        if (TextUtils.isEmpty(text) && selectedMessage.messageOwner != null) {
            text = selectedMessage.messageOwner.message;
        }
        if (TextUtils.isEmpty(text)) {
            return null;
        }
        return text.toString().trim();
    }
}
