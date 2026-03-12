package org.ushastoe.fluffy.hooks;

import android.graphics.Typeface;
import android.widget.TextView;

import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.AnimatedTextView;
import org.ushastoe.fluffy.patches.AppFontPatch;

public final class AppFontHook {

    private AppFontHook() {
    }

    public static Typeface getTypefaceOverride(String assetPath) {
        return AppFontPatch.getTypefaceOverride(assetPath);
    }

    public static Typeface getBoldTypefaceOverride() {
        return AppFontPatch.getBoldTypefaceOverride();
    }

    public static void applyToTextSettingsCell(TextView textView, AnimatedTextView valueTextView) {
        AppFontPatch.applyToTextSettingsCell(textView, valueTextView);
    }

    public static void applyToAnimatedTextView(AnimatedTextView textView) {
        AppFontPatch.applyToAnimatedTextView(textView);
    }

    public static void applyToTextCheckCell(TextView textView, TextView valueTextView) {
        AppFontPatch.applyToTextCheckCell(textView, valueTextView);
    }

    public static void applyToSimpleTextView(SimpleTextView textView) {
        AppFontPatch.applyToSimpleTextView(textView);
    }
}
