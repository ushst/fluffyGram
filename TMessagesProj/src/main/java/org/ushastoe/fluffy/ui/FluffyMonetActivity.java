package org.ushastoe.fluffy.ui;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.ushastoe.fluffy.monet.MonetPalette;
import org.ushastoe.fluffy.patches.MonetThemePatch;

import java.util.ArrayList;
import java.util.Locale;

public class FluffyMonetActivity extends BaseFragment {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_CHECK = 2;
    private static final int VIEW_TYPE_SHADOW = 3;
    private static final int VIEW_TYPE_INFO = 4;

    private static final int ROW_INFO = 0;
    private static final int ROW_PALETTE_HEADER = 1;
    private static final int ROW_PALETTE_SOURCE = 2;
    private static final int ROW_SEED_COLOR = 3;
    private static final int ROW_SCHEME = 4;
    private static final int ROW_STYLE_SECTION = 5;
    private static final int ROW_STYLE_HEADER = 6;
    private static final int ROW_AMOLED = 7;
    private static final int ROW_GRADIENT_BUBBLES = 8;
    private static final int ROW_GRADIENT_AVATARS = 9;
    private static final int ROW_MONOCHROME_NAMES = 10;
    private static final int ROW_VISIBLE_DIVIDERS = 11;
    private static final int ROW_INVERT_OUTGOING = 12;
    private static final int ROW_APPLY_SECTION = 13;
    private static final int ROW_APPLY_LIGHT = 14;
    private static final int ROW_APPLY_DARK = 15;
    private static final int ROW_BOTTOM_SHADOW = 16;

    private static final int[] PRESET_COLORS = {
            0xff1a73e8, 0xff008080, 0xff2e7d32, 0xffb8860b,
            0xffe8710a, 0xffc5221f, 0xffd81b60, 0xff7b1fa2,
    };
    private static final int[] PRESET_NAMES = {
            R.string.FluffyMonetColorBlue, R.string.FluffyMonetColorTeal,
            R.string.FluffyMonetColorGreen, R.string.FluffyMonetColorYellow,
            R.string.FluffyMonetColorOrange, R.string.FluffyMonetColorRed,
            R.string.FluffyMonetColorPink, R.string.FluffyMonetColorPurple,
    };

    private static final int[] SCHEME_NAMES = {
            R.string.FluffyMonetSchemeTonalSpot, R.string.FluffyMonetSchemeVibrant,
            R.string.FluffyMonetSchemeExpressive, R.string.FluffyMonetSchemeNeutral,
            R.string.FluffyMonetSchemeMonochrome, R.string.FluffyMonetSchemeRainbow,
            R.string.FluffyMonetSchemeFruitSalad, R.string.FluffyMonetSchemeContent,
            R.string.FluffyMonetSchemeFidelity,
    };

