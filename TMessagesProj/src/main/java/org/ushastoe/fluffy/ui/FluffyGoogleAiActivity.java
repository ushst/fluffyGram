package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.hooks.GoogleAiSettingsHook;
import org.ushastoe.fluffy.patches.FluffySettingsDeepLinkPatch;
import org.ushastoe.fluffy.patches.GoogleAiSettingsPatch;
import org.ushastoe.fluffy.utils.FluffySettingsTargetAnimator;
import org.ushastoe.fluffy.utils.GoogleAiClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;

public class FluffyGoogleAiActivity extends BaseFragment {
    private static final String ARG_TARGET = "fluffy_google_ai_target";
    private static final int VIEW_TYPE_HEADER = 0, VIEW_TYPE_SETTING = 1, VIEW_TYPE_INFO = 2, VIEW_TYPE_CHECK = 3;
    private static final int ROW_HEADER = 0, ROW_ENABLED = 1, ROW_API_KEY = 2, ROW_OPEN_KEY_PAGE = 3, ROW_REQUEST_URL = 4, ROW_MODEL = 5, ROW_PICK_MODEL = 6, ROW_PROMPTS_HEADER = 7, ROW_ADD_PROMPT = 8, ROW_GENERATION_HEADER = 9, ROW_ADD_GENERATION_PROMPT = 10, ROW_INFO = 11, ROW_PROMPT_BASE = 1000, ROW_GENERATION_PROMPT_BASE = 2000;

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<RowItem> items = new ArrayList<>();

    public FluffyGoogleAiActivity() { super(); }
    public FluffyGoogleAiActivity(Bundle args) { super(args); }

