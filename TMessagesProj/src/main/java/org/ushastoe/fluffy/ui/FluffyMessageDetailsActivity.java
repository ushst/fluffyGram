package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import android.text.style.ForegroundColorSpan;

public class FluffyMessageDetailsActivity extends BaseFragment {

    private static final int MENU_JSON = 1;

    private final MessageObject messageObject;
    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<RowData> rows = new ArrayList<>();

    private FrameLayout jsonContainer;
    private ScrollView jsonScroll;
    private TextView jsonView;
    private boolean showJsonMode;
    private boolean jsonBuildInProgress;
    private int jsonBuildToken;
    private String cachedJsonRaw;
    private CharSequence cachedJsonStyled;

    public FluffyMessageDetailsActivity(MessageObject messageObject) {
        this.messageObject = messageObject;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.MessageDetails));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_JSON) {
                    toggleJsonMode();
                }
            }
        });
        actionBar.createMenu().addItem(MENU_JSON, R.drawable.msg_info).setContentDescription(LocaleController.getString(R.string.JSON));

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> {
            if (position >= 0 && position < rows.size()) {
                AndroidUtilities.addToClipboard(rows.get(position).value);
            }
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        jsonContainer = new FrameLayout(context);
        jsonContainer.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        jsonView = new TextView(context);
        jsonView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        jsonView.setTextSize(11);
        jsonView.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        jsonView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        jsonView.setTypeface(Typeface.MONOSPACE);

        jsonScroll = new ScrollView(context);
        jsonScroll.setFillViewport(true);
        jsonScroll.addView(jsonView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        jsonContainer.addView(jsonScroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        frameLayout.addView(jsonContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        fragmentView = frameLayout;
        rebuildRows();
        updateViewMode();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        rebuildRows();
        updateViewMode();
    }

    private void rebuildRows() {
        invalidateJsonCache();
        rows.clear();
        if (messageObject == null || messageObject.messageOwner == null) {
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
            return;
        }

        TLRPC.Message message = messageObject.messageOwner;
        addRow(LocaleController.getString(R.string.idRow), String.valueOf(message.id));
        addRow(LocaleController.getString(R.string.dateRow), formatTime(message.date));

        if (!TextUtils.isEmpty(messageObject.messageText)) {
            addRow(LocaleController.getString(R.string.textRow), messageObject.messageText.toString());
        }
        if (!TextUtils.isEmpty(messageObject.caption)) {
            addRow(LocaleController.getString(R.string.captionRow), messageObject.caption.toString());
        }
        if (message.edit_date != 0) {
            addRow(LocaleController.getString(R.string.editDateRow), formatTime(message.edit_date));
        }
        if (message.fwd_from != null) {
            int forwardedOriginalDate = message.fwd_from.date != 0 ? message.fwd_from.date : message.fwd_from.saved_date;
            if (forwardedOriginalDate > 0) {
                addRow(LocaleController.getString(R.string.FluffyForwardedOriginalDateMenu), formatTime(forwardedOriginalDate));
            }
        }
        if (message.from_scheduled) {
            addRow(LocaleController.getString(R.string.scheduleRow), String.valueOf(true));
        }
        if (message.silent) {
            addRow(LocaleController.getString(R.string.silenceRow), String.valueOf(true));
        }

        if (message.from_id != null) {
            if (message.from_id.user_id != 0) {
                addRow(LocaleController.getString(R.string.ownerIdRow), String.valueOf(message.from_id.user_id));
            } else if (message.from_id.channel_id != 0) {
                addRow(LocaleController.getString(R.string.ownerIdRow), String.valueOf(-message.from_id.channel_id));
            } else if (message.from_id.chat_id != 0) {
                addRow(LocaleController.getString(R.string.ownerIdRow), String.valueOf(-message.from_id.chat_id));
            }
        }

        long docId = 0;
        TLRPC.MessageMedia media = MessageObject.getMedia(message);
        if (media != null) {
            if (media.document != null) {
                docId = media.document.id;
            } else if (media.photo != null) {
                docId = media.photo.id;
            }
        }
        if (docId != 0) {
            addRow(LocaleController.getString(R.string.docIdRow), String.valueOf(docId));
        }

        String filePath = resolveFilePath(messageObject);
        if (!TextUtils.isEmpty(filePath)) {
            addRow(LocaleController.getString(R.string.filePathRow), filePath);
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private String resolveFilePath(MessageObject object) {
        String path = object.messageOwner.attachPath;
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (file.exists()) {
                return path;
            }
        }

        File fromMessage = FileLoader.getInstance(currentAccount).getPathToMessage(object.messageOwner);
        if (fromMessage != null && fromMessage.exists()) {
            return fromMessage.getAbsolutePath();
        }

        File fromAttach = FileLoader.getInstance(currentAccount).getPathToAttach(object.getDocument(), true);
        if (fromAttach != null && fromAttach.exists()) {
            return fromAttach.getAbsolutePath();
        }
        return null;
    }

    private void addRow(String title, String value) {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        rows.add(new RowData(title, value));
    }

    private String formatTime(int timestamp) {
        if (timestamp == 0x7ffffffe) {
            return LocaleController.getString(R.string.MessageScheduledUntilOnline);
        }
        Date date = new Date(timestamp * 1000L);
        return timestamp + "\n" + LocaleController.formatString(
                R.string.formatDateAtTime,
                LocaleController.getInstance().getFormatterYear().format(date),
                LocaleController.getInstance().getFormatterDayWithSeconds().format(date));
    }

    private void toggleJsonMode() {
        showJsonMode = !showJsonMode;
        updateViewMode();
    }

    private void updateViewMode() {
        if (listView != null) {
            listView.setVisibility(showJsonMode ? View.GONE : View.VISIBLE);
        }
        if (jsonContainer != null) {
            jsonContainer.setVisibility(showJsonMode ? View.VISIBLE : View.GONE);
        }
        if (showJsonMode) {
            updateJsonView();
        }
    }

    private void updateJsonView() {
        if (jsonView == null || !showJsonMode) {
            return;
        }
        if (cachedJsonStyled != null) {
            jsonView.setText(cachedJsonStyled, TextView.BufferType.SPANNABLE);
            return;
        }
        if (jsonBuildInProgress) {
            return;
        }

        jsonBuildInProgress = true;
        final int token = ++jsonBuildToken;
        jsonView.setText("{\n  \"status\": \"building json...\"\n}");

        new Thread(() -> {
            String raw = messageObjectToJsonString(messageObject);
            CharSequence styled = applyJsonSyntaxHighlight(raw);
            AndroidUtilities.runOnUIThread(() -> {
                if (token != jsonBuildToken) {
                    return;
                }
                jsonBuildInProgress = false;
                cachedJsonRaw = raw;
                cachedJsonStyled = styled;
                if (jsonView != null && showJsonMode) {
                    jsonView.setText(cachedJsonStyled, TextView.BufferType.SPANNABLE);
                }
            });
        }, "FluffyJsonBuilder").start();
    }

    private void invalidateJsonCache() {
        jsonBuildToken++;
        jsonBuildInProgress = false;
        cachedJsonRaw = null;
        cachedJsonStyled = null;
    }

    private CharSequence applyJsonSyntaxHighlight(String rawJson) {
        if (TextUtils.isEmpty(rawJson)) {
            return "{}";
        }

        SpannableStringBuilder sb = new SpannableStringBuilder(rawJson);
        final int keyColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2);
        final int stringColor = Theme.getColor(Theme.key_windowBackgroundWhiteGreenText);
        final int numberColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText);
        final int boolColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText3);
        final int nullColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3);
        final int punctuationColor = Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);

        int i = 0;
        while (i < rawJson.length()) {
            char ch = rawJson.charAt(i);

            if (ch == '"') {
                int start = i;
                i++;
                while (i < rawJson.length()) {
                    char c = rawJson.charAt(i);
                    if (c == '"' && rawJson.charAt(i - 1) != '\\') {
                        i++;
                        break;
                    }
                    i++;
                }

                int color = stringColor;
                int j = i;
                while (j < rawJson.length() && Character.isWhitespace(rawJson.charAt(j))) {
                    j++;
                }
                if (j < rawJson.length() && rawJson.charAt(j) == ':') {
                    color = keyColor;
                }
                sb.setSpan(new ForegroundColorSpan(color), start, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            if (ch == '{' || ch == '}' || ch == '[' || ch == ']' || ch == ':' || ch == ',') {
                sb.setSpan(new ForegroundColorSpan(punctuationColor), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i++;
                continue;
            }

            if ((ch >= '0' && ch <= '9') || ch == '-') {
                int start = i;
                i++;
                while (i < rawJson.length()) {
                    char c = rawJson.charAt(i);
                    if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                        i++;
                    } else {
                        break;
                    }
                }
                sb.setSpan(new ForegroundColorSpan(numberColor), start, i, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            if (startsWithToken(rawJson, i, "true") || startsWithToken(rawJson, i, "false")) {
                int len = startsWithToken(rawJson, i, "true") ? 4 : 5;
                sb.setSpan(new ForegroundColorSpan(boolColor), i, i + len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i += len;
                continue;
            }

            if (startsWithToken(rawJson, i, "null")) {
                sb.setSpan(new ForegroundColorSpan(nullColor), i, i + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                i += 4;
                continue;
            }

            i++;
        }
        return sb;
    }

    private boolean startsWithToken(String source, int index, String token) {
        if (index < 0 || index + token.length() > source.length()) {
            return false;
        }
        if (!source.regionMatches(index, token, 0, token.length())) {
            return false;
        }
        int left = index - 1;
        int right = index + token.length();
        boolean leftOk = left < 0 || !Character.isLetterOrDigit(source.charAt(left));
        boolean rightOk = right >= source.length() || !Character.isLetterOrDigit(source.charAt(right));
        return leftOk && rightOk;
    }

    private String messageObjectToJsonString(MessageObject obj) {
        if (obj == null || obj.messageOwner == null) {
            return "{}";
        }

        try {
            JSONObject root = new JSONObject();
            Map<Object, Boolean> visited = new IdentityHashMap<>();
            root.put("messageOwner", toJsonValue(obj.messageOwner, visited, 0));

            if (!TextUtils.isEmpty(obj.messageText)) {
                root.put("messageText", obj.messageText.toString());
            }
            if (!TextUtils.isEmpty(obj.caption)) {
                root.put("captionText", obj.caption.toString());
            }

            String filePath = resolveFilePath(obj);
            if (!TextUtils.isEmpty(filePath)) {
                root.put("resolvedFilePath", filePath);
            }

            return root.toString(2);
        } catch (Throwable e) {
            return "{\n  \"error\": \"failed to build raw json\",\n  \"reason\": " + JSONObject.quote(String.valueOf(e)) + "\n}";
        }
    }

    private Object toJsonValue(Object value, Map<Object, Boolean> visited, int depth) {
        if (value == null) {
            return JSONObject.NULL;
        }
        if (depth > 8) {
            return "<max_depth>";
        }

        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return value;
        }
        if (value instanceof CharSequence) {
            return value.toString();
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }

        if (visited.containsKey(value)) {
            return "<cycle>";
        }
        visited.put(value, Boolean.TRUE);

        try {
            Class<?> cls = value.getClass();

            if (cls.isArray()) {
                JSONArray array = new JSONArray();
                int len = Array.getLength(value);
                for (int i = 0; i < len; i++) {
                    array.put(toJsonValue(Array.get(value, i), visited, depth + 1));
                }
                return array;
            }

            if (value instanceof List<?>) {
                JSONArray array = new JSONArray();
                for (Object item : (List<?>) value) {
                    array.put(toJsonValue(item, visited, depth + 1));
                }
                return array;
            }

            JSONObject object = new JSONObject();
            object.put("_type", cls.getName());
            for (Field field : getAllFields(cls)) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || field.isSynthetic()) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    object.put(field.getName(), toJsonValue(field.get(value), visited, depth + 1));
                } catch (Throwable fieldError) {
                    object.put(field.getName(), "<error:" + fieldError.getClass().getSimpleName() + ">");
                }
            }
            return object;
        } catch (Throwable e) {
            return "<error:" + e.getClass().getSimpleName() + ">";
        } finally {
            visited.remove(value);
        }
    }

    private ArrayList<Field> getAllFields(Class<?> cls) {
        ArrayList<Field> fields = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            Field[] declared = current.getDeclaredFields();
            for (Field field : declared) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private static class RowData {
        final String title;
        final String value;

        RowData(String title, String value) {
            this.title = title;
            this.value = value;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        @Override
        public int getItemCount() {
            return rows.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextDetailSettingsCell cell = new TextDetailSettingsCell(parent.getContext());
            cell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cell.setLayoutParams(new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT));
            cell.setMultilineDetail(true);
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowData row = rows.get(position);
            ((TextDetailSettingsCell) holder.itemView).setTextAndValue(row.title, row.value, false);
        }
    }
}
