package org.ushastoe.fluffy.patches;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.SparseIntArray;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.CompoundEmoji;
import org.telegram.messenger.AndroidUtilities;
import org.ushastoe.fluffy.patches.AppearanceSettingsPatch;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public final class EmojiAssetPatch {

    private static SparseIntArray emojiAlphaMasks;
    private static boolean metadataChecked;
    private static boolean metadataAvailable;

    private EmojiAssetPatch() {
    }

    public static String mapAssetPath(String path) {
        if (path == null || !path.startsWith("emoji/")) {
            return path;
        }
        if (AppearanceSettingsPatch.getEmojiSet() == AppearanceSettingsPatch.EMOJI_SET_NOTO) {
            return "emoji_noto/" + path.substring("emoji/".length());
        }
        return path;
    }

    public static Bitmap applyOptionalAlphaMask(Bitmap bitmap, byte page, short page2) {
        if (bitmap == null) {
            return null;
        }
        ensureAlphaMaskMetadataLoaded();
        if (!metadataAvailable || emojiAlphaMasks == null) {
            return bitmap;
        }

        int maskIndex = emojiAlphaMasks.get(page * 4096 + page2, -1);
        if (maskIndex == -1) {
            return bitmap;
        }

        Bitmap alphaBitmap = loadEmojiMask(maskIndex);
        if (alphaBitmap == null) {
            return bitmap;
        }

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] rgbPixels = new int[width * height];
            int[] alphaPixels = new int[width * height];

            bitmap.getPixels(rgbPixels, 0, width, 0, 0, width, height);
            alphaBitmap.getPixels(alphaPixels, 0, width, 0, 0, width, height);
            alphaBitmap.recycle();

            for (int i = 0; i < rgbPixels.length; i++) {
                int color = rgbPixels[i];
                rgbPixels[i] = (color & 0x00FFFFFF) | ((alphaPixels[i] & 0xFF) << 24);
            }

            bitmap.recycle();
            Bitmap maskedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            maskedBitmap.setPixels(rgbPixels, 0, width, 0, 0, width, height);
            return maskedBitmap;
        } catch (Exception e) {
            FileLog.e(e);
            return bitmap;
        }
    }

    private static void ensureAlphaMaskMetadataLoaded() {
        if (metadataChecked) {
            return;
        }
        metadataChecked = true;
        emojiAlphaMasks = loadEmojiAlphaMasks();
        metadataAvailable = emojiAlphaMasks != null;
    }

    private static SparseIntArray loadEmojiAlphaMasks() {
        try (InputStream inputStream = ApplicationLoader.applicationContext.getAssets().open("emoji/metadata.bin")) {
            ArrayList<byte[]> chunks = new ArrayList<>();
            int totalSize = 0;
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                byte[] copy = new byte[read];
                System.arraycopy(buffer, 0, copy, 0, read);
                chunks.add(copy);
                totalSize += read;
            }

            byte[] all = new byte[totalSize];
            int position = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, all, position, chunk.length);
                position += chunk.length;
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(all).order(ByteOrder.LITTLE_ENDIAN);
            int pairs = totalSize / 4;
            SparseIntArray map = new SparseIntArray(pairs);
            for (int i = 0; i < pairs; i++) {
                int emojiIndex = byteBuffer.getShort() & 0xFFFF;
                int maskId = byteBuffer.getShort() & 0xFFFF;
                map.put(emojiIndex, maskId);
            }
            return map;
        } catch (Exception ignore) {
            return null;
        }
    }

    private static Bitmap loadEmojiMask(int maskIndex) {
        try (InputStream inputStream = ApplicationLoader.applicationContext.getAssets().open("emoji/masks/" + maskIndex + ".png")) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception ignore) {
            return null;
        }
    }

    public static void onEmojiSetChanged() {
        onEmojiSetChanged(null);
    }

    public static void onEmojiSetChanged(Bitmap[][] emojiBitmaps) {
        clearEmojiBitmaps(emojiBitmaps);
        CompoundEmoji.clearFluffyEmojiCache();
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.emojiLoaded);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.reloadInterface);
        });
    }

    private static void clearEmojiBitmaps(Bitmap[][] emojiBitmaps) {
        if (emojiBitmaps == null) {
            return;
        }
        for (Bitmap[] pageBitmaps : emojiBitmaps) {
            if (pageBitmaps == null) {
                continue;
            }
            for (int i = 0; i < pageBitmaps.length; i++) {
                Bitmap bitmap = pageBitmaps[i];
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                pageBitmaps[i] = null;
            }
        }
    }
}