    private RecyclerListView listView;
    private ListAdapter adapter;
    private final ArrayList<ItemInner> items = new ArrayList<>();

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString(R.string.FluffyMonetTitle));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(adapter = new ListAdapter());
        listView.setOnItemClickListener((view, position) -> onItemClick(view, position));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateItems();

        fragmentView = frameLayout;
        return fragmentView;
    }

    private void onItemClick(View view, int position) {
        if (position < 0 || position >= items.size()) {
            return;
        }
        int id = items.get(position).id;
        if (id == ROW_PALETTE_SOURCE) {
            showPaletteSourceDialog();
        } else if (id == ROW_SEED_COLOR) {
            showSeedColorDialog();
        } else if (id == ROW_SCHEME) {
            showSchemeDialog();
        } else if (id == ROW_AMOLED) {
            toggle(view, !MonetThemePatch.isAmoled(), MonetThemePatch::setAmoled);
        } else if (id == ROW_GRADIENT_BUBBLES) {
            toggle(view, !MonetThemePatch.useGradientBubbles(), MonetThemePatch::setUseGradientBubbles);
        } else if (id == ROW_GRADIENT_AVATARS) {
            toggle(view, !MonetThemePatch.useGradientAvatars(), MonetThemePatch::setUseGradientAvatars);
        } else if (id == ROW_MONOCHROME_NAMES) {
            toggle(view, !MonetThemePatch.useMonochromeNames(), MonetThemePatch::setUseMonochromeNames);
        } else if (id == ROW_VISIBLE_DIVIDERS) {
            toggle(view, !MonetThemePatch.useVisibleDividers(), MonetThemePatch::setUseVisibleDividers);
        } else if (id == ROW_INVERT_OUTGOING) {
            toggle(view, !MonetThemePatch.invertOutgoing(), MonetThemePatch::setInvertOutgoing);
        } else if (id == ROW_APPLY_LIGHT) {
            applyTheme(MonetThemePatch.THEME_NAME_LIGHT);
        } else if (id == ROW_APPLY_DARK) {
            applyTheme(MonetThemePatch.THEME_NAME_DARK);
        }
    }

    private interface BooleanSetter {
        void set(boolean value);
    }

    private void toggle(View view, boolean value, BooleanSetter setter) {
        setter.set(value);
        if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setChecked(value);
        }
    }

    private void applyTheme(String themeName) {
        Theme.ThemeInfo themeInfo = Theme.getTheme(themeName);
        if (themeInfo == null) {
            return;
        }
        NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.needSetDayNightTheme, themeInfo, false, null, -1);
        updateItems();
    }

    private void updateItems() {
        items.clear();
        items.add(new ItemInner(VIEW_TYPE_INFO, ROW_INFO,
                LocaleController.getString(R.string.FluffyMonetInfo), false));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_PALETTE_HEADER,
                LocaleController.getString(R.string.FluffyMonetPaletteSection), false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_PALETTE_SOURCE,
                LocaleController.getString(R.string.FluffyMonetPaletteSource), false));
        if (MonetThemePatch.getPaletteSource() == MonetThemePatch.PALETTE_SOURCE_CUSTOM
                || !MonetPalette.isSystemPaletteAvailable()) {
            items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SEED_COLOR,
                    LocaleController.getString(R.string.FluffyMonetSeedColor), false));
            items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_SCHEME,
                    LocaleController.getString(R.string.FluffyMonetScheme), false));
        }

        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_STYLE_SECTION, "", false));
        items.add(new ItemInner(VIEW_TYPE_HEADER, ROW_STYLE_HEADER,
                LocaleController.getString(R.string.FluffyMonetStyleSection), false));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_AMOLED,
                LocaleController.getString(R.string.FluffyMonetAmoled), MonetThemePatch.isAmoled()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_GRADIENT_BUBBLES,
                LocaleController.getString(R.string.FluffyMonetGradientBubbles), MonetThemePatch.useGradientBubbles()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_GRADIENT_AVATARS,
                LocaleController.getString(R.string.FluffyMonetGradientAvatars), MonetThemePatch.useGradientAvatars()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_MONOCHROME_NAMES,
                LocaleController.getString(R.string.FluffyMonetMonochromeNames), MonetThemePatch.useMonochromeNames()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_VISIBLE_DIVIDERS,
                LocaleController.getString(R.string.FluffyMonetVisibleDividers), MonetThemePatch.useVisibleDividers()));
        items.add(new ItemInner(VIEW_TYPE_CHECK, ROW_INVERT_OUTGOING,
                LocaleController.getString(R.string.FluffyMonetInvertOutgoing), MonetThemePatch.invertOutgoing()));

        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_APPLY_SECTION, "", false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_APPLY_LIGHT,
                LocaleController.getString(R.string.FluffyMonetApplyLight), false));
        items.add(new ItemInner(VIEW_TYPE_TEXT, ROW_APPLY_DARK,
                LocaleController.getString(R.string.FluffyMonetApplyDark), false));
        items.add(new ItemInner(VIEW_TYPE_SHADOW, ROW_BOTTOM_SHADOW, "", false));

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showPaletteSourceDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[]{
                LocaleController.getString(MonetPalette.isSystemPaletteAvailable()
                        ? R.string.FluffyMonetPaletteSourceSystem
                        : R.string.FluffyMonetPaletteSourceSystemUnavailable),
                LocaleController.getString(R.string.FluffyMonetPaletteSourceCustom),
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyMonetPaletteSource));
        builder.setItems(options, (dialog, which) -> {
            MonetThemePatch.setPaletteSource(which == 1
                    ? MonetThemePatch.PALETTE_SOURCE_CUSTOM : MonetThemePatch.PALETTE_SOURCE_SYSTEM);
            updateItems();
        });
        showDialog(builder.create());
    }

    private void showSchemeDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[SCHEME_NAMES.length];
        for (int i = 0; i < SCHEME_NAMES.length; i++) {
            options[i] = LocaleController.getString(SCHEME_NAMES[i]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyMonetScheme));
        builder.setItems(options, (dialog, which) -> {
            MonetThemePatch.setScheme(which);
            updateItems();
        });
        showDialog(builder.create());
    }

    private void showSeedColorDialog() {
        if (getParentActivity() == null) {
            return;
        }
        CharSequence[] options = new CharSequence[PRESET_COLORS.length + 1];
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            options[i] = LocaleController.getString(PRESET_NAMES[i]);
        }
        options[PRESET_COLORS.length] = LocaleController.getString(R.string.FluffyMonetSeedColorCustom);

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyMonetSeedColor));
        builder.setItems(options, (dialog, which) -> {
            if (which == PRESET_COLORS.length) {
                showCustomSeedColorDialog();
                return;
            }
            MonetThemePatch.setSeedColor(PRESET_COLORS[which]);
            updateItems();
        });
        showDialog(builder.create());
    }

    private void showCustomSeedColorDialog() {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_dialogTextHint));
        editText.setBackgroundDrawable(null);
        editText.setLineColors(
                Theme.getColor(Theme.key_dialogInputField),
                Theme.getColor(Theme.key_dialogInputFieldActivated),
                Theme.getColor(Theme.key_text_RedRegular));
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        editText.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        editText.setHint(LocaleController.getString(R.string.FluffyMonetSeedColorHint));
        editText.setText(String.format(Locale.US, "%06X", MonetThemePatch.getSeedColor() & 0x00ffffff));
        editText.setSelection(editText.length());

        FrameLayout container = new FrameLayout(context);
        container.addView(editText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 24, 6, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(context, getResourceProvider());
        builder.setTitle(LocaleController.getString(R.string.FluffyMonetSeedColor));
        builder.setView(container);
        builder.setPositiveButton(LocaleController.getString(R.string.Done), (dialog, which) -> {
            Integer color = parseSeedColor(editText.getText().toString());
            if (color == null) {
                BulletinFactory.of(this)
                        .createErrorBulletin(LocaleController.getString(R.string.FluffyMonetSeedColorInvalid))
                        .show();
                return;
            }
            MonetThemePatch.setSeedColor(color);
            updateItems();
        });
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(builder.create());
        AndroidUtilities.runOnUIThread(() -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        }, 100);
    }

    private static Integer parseSeedColor(String input) {
        if (TextUtils.isEmpty(input)) {
            return null;
        }
        String value = input.trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6 && value.length() != 8) {
            return null;
        }
        try {
            return (int) (0xff000000L | (Long.parseLong(value, 16) & 0x00ffffffL));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CharSequence getPaletteSourceValue() {
        if (!MonetPalette.isSystemPaletteAvailable()
                || MonetThemePatch.getPaletteSource() == MonetThemePatch.PALETTE_SOURCE_CUSTOM) {
            return LocaleController.getString(R.string.FluffyMonetPaletteSourceCustom);
        }
        return LocaleController.getString(R.string.FluffyMonetPaletteSourceSystem);
    }

    private CharSequence getSeedColorValue() {
        int color = MonetThemePatch.getSeedColor();
        for (int i = 0; i < PRESET_COLORS.length; i++) {
            if (PRESET_COLORS[i] == color) {
                return LocaleController.getString(PRESET_NAMES[i]);
            }
        }
        return String.format(Locale.US, "#%06X", color & 0x00ffffff);
    }

    private static class ItemInner {
        final int viewType;
        final int id;
        final CharSequence text;
        final boolean checked;

        ItemInner(int viewType, int id, CharSequence text, boolean checked) {
            this.viewType = viewType;
            this.id = id;
            this.text = text;
            this.checked = checked;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int viewType = holder.getItemViewType();
            return viewType == VIEW_TYPE_TEXT || viewType == VIEW_TYPE_CHECK;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view;
            switch (viewType) {
                case VIEW_TYPE_HEADER:
                    view = new HeaderCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_CHECK:
                    view = new TextCheckCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case VIEW_TYPE_SHADOW:
                    view = new ShadowSectionCell(context);
                    break;
                case VIEW_TYPE_INFO:
                    view = new TextInfoPrivacyCell(context);
                    break;
                case VIEW_TYPE_TEXT:
                default:
                    view = new TextSettingsCell(context);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemInner item = items.get(position);
            switch (item.viewType) {
                case VIEW_TYPE_HEADER:
                    ((HeaderCell) holder.itemView).setText(item.text);
                    break;
                case VIEW_TYPE_CHECK:
                    ((TextCheckCell) holder.itemView).setTextAndCheck(item.text, item.checked, true);
                    break;
                case VIEW_TYPE_INFO:
                    ((TextInfoPrivacyCell) holder.itemView).setText(item.text);
                    break;
                case VIEW_TYPE_TEXT:
                    TextSettingsCell cell = (TextSettingsCell) holder.itemView;
                    if (item.id == ROW_PALETTE_SOURCE) {
                        cell.setTextAndValue(item.text, getPaletteSourceValue(), true);
                    } else if (item.id == ROW_SEED_COLOR) {
                        cell.setTextAndValue(item.text, getSeedColorValue(), true);
                    } else if (item.id == ROW_SCHEME) {
                        cell.setTextAndValue(item.text,
                                LocaleController.getString(SCHEME_NAMES[MonetThemePatch.getScheme()]), false);
                    } else if (item.id == ROW_APPLY_LIGHT) {
                        cell.setText(item.text, true);
                    } else {
                        cell.setText(item.text, false);
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
