package org.ushastoe.fluffy.utils;

import org.telegram.messenger.FileLog;

public final class FluffyPatchUtils {
    
    private FluffyPatchUtils() {}

    public static boolean applyPatch(String oldPath, String newPath, String patchPath) {
        try {
            return applyPatchNative(oldPath, newPath, patchPath) == 0;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        }
    }

    private native static int applyPatchNative(String oldPath, String newPath, String patchPath);
}
