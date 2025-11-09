package org.ushastoe.fluffy.activities.elements;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

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
            switch (key) {
                case Theme.key_dialogBackground:
                case Theme.key_dialogRoundBackground:
                    return Theme.getColor(Theme.key_windowBackgroundWhite);
                case Theme.key_dialogTextBlack:
                    return Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);
                case Theme.key_dialogTextGray2:
                    return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2);
                case Theme.key_dialogTextGray3:
                    return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3);
                case Theme.key_dialogIcon:
                    return Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader);
                case Theme.key_dialogTextLink:
                    return Theme.getColor(Theme.key_windowBackgroundWhiteLinkText);
                case Theme.key_dialogButton:
                    return Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4);
                case Theme.key_dialogButtonSelector:
                    return Theme.getColor(Theme.key_listSelector);
                case Theme.key_dialogScrollGlow:
                    return Theme.getColor(Theme.key_actionBarDefault);
                case Theme.key_dialogLineProgress:
                    return Theme.getColor(Theme.key_featuredStickers_buttonProgress);
                case Theme.key_dialogLineProgressBackground:
                    return Theme.getColor(Theme.key_divider);
                default:
                    if (baseProvider != null) {
                        return baseProvider.getColor(key);
                    }
                    return Theme.getColor(key);
            }
        }
    }
}
