package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.VideoEditedInfo;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.PhotoViewer;
import org.ushastoe.fluffy.patches.SendAsRoundVideoPatch;

import java.util.HashMap;

public final class SendAsRoundVideoHook {

    private SendAsRoundVideoHook() {
    }

    public static boolean canShowMenuItem(boolean isCurrentVideo, boolean canEdit, boolean hasTimer, int sendPhotoType,
                                          ChatActivity chatActivity, PhotoViewer.PhotoViewerProvider provider, int currentIndex) {
        return SendAsRoundVideoPatch.canShowMenuItem(isCurrentVideo, canEdit, hasTimer, sendPhotoType, chatActivity, provider, currentIndex);
    }

    public static boolean canShowAttachMenuItem(ChatActivity chatActivity, HashMap<Object, Object> selectedPhotos) {
        return SendAsRoundVideoPatch.canShowAttachMenuItem(chatActivity, selectedPhotos);
    }

    public static void markAttachRoundSend(HashMap<Object, Object> selectedPhotos) {
        SendAsRoundVideoPatch.markAttachRoundSend(selectedPhotos);
    }

    public static void beginRoundSend() {
        SendAsRoundVideoPatch.beginRoundSend();
    }

    public static void endRoundSend() {
        SendAsRoundVideoPatch.endRoundSend();
    }

    public static VideoEditedInfo onSendPressed(VideoEditedInfo info, Object entry) {
        return SendAsRoundVideoPatch.onSendPressed(info, entry);
    }

    public static void prepareMediaInfo(SendMessagesHelper.SendingMediaInfo info, VideoEditedInfo videoEditedInfo) {
        SendAsRoundVideoPatch.prepareMediaInfo(info, videoEditedInfo);
    }

    public static void applyVideoAttribute(TLRPC.TL_documentAttributeVideo attribute, VideoEditedInfo videoEditedInfo) {
        SendAsRoundVideoPatch.applyVideoAttribute(attribute, videoEditedInfo);
    }
}
