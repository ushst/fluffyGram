package org.ushastoe.fluffy.smartreply;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Port of HF {@code BertTokenizer} with do_lower_case=false, strip_accents=None. */
final class WordPieceTokenizer {

    private static final int MAX_WORD_CHARS = 100;

    private final HashMap<String, Integer> vocab = new HashMap<>(120_000);
    final int clsId;
    final int sepId;
    private final int unkId;

    WordPieceTokenizer(File vocabFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(vocabFile), StandardCharsets.UTF_8))) {
            String line;
            int id = 0;
            while ((line = reader.readLine()) != null) {
                vocab.put(line, id++);
            }
        }
        clsId = require("[CLS]");
        sepId = require("[SEP]");
        unkId = require("[UNK]");
    }

    private int require(String token) throws IOException {
        Integer id = vocab.get(token);
        if (id == null) {
            throw new IOException("vocab misses " + token);
        }
        return id;
    }

    /** [CLS] tokens… [SEP], truncated to {@code maxLen}. */
    int[] encode(String text, int maxLen) {
        ArrayList<Integer> ids = new ArrayList<>();
        ids.add(clsId);
        for (String word : basicTokenize(text)) {
            wordPiece(word, ids);
            if (ids.size() >= maxLen - 1) {
                break;
            }
        }
        int n = Math.min(ids.size(), maxLen - 1);
        int[] out = new int[n + 1];
        for (int i = 0; i < n; i++) {
            out[i] = ids.get(i);
        }
        out[n] = sepId;
        return out;
    }

    private void wordPiece(String word, List<Integer> out) {
        int[] cps = codePoints(word);
        if (cps.length > MAX_WORD_CHARS) {
            out.add(unkId);
            return;
        }
        int mark = out.size();
        int start = 0;
        while (start < cps.length) {
            int end = cps.length;
            Integer found = null;
            while (start < end) {
                String sub = new String(cps, start, end - start);
                found = vocab.get(start > 0 ? "##" + sub : sub);
                if (found != null) {
                    break;
                }
                end--;
            }
            if (found == null) {
                while (out.size() > mark) {
                    out.remove(out.size() - 1);
                }
                out.add(unkId);
                return;
            }
            out.add(found);
            start = end;
        }
    }

    private static List<String> basicTokenize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC);
        StringBuilder cleaned = new StringBuilder(normalized.length() + 8);
        for (int cp : codePoints(normalized)) {
            if (cp == 0 || cp == 0xFFFD) {
                continue;
            }
            int type = Character.getType(cp);
            if (cp == ' ' || cp == '\t' || cp == '\n' || cp == '\r' || type == Character.SPACE_SEPARATOR) {
                cleaned.append(' ');
            } else if (type == Character.CONTROL || type == Character.FORMAT) {
                // dropped, as in HF _clean_text
            } else if (isCjk(cp)) {
                cleaned.append(' ').appendCodePoint(cp).append(' ');
            } else {
                cleaned.appendCodePoint(cp);
            }
        }

        ArrayList<String> words = new ArrayList<>();
        for (String chunk : cleaned.toString().trim().split(" +")) {
            if (chunk.isEmpty()) {
                continue;
            }
            StringBuilder cur = new StringBuilder();
            int i = 0;
            while (i < chunk.length()) {
                int cp = chunk.codePointAt(i);
                i += Character.charCount(cp);
                if (isPunctuation(cp)) {
                    if (cur.length() > 0) {
                        words.add(cur.toString());
                        cur.setLength(0);
                    }
                    words.add(new String(Character.toChars(cp)));
                } else {
                    cur.appendCodePoint(cp);
                }
            }
            if (cur.length() > 0) {
                words.add(cur.toString());
            }
        }
        return words;
    }

    private static int[] codePoints(String s) {
        int[] out = new int[s.codePointCount(0, s.length())];
        for (int i = 0, n = 0; i < s.length(); n++) {
            int cp = s.codePointAt(i);
            out[n] = cp;
            i += Character.charCount(cp);
        }
        return out;
    }

    private static boolean isPunctuation(int cp) {
        if ((cp >= 33 && cp <= 47) || (cp >= 58 && cp <= 64) || (cp >= 91 && cp <= 96) || (cp >= 123 && cp <= 126)) {
            return true;
        }
        switch (Character.getType(cp)) {
            case Character.CONNECTOR_PUNCTUATION:
            case Character.DASH_PUNCTUATION:
            case Character.START_PUNCTUATION:
            case Character.END_PUNCTUATION:
            case Character.INITIAL_QUOTE_PUNCTUATION:
            case Character.FINAL_QUOTE_PUNCTUATION:
            case Character.OTHER_PUNCTUATION:
                return true;
            default:
                return false;
        }
    }

    private static boolean isCjk(int cp) {
        return (cp >= 0x4E00 && cp <= 0x9FFF) || (cp >= 0x3400 && cp <= 0x4DBF)
                || (cp >= 0x20000 && cp <= 0x2A6DF) || (cp >= 0x2A700 && cp <= 0x2B73F)
                || (cp >= 0x2B740 && cp <= 0x2B81F) || (cp >= 0x2B820 && cp <= 0x2CEAF)
                || (cp >= 0xF900 && cp <= 0xFAFF) || (cp >= 0x2F800 && cp <= 0x2FA1F);
    }
}
