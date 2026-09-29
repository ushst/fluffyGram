package org.ushastoe.fluffy.patches;

import android.content.Context;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.PhotoViewer;

import java.util.HashMap;

/**
 * "Send as video message": turns a regular gallery video into a round video (video note).
 *
 * The video is center-cropped to a square through {@link MediaController.CropState}
 * (non-matrix path of TextureRenderer), re-encoded at the chosen quality preset and trimmed
 * to {@link #MAX_DURATION_US}. Sending then goes through the regular media pipeline,
 * which only needs round_message on the video attribute.
 */
public final class SendAsRoundVideoPatch {

    public static final int QUALITY_LOW = 0;
    public static final int QUALITY_MEDIUM = 1;
    public static final int QUALITY_HIGH = 2;

    // Indexed by QUALITY_*. Sides are multiples of 16; 640 is the largest video note size clients expect.
    private static final int[] ROUND_SIZES = {384, 512, 640};
    private static final int[] ROUND_BITRATES = {1_000_000, 2_000_000, 3_500_000};
    private static final int ROUND_AUDIO_BITRATE = 64_000;
    private static final long MAX_DURATION_US = 60_000_000L;

    private static final long PENDING_PATH_TTL_MS = 60_000L;

    private static int pendingQuality = -1;
    // Videos picked from the attach alert menu: path -> {time marked, quality}. prepareSendingMedia
    // builds their VideoEditedInfo on a background thread, so they are converted there.
    private static final HashMap<String, long[]> pendingRoundPaths = new HashMap<>();

    private SendAsRoundVideoPatch() {
    }

    public static boolean canShowMenuItem(boolean isCurrentVideo, boolean canEdit, boolean hasTimer, int sendPhotoType,
                                          ChatActivity chatActivity, PhotoViewer.PhotoViewerProvider provider, int currentIndex) {
        if (!isCurrentVideo || canEdit || hasTimer || sendPhotoType != 0 || chatActivity == null || provider == null) {
            return false;
        }
        if (chatActivity.isEditingMessageMedia()) {
            return false;
        }
        // The round flag is attached to the currently shown entry only, so nothing else may be sent with it.
        int selectedCount = provider.getSelectedCount();
        if (selectedCount > 1 || selectedCount == 1 && !provider.isPhotoChecked(currentIndex)) {
            return false;
        }
        TLRPC.Chat chat = chatActivity.getCurrentChat();
        return chat == null || ChatObject.canSendRoundVideo(chat);
    }

    public static boolean canShowAttachMenuItem(ChatActivity chatActivity, HashMap<Object, Object> selectedPhotos) {
        if (chatActivity == null || chatActivity.isEditingMessageMedia() || getSingleSelectedVideo(selectedPhotos) == null) {
            return false;
        }
        TLRPC.Chat chat = chatActivity.getCurrentChat();
        return chat == null || ChatObject.canSendRoundVideo(chat);
    }

