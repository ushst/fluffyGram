package org.ushastoe.fluffy.patches;

import org.telegram.messenger.ChatObject;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.PhotoViewer;

import java.util.HashMap;

/**
 * "Send as video message": turns a regular gallery video into a round video (video note).
 *
 * The video is center-cropped to a square through {@link MediaController.CropState}
 * (non-matrix path of TextureRenderer), re-encoded at {@link #ROUND_SIZE}px and trimmed
 * to {@link #MAX_DURATION_US}. Sending then goes through the regular media pipeline,
 * which only needs round_message on the video attribute.
 */
public final class SendAsRoundVideoPatch {

    private static final int ROUND_SIZE = 384;
    private static final int ROUND_BITRATE = 1_000_000;
    private static final int ROUND_AUDIO_BITRATE = 64_000;
    private static final long MAX_DURATION_US = 60_000_000L;

    private static final long PENDING_PATH_TTL_MS = 60_000L;

    private static boolean pendingRound;
    // Videos picked from the attach alert menu: path -> time marked. prepareSendingMedia builds
    // their VideoEditedInfo on a background thread, so they are converted there.
    private static final HashMap<String, Long> pendingRoundPaths = new HashMap<>();

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

    public static void markAttachRoundSend(HashMap<Object, Object> selectedPhotos) {
        MediaController.PhotoEntry entry = getSingleSelectedVideo(selectedPhotos);
        if (entry == null) {
            return;
        }
        synchronized (pendingRoundPaths) {
            pendingRoundPaths.put(entry.path, System.currentTimeMillis());
        }
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

    private static boolean consumePendingPath(String path) {
        if (path == null) {
            return false;
        }
        synchronized (pendingRoundPaths) {
            Long markedAt = pendingRoundPaths.remove(path);
            return markedAt != null && System.currentTimeMillis() - markedAt < PENDING_PATH_TTL_MS;
        }
    }

    public static void beginRoundSend() {
        pendingRound = true;
    }

    public static void endRoundSend() {
        pendingRound = false;
    }

    public static VideoEditedInfo onSendPressed(VideoEditedInfo info, Object entry) {
        if (!pendingRound) {
            return info;
        }
        pendingRound = false;
        if (info == null || info.isPhoto || info.originalWidth <= 0 || info.originalHeight <= 0) {
            return info;
        }
        convertToRound(info);
        if (entry instanceof MediaController.PhotoEntry) {
            MediaController.PhotoEntry photoEntry = (MediaController.PhotoEntry) entry;
            photoEntry.caption = null;
            photoEntry.entities = null;
            photoEntry.hasSpoiler = false;
        }
        return info;
    }

    private static void convertToRound(VideoEditedInfo info) {
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
        cropState.transformWidth = ROUND_SIZE;
        cropState.transformHeight = ROUND_SIZE;
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

        info.bitrate = ROUND_BITRATE;
        info.estimatedSize = Math.max(1, (long) ((ROUND_BITRATE + ROUND_AUDIO_BITRATE) / 8.0 * durationMs / 1000.0));
    }

    public static boolean isRound(VideoEditedInfo info) {
        return info != null && info.roundVideo;
    }

    public static void prepareMediaInfo(SendMessagesHelper.SendingMediaInfo info, VideoEditedInfo videoEditedInfo) {
        if (info == null) {
            return;
        }
        if (consumePendingPath(info.path) && videoEditedInfo != null && !videoEditedInfo.isPhoto
                && videoEditedInfo.originalWidth > 0 && videoEditedInfo.originalHeight > 0) {
            convertToRound(videoEditedInfo);
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
