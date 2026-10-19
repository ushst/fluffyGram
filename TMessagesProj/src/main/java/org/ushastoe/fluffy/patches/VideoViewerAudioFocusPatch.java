package org.ushastoe.fluffy.patches;

import org.telegram.ui.Components.VideoPlayer;

/** Gives fullscreen video playback the player's native audio-focus lifecycle. */
public final class VideoViewerAudioFocusPatch {
    private VideoViewerAudioFocusPatch() {
    }

    public static void apply(VideoPlayer player) {
        if (player != null) {
            player.handleAudioFocus(true);
        }
    }
}