    public static FluffyGoogleAiActivity createForTarget(String target) {
        Bundle args = new Bundle();
        args.putString(ARG_TARGET, target);
        return new FluffyGoogleAiActivity(args);
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyGoogleAi));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        FrameLayout frame = new FrameLayout(context);
        frame.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        listView = new RecyclerListView(context);
        listView.setSections();
        actionBar.setAdaptiveBackground(listView);
        listView.setLayoutManager(new LinearLayoutManager(context));
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> onRowClick(position));
        listView.setOnItemLongClickListener((view, position) -> onRowLongClick(position));
        frame.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        updateItems();
        applyTargetScroll();
        fragmentView = frame;
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateItems();
        applyTargetScroll();
    }

    private void updateItems() {
        items.clear();
        boolean ready = GoogleAiSettingsHook.isApiKeyValidated();
        items.add(new RowItem(VIEW_TYPE_HEADER, ROW_HEADER, LocaleController.getString(R.string.FluffyGoogleAiSection), null, null));
        items.add(new RowItem(VIEW_TYPE_CHECK, ROW_ENABLED, LocaleController.getString(R.string.FluffyGoogleAiEnabled), null, null));
        items.add(new RowItem(VIEW_TYPE_SETTING, ROW_API_KEY, LocaleController.getString(R.string.FluffyGoogleAiApiKey), null, null));
        items.add(new RowItem(VIEW_TYPE_SETTING, ROW_OPEN_KEY_PAGE, LocaleController.getString(R.string.FluffyGoogleAiOpenApiKeyPage), null, null));
        items.add(new RowItem(VIEW_TYPE_SETTING, ROW_REQUEST_URL, LocaleController.getString(R.string.FluffyGoogleAiRequestUrl), ellipsize(GoogleAiSettingsHook.getApiBaseUrl(), 28), null));
        if (ready) {
            items.add(new RowItem(VIEW_TYPE_SETTING, ROW_MODEL, LocaleController.getString(R.string.FluffyGoogleAiModel), GoogleAiSettingsHook.getModel(), null));
            items.add(new RowItem(VIEW_TYPE_SETTING, ROW_PICK_MODEL, LocaleController.getString(R.string.FluffyGoogleAiPickAvailableModel), null, null));
            items.add(new RowItem(VIEW_TYPE_HEADER, ROW_PROMPTS_HEADER, LocaleController.getString(R.string.FluffyGoogleAiPromptsSection), null, null));
            items.add(new RowItem(VIEW_TYPE_SETTING, ROW_ADD_PROMPT, LocaleController.getString(R.string.FluffyGoogleAiAddPrompt), null, null));
            ArrayList<GoogleAiSettingsPatch.PromptPreset> presets = GoogleAiSettingsHook.getPromptPresets();
            for (int i = 0; i < presets.size(); i++) {
                GoogleAiSettingsPatch.PromptPreset preset = presets.get(i);
                items.add(new RowItem(VIEW_TYPE_SETTING, ROW_PROMPT_BASE + i, preset.title, null, preset));
            }
            items.add(new RowItem(VIEW_TYPE_HEADER, ROW_GENERATION_HEADER, LocaleController.getString(R.string.FluffyGoogleAiGenerationSection), null, null));
            items.add(new RowItem(VIEW_TYPE_SETTING, ROW_ADD_GENERATION_PROMPT, LocaleController.getString(R.string.FluffyGoogleAiAddGenerationStyle), null, null));
            ArrayList<GoogleAiSettingsPatch.PromptPreset> composePresets = GoogleAiSettingsHook.getComposePromptPresets();
            for (int i = 0; i < composePresets.size(); i++) {
                GoogleAiSettingsPatch.PromptPreset preset = composePresets.get(i);
                items.add(new RowItem(VIEW_TYPE_SETTING, ROW_GENERATION_PROMPT_BASE + i, preset.title, null, preset));
            }
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO, LocaleController.getString(R.string.FluffyGoogleAiPromptPresetInfo), null, null));
        } else {
            items.add(new RowItem(VIEW_TYPE_INFO, ROW_INFO, LocaleController.getString(R.string.FluffyGoogleAiApiKeyOnlyInfo), null, null));
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void onRowClick(int position) {
        if (position < 0 || position >= items.size()) return;
        RowItem item = items.get(position);
        if (item.id == ROW_ENABLED) { GoogleAiSettingsHook.setEnabled(!GoogleAiSettingsHook.isEnabled()); updateItems(); return; }
        if (item.id == ROW_API_KEY) { showValueDialog(ROW_API_KEY, LocaleController.getString(R.string.FluffyGoogleAiApiKey), LocaleController.getString(R.string.FluffyGoogleAiApiKeyInfo), GoogleAiSettingsHook.getApiKey(), LocaleController.getString(R.string.FluffyGoogleAiApiKeyHint), true, false); return; }
        if (item.id == ROW_OPEN_KEY_PAGE) { Browser.openUrl(getParentActivity(), "https://aistudio.google.com/apikey"); return; }
        if (item.id == ROW_REQUEST_URL) { showValueDialog(ROW_REQUEST_URL, LocaleController.getString(R.string.FluffyGoogleAiRequestUrl), LocaleController.getString(R.string.FluffyGoogleAiRequestUrlInfo), GoogleAiSettingsHook.getApiBaseUrl(), LocaleController.getString(R.string.FluffyGoogleAiRequestUrlHint), false, false); return; }
        if (item.id == ROW_MODEL) { showModelChooser(); return; }
        if (item.id == ROW_PICK_MODEL) { pickAvailableModel(); return; }
        if (item.id == ROW_ADD_PROMPT) { showPromptDialog(null, false); return; }
        if (item.id == ROW_ADD_GENERATION_PROMPT) { showPromptDialog(null, true); return; }
        if (item.preset != null) showPromptDialog(item.preset, item.id >= ROW_GENERATION_PROMPT_BASE);
    }

    private boolean onRowLongClick(int position) {
        if (position < 0 || position >= items.size()) return false;
        RowItem item = items.get(position);
        String path = "google_ai";
        if (item.id == ROW_ENABLED) path = "google_ai/enabled";
        else if (item.id == ROW_API_KEY) path = "google_ai/api-key";
        else if (item.id == ROW_REQUEST_URL) path = "google_ai/api-base-url";
        else if (item.id == ROW_MODEL) path = "google_ai/model";
        else if (item.id == ROW_PICK_MODEL) path = "google_ai/pick-available-model";
        else if (item.id == ROW_ADD_PROMPT || item.preset != null && item.id < ROW_GENERATION_PROMPT_BASE) path = "google_ai/prompts";
        else if (item.id == ROW_ADD_GENERATION_PROMPT || item.preset != null && item.id >= ROW_GENERATION_PROMPT_BASE) path = "google_ai/generation";
        return FluffySettingsDeepLinkPatch.copyLink(this, FluffySettingsDeepLinkPatch.buildSettingsLink(path));
    }

    private void showValueDialog(int rowId, CharSequence title, CharSequence message, String currentValue, CharSequence hint, boolean secret, boolean multiline) {
        Context context = getParentActivity();
        if (context == null) return;
        EditTextBoldCursor edit = createEditText(context, hint, secret, multiline);
        edit.setText(currentValue);
        edit.setSelection(edit.length());
        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(title).setView(buildDialogLayout(context, message, edit, multiline)).setNegativeButton(LocaleController.getString(R.string.Cancel), null).setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
            if (rowId == ROW_API_KEY) validateAndSaveApiKey(edit.getText().toString());
            else if (rowId == ROW_REQUEST_URL) { GoogleAiSettingsHook.setApiBaseUrl(edit.getText().toString()); updateItems(); }
            else if (rowId == ROW_MODEL) { GoogleAiSettingsHook.setModel(edit.getText().toString()); updateItems(); }
        });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> { edit.requestFocus(); AndroidUtilities.showKeyboard(edit); }, 50));
        showDialog(dialog);
    }

    private void showPromptDialog(GoogleAiSettingsPatch.PromptPreset preset, boolean compose) {
        Context context = getParentActivity();
        if (context == null) return;
        EditTextBoldCursor titleEdit = createEditText(context, LocaleController.getString(R.string.FluffyGoogleAiPromptTitleHint), false, false);
        titleEdit.setText(preset != null ? preset.title : "");
        EditTextBoldCursor promptEdit = createEditText(context, LocaleController.getString(R.string.FluffyGoogleAiPromptCustomHint), false, true);
        promptEdit.setText(preset != null ? preset.prompt : "");
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView msg = new TextView(context);
        msg.setText(LocaleController.getString(compose ? R.string.FluffyGoogleAiGenerationPresetInfo : R.string.FluffyGoogleAiPromptPresetInfo));
        msg.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        msg.setPadding(AndroidUtilities.dp(23), AndroidUtilities.dp(12), AndroidUtilities.dp(23), AndroidUtilities.dp(6));
        msg.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(msg, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        layout.addView(titleEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));
        layout.addView(promptEdit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 24, 12, 24, 0));
        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(preset == null
                        ? LocaleController.getString(compose ? R.string.FluffyGoogleAiAddGenerationStyle : R.string.FluffyGoogleAiAddPrompt)
                        : LocaleController.getString(compose ? R.string.FluffyGoogleAiEditGenerationStyle : R.string.FluffyGoogleAiEditPrompt))
                .setView(layout)
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .setPositiveButton(LocaleController.getString(R.string.Save), (dialog, which) -> {
                    if (compose) GoogleAiSettingsHook.upsertComposePromptPreset(preset == null ? null : preset.id, titleEdit.getText().toString(), promptEdit.getText().toString());
                    else GoogleAiSettingsHook.upsertPromptPreset(preset == null ? null : preset.id, titleEdit.getText().toString(), promptEdit.getText().toString());
                    updateItems();
                });
        if (preset != null) builder.setNeutralButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
            if (compose) GoogleAiSettingsHook.deleteComposePromptPreset(preset.id);
            else GoogleAiSettingsHook.deletePromptPreset(preset.id);
            updateItems();
        });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> AndroidUtilities.runOnUIThread(() -> { titleEdit.requestFocus(); AndroidUtilities.showKeyboard(titleEdit); }, 50));
        showDialog(dialog);
    }

    private void validateAndSaveApiKey(String value) {
        String apiKey = value == null ? "" : value.trim();
        if (TextUtils.isEmpty(apiKey)) { GoogleAiSettingsHook.clearApiKey(); updateItems(); return; }
        if (getParentActivity() == null) return;
        AlertDialog progress = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER, getResourceProvider());
        progress.setCanCancel(true);
        progress.showDelayed(120);
        GoogleAiClient.validateApiKey(GoogleAiSettingsHook.getApiBaseUrl(), apiKey, (responseText, errorMessage) -> {
            try { progress.dismissUnless(500); } catch (Exception ignore) {}
            if (getParentActivity() == null) return;
            if (!TextUtils.isEmpty(errorMessage)) { GoogleAiSettingsHook.setApiKeyValidated(false); showError(errorMessage); return; }
            GoogleAiSettingsHook.setApiKey(apiKey);
            GoogleAiSettingsHook.setApiKeyValidated(true);
            updateItems();
            AlertsCreator.showSimpleToast(this, LocaleController.getString(R.string.FluffyGoogleAiKeyValidated));
        });
    }

    private void showModelChooser() { requestModels(false); }
    private void pickAvailableModel() { requestModels(true); }

    private void requestModels(boolean autoPick) {
        Context context = getParentActivity();
        if (context == null || !GoogleAiSettingsHook.isApiKeyValidated()) return;
        AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER, getResourceProvider());
        progress.setCanCancel(true);
        progress.showDelayed(120);
        GoogleAiClient.listModels(GoogleAiSettingsHook.getApiBaseUrl(), GoogleAiSettingsHook.getApiKey(), (models, errorMessage) -> {
            if (getParentActivity() == null) return;
            if (!TextUtils.isEmpty(errorMessage)) { try { progress.dismissUnless(500); } catch (Exception ignore) {} showError(errorMessage); return; }
            ArrayList<ModelOption> options = buildModelOptions(models);
            if (!autoPick) { try { progress.dismissUnless(500); } catch (Exception ignore) {} showModelChooserDialog(options); return; }
            ArrayList<String> candidates = new ArrayList<>();
            for (int i = 0; i < options.size(); i++) if (!options.get(i).customInput && !TextUtils.isEmpty(options.get(i).modelName)) candidates.add(options.get(i).modelName);
            if (candidates.isEmpty()) { try { progress.dismissUnless(500); } catch (Exception ignore) {} AlertsCreator.showSimpleToast(this, LocaleController.getString(R.string.FluffyGoogleAiNoSuitableModels)); return; }
            tryCandidate(candidates, 0, progress, null);
        });
    }

    private void tryCandidate(ArrayList<String> candidates, int index, AlertDialog progress, String lastError) {
        if (getParentActivity() == null) return;
        if (index >= candidates.size()) { try { progress.dismissUnless(500); } catch (Exception ignore) {} if (TextUtils.isEmpty(lastError)) AlertsCreator.showSimpleToast(this, LocaleController.getString(R.string.FluffyGoogleAiNoSuitableModels)); else showError(lastError); return; }
        String candidate = candidates.get(index);
        GoogleAiClient.validateModel(GoogleAiSettingsHook.getApiBaseUrl(), GoogleAiSettingsHook.getApiKey(), candidate, (responseText, errorMessage) -> {
            if (getParentActivity() == null) return;
            if (TextUtils.isEmpty(errorMessage)) {
                try { progress.dismissUnless(500); } catch (Exception ignore) {}
                GoogleAiSettingsHook.setModel(candidate);
                updateItems();
                AlertsCreator.showSimpleToast(this, LocaleController.formatString(R.string.FluffyGoogleAiModelPicked, candidate));
            } else {
                tryCandidate(candidates, index + 1, progress, errorMessage);
            }
        });
    }

    private void showModelChooserDialog(ArrayList<ModelOption> options) {
        Context context = getParentActivity();
        if (context == null) return;
        ArrayList<CharSequence> labels = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) labels.add(options.get(i).label);
        new AlertDialog.Builder(context, getResourceProvider())
                .setTitle(LocaleController.getString(R.string.FluffyGoogleAiModel))
                .setItems(labels.toArray(new CharSequence[0]), (dialog, which) -> {
                    ModelOption option = options.get(which);
                    if (option.customInput) showValueDialog(ROW_MODEL, LocaleController.getString(R.string.FluffyGoogleAiModel), LocaleController.getString(R.string.FluffyGoogleAiModelInfo), GoogleAiSettingsHook.getModel(), LocaleController.getString(R.string.FluffyGoogleAiModelHint), false, false);
                    else { GoogleAiSettingsHook.setModel(option.modelName); updateItems(); }
                })
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .show();
    }

    private ArrayList<ModelOption> buildModelOptions(ArrayList<GoogleAiClient.ModelInfo> models) {
        ArrayList<ModelOption> result = new ArrayList<>();
        ArrayList<GoogleAiClient.ModelInfo> suitable = new ArrayList<>();
        if (models != null) for (int i = 0; i < models.size(); i++) if (isModelSuitable(models.get(i))) suitable.add(models.get(i));
        Collections.sort(suitable, Comparator.comparingInt(this::scoreModel).reversed().thenComparing(m -> modelTitle(m).toLowerCase()));
        LinkedHashSet<String> added = new LinkedHashSet<>();
        addRecommended(result, suitable, added, "pro", LocaleController.getString(R.string.FluffyGoogleAiModelLabelQuality));
        addRecommended(result, suitable, added, "flash-lite", LocaleController.getString(R.string.FluffyGoogleAiModelLabelFast));
        addRecommended(result, suitable, added, "flash", LocaleController.getString(R.string.FluffyGoogleAiModelLabelBalanced));
        for (int i = 0; i < suitable.size(); i++) {
            String modelName = stripModelPrefix(suitable.get(i).name);
            if (!TextUtils.isEmpty(modelName) && !added.contains(modelName)) result.add(new ModelOption(modelName, modelTitle(suitable.get(i)), false));
        }
        result.add(new ModelOption(null, LocaleController.getString(R.string.FluffyGoogleAiModelCustomInput), true));
        return result;
    }

    private void addRecommended(ArrayList<ModelOption> out, ArrayList<GoogleAiClient.ModelInfo> models, LinkedHashSet<String> added, String token, String badge) {
        GoogleAiClient.ModelInfo best = null; int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < models.size(); i++) {
            String modelName = stripModelPrefix(models.get(i).name);
            if (TextUtils.isEmpty(modelName) || added.contains(modelName) || !modelName.toLowerCase().contains(token)) continue;
            int score = scoreModel(models.get(i));
            if (score > bestScore) { best = models.get(i); bestScore = score; }
        }
        if (best != null) { String modelName = stripModelPrefix(best.name); out.add(new ModelOption(modelName, modelTitle(best) + "  [" + badge + "]", false)); added.add(modelName); }
    }

    private boolean isModelSuitable(GoogleAiClient.ModelInfo model) {
        String name = stripModelPrefix(model.name).toLowerCase();
        return model != null && model.supportsGenerateContent() && name.contains("gemini") && !name.contains("embedding") && !name.contains("image") && !name.contains("imagen") && !name.contains("veo") && !name.contains("tts") && !name.contains("aqa") && !name.contains("audio");
    }

    private int scoreModel(GoogleAiClient.ModelInfo model) {
        String name = stripModelPrefix(model.name).toLowerCase();
        int score = name.contains("gemini-2.5") ? 500 : name.contains("gemini-2.0") ? 400 : name.contains("gemini-1.5") ? 300 : 0;
        if (name.contains("pro")) score += 120;
        if (name.contains("flash")) score += 100;
        if (name.contains("flash-lite")) score += 80;
        if (name.contains("preview")) score -= 50;
        score += Math.min(model.inputTokenLimit / 4096, 40) + Math.min(model.outputTokenLimit / 4096, 20);
        return score;
    }

    private String modelTitle(GoogleAiClient.ModelInfo model) {
        String display = TextUtils.isEmpty(model.displayName) ? stripModelPrefix(model.name) : model.displayName;
        return display + " (" + stripModelPrefix(model.name) + ")";
    }

    private String stripModelPrefix(String value) { return TextUtils.isEmpty(value) ? "" : value.startsWith("models/") ? value.substring(7) : value; }

    private void showError(String text) {
        if (getParentActivity() == null) return;
        new AlertDialog.Builder(getParentActivity(), getResourceProvider()).setTitle(LocaleController.getString(R.string.FluffyGoogleAiRequestFailed)).setMessage(text).setPositiveButton(LocaleController.getString(R.string.OK), null).show();
    }

    private EditTextBoldCursor createEditText(Context context, CharSequence hint, boolean secret, boolean multiline) {
        EditTextBoldCursor edit = new EditTextBoldCursor(context);
        edit.setBackground(null);
        edit.setLineColors(Theme.getColor(Theme.key_dialogInputField), Theme.getColor(Theme.key_dialogInputFieldActivated), Theme.getColor(Theme.key_text_RedBold));
        edit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        edit.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        edit.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        edit.setHint(hint);
        edit.setGravity(Gravity.LEFT | Gravity.TOP);
        edit.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        edit.setCursorSize(AndroidUtilities.dp(20));
        edit.setCursorWidth(1.5f);
        edit.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        if (multiline) { edit.setMinLines(3); edit.setMaxLines(8); edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); }
        else if (secret) { edit.setSingleLine(true); edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); }
        else { edit.setSingleLine(true); edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES); }
        return edit;
    }

    private LinearLayout buildDialogLayout(Context context, CharSequence message, EditTextBoldCursor editText, boolean multiline) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        TextView msg = new TextView(context);
        msg.setText(message);
        msg.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        msg.setPadding(AndroidUtilities.dp(23), AndroidUtilities.dp(12), AndroidUtilities.dp(23), AndroidUtilities.dp(6));
        msg.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(msg, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        layout.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, multiline ? LayoutHelper.WRAP_CONTENT : 36, Gravity.TOP | Gravity.LEFT, 24, 6, 24, 0));
        return layout;
    }

    private void applyTargetScroll() {
        if (listView == null) return;
        int rowId = getTargetRowId();
        if (rowId < 0) return;
        for (int i = 0; i < items.size(); i++) if (items.get(i).id == rowId) { int index = i; listView.post(() -> FluffySettingsTargetAnimator.scrollAndPulseTarget(listView, index)); break; }
    }

    private int getTargetRowId() {
        Bundle args = getArguments();
        if (args == null) return -1;
        String target = args.getString(ARG_TARGET);
        if ("enabled".equals(target)) return ROW_ENABLED;
        if ("api-key".equals(target)) return ROW_API_KEY;
        if ("api-base-url".equals(target)) return ROW_REQUEST_URL;
        if ("model".equals(target)) return ROW_MODEL;
        if ("pick-available-model".equals(target)) return ROW_PICK_MODEL;
        if ("prompts".equals(target) || "add-prompt".equals(target)) return ROW_ADD_PROMPT;
        if ("generation".equals(target) || "add-generation-prompt".equals(target)) return ROW_ADD_GENERATION_PROMPT;
        return -1;
    }

    private String ellipsize(String value, int max) {
        if (TextUtils.isEmpty(value)) return LocaleController.getString(R.string.FluffyGoogleAiNotSet);
        String text = value.replace('\n', ' ').trim();
        return text.length() > max ? text.substring(0, max).trim() + "..." : text;
    }

    private static class RowItem {
        final int viewType, id;
        final CharSequence text, value;
        final GoogleAiSettingsPatch.PromptPreset preset;
        RowItem(int viewType, int id, CharSequence text, CharSequence value, GoogleAiSettingsPatch.PromptPreset preset) { this.viewType = viewType; this.id = id; this.text = text; this.value = value; this.preset = preset; }
    }

    private static class ModelOption {
        final String modelName; final CharSequence label; final boolean customInput;
        ModelOption(String modelName, CharSequence label, boolean customInput) { this.modelName = modelName; this.label = label; this.customInput = customInput; }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override public int getItemCount() { return items.size(); }
        @Override public boolean isEnabled(RecyclerView.ViewHolder holder) { int type = holder.getItemViewType(); return type == VIEW_TYPE_SETTING || type == VIEW_TYPE_CHECK; }
        @Override public int getItemViewType(int position) { return items.get(position).viewType; }
        @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            if (viewType == VIEW_TYPE_HEADER) view = new HeaderCell(parent.getContext());
            else if (viewType == VIEW_TYPE_CHECK) { view = new TextCheckCell(parent.getContext()); view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite)); }
            else if (viewType == VIEW_TYPE_SETTING) { view = new TextSettingsCell(parent.getContext()); view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite)); }
            else view = new TextInfoPrivacyCell(parent.getContext());
            return new RecyclerListView.Holder(view);
        }
        @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowItem item = items.get(position);
            if (holder.getItemViewType() == VIEW_TYPE_HEADER) ((HeaderCell) holder.itemView).setText(item.text);
            else if (holder.getItemViewType() == VIEW_TYPE_CHECK) ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, GoogleAiSettingsHook.isEnabled(), false);
            else if (holder.getItemViewType() == VIEW_TYPE_SETTING) ((TextSettingsCell) holder.itemView).setTextAndValue(item.text, item.value, false);
            else ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
        }
    }
}