    public static void showQualityPicker(Context context, Theme.ResourcesProvider resourcesProvider, Utilities.Callback<Integer> onPicked) {
        if (context == null || onPicked == null) {
            return;
        }
        CharSequence[] items = new CharSequence[]{
                LocaleController.formatString(R.string.FluffyRoundVideoQualityLow, ROUND_SIZES[QUALITY_LOW]),
                LocaleController.formatString(R.string.FluffyRoundVideoQualityMedium, ROUND_SIZES[QUALITY_MEDIUM]),
                LocaleController.formatString(R.string.FluffyRoundVideoQualityHigh, ROUND_SIZES[QUALITY_HIGH])
        };
        BottomSheet.Builder builder = new BottomSheet.Builder(context, false, resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.FluffyRoundVideoQuality), true);
        builder.setItems(items, (dialog, which) -> onPicked.run(which));
        builder.show();
    }

    public static void markAttachRoundSend(HashMap<Object, Object> selectedPhotos, int quality) {
        MediaController.PhotoEntry entry = getSingleSelectedVideo(selectedPhotos);
        if (entry == null) {
            return;
        }
        synchronized (pendingRoundPaths) {
            pendingRoundPaths.put(entry.path, new long[]{System.currentTimeMillis(), clampQuality(quality)});
        }
    }

    private static int clampQuality(int quality) {
        return Math.max(QUALITY_LOW, Math.min(QUALITY_HIGH, quality));
    }

    private static MediaController.PhotoEntry getSingleSelectedVideo(HashMap<Object, Object> selectedPhotos) {
        if (selectedPhotos == null || selectedPhotos.size() != 1) {
            return null;
        }
        Object object = selectedPhotos.values().iterator().next();
        if (!(object instanceof MediaController.PhotoEntry)) {
            return null;
        }
        MediaController.PhotoEntry entry = (MediaController.PhotoEntry) object;
        if (!entry.isVideo || entry.path == null || entry.ttl != 0) {
            return null;
        }
        return entry;
    }

    /** @return quality marked for this path, or -1 when the video is not a pending round send. */
    private static int consumePendingPath(String path) {
        if (path == null) {
            return -1;
        }
        synchronized (pendingRoundPaths) {
            long[] pending = pendingRoundPaths.remove(path);
            if (pending == null || System.currentTimeMillis() - pending[0] >= PENDING_PATH_TTL_MS) {
                return -1;
            }
            return (int) pending[1];
        }
    }

    public static void beginRoundSend(int quality) {
        pendingQuality = clampQuality(quality);
    }

    public static void endRoundSend() {
        pendingQuality = -1;
    }

    public static VideoEditedInfo onSendPressed(VideoEditedInfo info, Object entry) {
        int quality = pendingQuality;
        if (quality < 0) {
            return info;
        }
        pendingQuality = -1;
        if (info == null || info.isPhoto || info.originalWidth <= 0 || info.originalHeight <= 0) {
            return info;
        }
        convertToRound(info, quality);
        if (entry instanceof MediaController.PhotoEntry) {
            MediaController.PhotoEntry photoEntry = (MediaController.PhotoEntry) entry;
            photoEntry.caption = null;
            photoEntry.entities = null;
            photoEntry.hasSpoiler = false;
        }
        return info;
    }

    private static void convertToRound(VideoEditedInfo info, int quality) {
        int size = ROUND_SIZES[quality];
        int bitrate = ROUND_BITRATES[quality];
        info.roundVideo = true;
        // Muted video is sent as a GIF-like document, which breaks video notes.
        info.muted = false;

        if (info.resultWidth <= 0 || info.resultHeight <= 0) {
            info.resultWidth = info.originalWidth;
            info.resultHeight = info.originalHeight;
        }

        // TextureRenderer works in the final (rotated) orientation: MediaController swaps
        // resultWidth/resultHeight for 90/270 before handing them to the renderer.
        boolean swap = info.rotationValue == 90 || info.rotationValue == 270;
        float w = swap ? info.resultHeight : info.resultWidth;
        float h = swap ? info.resultWidth : info.resultHeight;

        MediaController.CropState cropState = info.cropState != null ? info.cropState.clone() : new MediaController.CropState();
        cropState.useMatrix = null;
        float cropW = w * cropState.cropPw;
        float cropH = h * cropState.cropPh;
        if (cropW > cropH) {
            cropState.cropPw = cropH / w;
        } else if (cropH > cropW) {
            cropState.cropPh = cropW / h;
        }
        cropState.transformWidth = size;
        cropState.transformHeight = size;
        info.cropState = cropState;

        long originalDurationUs = info.originalDuration;
        long start = Math.max(0, info.startTime);
        long end = info.endTime > 0 ? info.endTime : originalDurationUs;
        if (end - start > MAX_DURATION_US) {
            end = start + MAX_DURATION_US;
            info.endTime = end;
            if (info.startTime < 0) {
                info.startTime = 0;
            }
        }
        long durationMs = Math.max(1, (end - start) / 1000);
        info.estimatedDuration = durationMs;

        info.bitrate = bitrate;
        info.estimatedSize = Math.max(1, (long) ((bitrate + ROUND_AUDIO_BITRATE) / 8.0 * durationMs / 1000.0));
    }

    public static boolean isRound(VideoEditedInfo info) {
        return info != null && info.roundVideo;
    }

    public static void prepareMediaInfo(SendMessagesHelper.SendingMediaInfo info, VideoEditedInfo videoEditedInfo) {
        if (info == null) {
            return;
        }
        int quality = consumePendingPath(info.path);
        if (quality >= 0 && videoEditedInfo != null && !videoEditedInfo.isPhoto
                && videoEditedInfo.originalWidth > 0 && videoEditedInfo.originalHeight > 0) {
            convertToRound(videoEditedInfo, quality);
        }
        if (!isRound(videoEditedInfo)) {
            return;
        }
        info.caption = null;
        info.entities = null;
        info.hasMediaSpoilers = false;
    }

    public static void applyVideoAttribute(TLRPC.TL_documentAttributeVideo attribute, VideoEditedInfo videoEditedInfo) {
        if (attribute != null && isRound(videoEditedInfo)) {
            attribute.round_message = true;
        }
    }
}
