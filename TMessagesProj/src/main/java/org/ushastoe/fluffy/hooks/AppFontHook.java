package org.ushastoe.fluffy.hooks;

import android.graphics.Typeface;
import android.text.TextPaint;
import android.widget.TextView;

import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.TextStyleSpan;
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

    public static Typeface getRegularTypefaceOverride() {
        return AppFontPatch.getRegularTypefaceOverride();
    }

    public static void initializeGlobalOverride() {
        AppFontPatch.initializeGlobalOverride();
    }

    public static void onFontChanged() {
        AppFontPatch.onFontChanged();
    }

    public static void applyToTextSettingsCell(TextView textView, AnimatedTextView valueTextView) {
        AppFontPatch.applyToTextSettingsCell(textView, valueTextView);
    }

    public static void applyToAnimatedTextView(AnimatedTextView textView) {
        AppFontPatch.applyToAnimatedTextView(textView);
    }

    public static void applyToCommonMessagePaints(TextPaint... textPaints) {
        AppFontPatch.applyToCommonMessagePaints(textPaints);
    }

    public static void applyToDialogMessagePaints(TextPaint[]... textPaintGroups) {
        AppFontPatch.applyToDialogMessagePaints(textPaintGroups);
    }

    public static void applyToRegularPaints(TextPaint... textPaints) {
        AppFontPatch.applyToRegularPaints(textPaints);
    }

    public static void applyToBoldPaints(TextPaint... textPaints) {
        AppFontPatch.applyToBoldPaints(textPaints);
    }

    public static void applyToTextCheckCell(TextView textView, TextView valueTextView) {
        AppFontPatch.applyToTextCheckCell(textView, valueTextView);
    }

    public static void applyToSimpleTextView(SimpleTextView textView) {
        AppFontPatch.applyToSimpleTextView(textView);
    }

    public static void applyToTextView(TextView textView) {
        AppFontPatch.applyToTextView(textView);
    }

    public static void applyBoldToTextView(TextView textView) {
        AppFontPatch.applyBoldToTextView(textView);
    }

    public static void applyCodeBackground(TextPaint textPaint, byte type) {
        AppFontPatch.applyCodeBackground(textPaint, type);
    }

    public static Object createInlineCodeSpan(CharSequence message, int start, int end, byte type, TextStyleSpan.TextStyleRun run) {
        return AppFontPatch.createInlineCodeSpan(message, start, end, type, run);
    }
}
