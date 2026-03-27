package org.ushastoe.fluffy.utils;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

public final class GoogleAiMarkdownFormatter {

    private GoogleAiMarkdownFormatter() {
    }

    public static CharSequence format(String markdown) {
        if (TextUtils.isEmpty(markdown)) {
            return "";
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(markdown);
        applyLinks(builder);
        applyDelimitedSpan(builder, "```", "```", true, new TypefaceSpan("monospace"), new BackgroundColorSpan(0x143F8AE0));
        applyDelimitedSpan(builder, "`", "`", false, new TypefaceSpan("monospace"), new BackgroundColorSpan(0x143F8AE0));
        applyDelimitedSpan(builder, "**", "**", false, new StyleSpan(Typeface.BOLD));
        applyDelimitedSpan(builder, "__", "__", false, new StyleSpan(Typeface.BOLD));
        applyDelimitedSpan(builder, "*", "*", false, new StyleSpan(Typeface.ITALIC));
        applyDelimitedSpan(builder, "_", "_", false, new StyleSpan(Typeface.ITALIC));
        return builder;
    }

    private static void applyLinks(SpannableStringBuilder builder) {
        int index = 0;
        while (index < builder.length()) {
            int openText = builder.toString().indexOf('[', index);
            if (openText < 0) {
                return;
            }
            int closeText = builder.toString().indexOf(']', openText + 1);
            if (closeText < 0 || closeText + 1 >= builder.length() || builder.charAt(closeText + 1) != '(') {
                index = openText + 1;
                continue;
            }
            int closeUrl = builder.toString().indexOf(')', closeText + 2);
            if (closeUrl < 0) {
                return;
            }
            String label = builder.subSequence(openText + 1, closeText).toString();
            String url = builder.subSequence(closeText + 2, closeUrl).toString();
            if (TextUtils.isEmpty(label) || TextUtils.isEmpty(url)) {
                index = closeUrl + 1;
                continue;
            }
            builder.replace(openText, closeUrl + 1, label);
            builder.setSpan(new URLSpan(url), openText, openText + label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = openText + label.length();
        }
    }

    private static void applyDelimitedSpan(SpannableStringBuilder builder, String open, String close, boolean trimInnerNewlines, Object... spans) {
        int index = 0;
        while (index < builder.length()) {
            int start = builder.toString().indexOf(open, index);
            if (start < 0) {
                return;
            }
            int end = builder.toString().indexOf(close, start + open.length());
            if (end < 0) {
                return;
            }
            int contentStart = start + open.length();
            int contentEnd = end;
            String text = builder.subSequence(contentStart, contentEnd).toString();
            if (trimInnerNewlines) {
                text = trimFenceEdges(text);
            }
            builder.replace(start, end + close.length(), text);
            int spanEnd = start + text.length();
            for (int i = 0; i < spans.length; i++) {
                builder.setSpan(spans[i], start, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            index = spanEnd;
        }
    }

    private static String trimFenceEdges(String value) {
        String text = value;
        if (text.startsWith("\n")) {
            text = text.substring(1);
        }
        if (text.endsWith("\n")) {
            text = text.substring(0, text.length() - 1);
        }
        int firstLineBreak = text.indexOf('\n');
        if (firstLineBreak > 0) {
            String firstLine = text.substring(0, firstLineBreak);
            if (!firstLine.contains(" ")) {
                text = text.substring(firstLineBreak + 1);
            }
        }
        return text;
    }
}
