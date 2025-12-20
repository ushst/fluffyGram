package org.ushastoe.fluffy.activities.elements;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.EditTextBoldCursor;

/**
 * Helper methods for building alert dialogs that match the Fluffy design system.
 */
public final class FluffyDialogUtils {

    private FluffyDialogUtils() {
    }

    /**
     * Creates a themed {@link AlertDialog.Builder} with Fluffy specific overrides applied.
     */
    public static AlertDialog.Builder themedBuilder(Context context) {
        return themedBuilder(context, null);
    }

    /**
     * Creates a themed {@link AlertDialog.Builder} that optionally decorates the provided
     * {@link Theme.ResourcesProvider}.
     */
    public static AlertDialog.Builder themedBuilder(Context context, @Nullable Theme.ResourcesProvider baseProvider) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, new FluffyDialogResourcesProvider(baseProvider));
        builder.setAdditionalHorizontalPadding(AndroidUtilities.dp(12));
        return builder;
    }

    /**
     * Wraps the provided view with standard dialog paddings to keep consistent spacing.
     */
    public static View wrapWithStandardPadding(View view) {
        if (view == null) {
            return null;
        }
        if (view.getParent() instanceof ViewGroup) {
            ((ViewGroup) view.getParent()).removeView(view);
        }
        Context context = view.getContext();
        FrameLayout container = new FrameLayout(context);
        container.setClipToPadding(false);
        int horizontal = AndroidUtilities.dp(20);
        int top = AndroidUtilities.dp(16);
        int bottom = AndroidUtilities.dp(12);
        container.setPadding(horizontal, top, horizontal, bottom);
        container.addView(view, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return container;
    }

    /**
     * Applies theme-aware colors and backgrounds to dialog input fields.
     */
    public static void styleTextInput(EditText editText) {
        styleTextInput(editText, null);
    }

    /**
     * Applies theme-aware colors and backgrounds to dialog input fields using the provided resources provider.
     */
    public static void styleTextInput(EditText editText, @Nullable Theme.ResourcesProvider resourcesProvider) {
        if (editText == null) {
            return;
        }
        Context context = editText.getContext();
        if (context != null) {
            editText.setBackground(Theme.createEditTextDrawable(context, true));
        }

        int textColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
        int hintColor = Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourcesProvider);
        int linkColor = Theme.getColor(Theme.key_windowBackgroundWhiteLinkText, resourcesProvider);
        int cursorColor = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider);

        editText.setTextColor(textColor);
        editText.setHintTextColor(hintColor);
        editText.setLinkTextColor(linkColor);
        editText.setHighlightColor(Theme.multAlpha(cursorColor, 0.3f));

        if (editText instanceof EditTextBoldCursor) {
            EditTextBoldCursor boldCursor = (EditTextBoldCursor) editText;
            boldCursor.setCursorColor(cursorColor);
        }
    }

    /**
     * Applies final window styling tweaks when the dialog is created.
     */
    public static void applyWindowStyling(AlertDialog dialog) {
        if (dialog == null || dialog.getWindow() == null) {
            return;
        }
        dialog.getWindow().setBackgroundDrawable(Theme.createRoundRectDrawable(AndroidUtilities.dp(20), Theme.getColor(Theme.key_dialogBackground)));
        dialog.getWindow().setDimAmount(0.45f);
    }

    private static class FluffyDialogResourcesProvider implements Theme.ResourcesProvider {

        private final Theme.ResourcesProvider baseProvider;

        private FluffyDialogResourcesProvider(@Nullable Theme.ResourcesProvider baseProvider) {
            this.baseProvider = baseProvider;
        }

        @Override
        public int getColor(int key) {
            // Theme.key_* fields may not be compile-time constants in some builds,
            // so using them in switch case labels can cause "constant expression required" errors.
            // Use explicit comparisons instead.
            if (key == Theme.key_dialogBackground || key == Theme.key_dialogRoundBackground) {
                return Theme.getColor(Theme.key_windowBackgroundWhite);
            } else if (key == Theme.key_dialogTextBlack) {
                return Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
            } else if (key == Theme.key_dialogTextGray2) {
                return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
            } else if (key == Theme.key_dialogTextGray3) {
                return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3);
            } else if (key == Theme.key_dialogIcon) {
                return Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader);
            } else if (key == Theme.key_dialogTextLink) {
                return Theme.getColor(Theme.key_windowBackgroundWhiteLinkText);
            } else if (key == Theme.key_dialogButton) {
                return Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
            } else if (key == Theme.key_dialogButtonSelector) {
                return Theme.getColor(Theme.key_listSelector);
            } else if (key == Theme.key_dialogScrollGlow) {
                return Theme.getColor(Theme.key_actionBarDefault);
            } else if (key == Theme.key_dialogLineProgress) {
                return Theme.getColor(Theme.key_featuredStickers_buttonProgress);
            } else if (key == Theme.key_dialogLineProgressBackground) {
                return Theme.getColor(Theme.key_divider);
            } else {
                if (baseProvider != null) {
                    return baseProvider.getColor(key);
                }
                return Theme.getColor(key);
            }
        }
    }
}
