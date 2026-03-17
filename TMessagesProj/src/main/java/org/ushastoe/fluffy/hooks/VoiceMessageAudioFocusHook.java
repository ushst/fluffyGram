package org.ushastoe.fluffy.hooks;

import org.ushastoe.fluffy.patches.VoiceMessageAudioFocusPatch;

public final class VoiceMessageAudioFocusHook {

    private VoiceMessageAudioFocusHook() {
    }

    public static int resolvePlaybackFocusGain(boolean isVoiceMessage, int defaultFocusGain) {
        return VoiceMessageAudioFocusPatch.resolvePlaybackFocusGain(isVoiceMessage, defaultFocusGain);
    }

    public static boolean shouldAbandonFocusOnPause(boolean isVoiceMessage, boolean resumeOnFocusGainPending) {
        return VoiceMessageAudioFocusPatch.shouldAbandonFocusOnPause(isVoiceMessage, resumeOnFocusGainPending);
    }
}
