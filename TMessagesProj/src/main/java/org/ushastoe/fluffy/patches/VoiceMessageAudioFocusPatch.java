package org.ushastoe.fluffy.patches;

import android.media.AudioManager;

public final class VoiceMessageAudioFocusPatch {

    private VoiceMessageAudioFocusPatch() {
    }

    public static int resolvePlaybackFocusGain(boolean isVoiceMessage, int defaultFocusGain) {
        if (isVoiceMessage) {
            return AudioManager.AUDIOFOCUS_GAIN_TRANSIENT;
        }
        return defaultFocusGain;
    }

    public static boolean shouldAbandonFocusOnPause(boolean isVoiceMessage, boolean resumeOnFocusGainPending) {
        return isVoiceMessage && !resumeOnFocusGainPending;
    }
}
