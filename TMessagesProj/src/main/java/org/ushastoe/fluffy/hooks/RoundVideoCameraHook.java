package org.ushastoe.fluffy.hooks;

import android.content.Context;
import android.graphics.Canvas;

import org.ushastoe.fluffy.patches.RoundVideoCameraPatch;

public final class RoundVideoCameraHook {

    private RoundVideoCameraHook() {
    }

    public static boolean resolveInitialFrontCamera(boolean fromPaused, boolean currentFrontface) {
        return RoundVideoCameraPatch.resolveInitialFrontCamera(fromPaused, currentFrontface);
    }

    public static void onRoundVideoCameraSwitched(boolean frontface) {
        RoundVideoCameraPatch.setCurrentFrontCamera(frontface);
    }

    public static void drawVideoCameraBadge(Canvas canvas, boolean videoMode, int width, int height) {
        RoundVideoCameraPatch.drawVideoCameraBadge(canvas, videoMode, width, height);
    }

    public static CharSequence getRecordButtonContentDescription(Context context, boolean videoMode) {
        return RoundVideoCameraPatch.getRecordButtonContentDescription(context, videoMode);
    }
}
