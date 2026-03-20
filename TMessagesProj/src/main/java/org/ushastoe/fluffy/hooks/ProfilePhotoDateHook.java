package org.ushastoe.fluffy.hooks;

import org.telegram.messenger.ImageLocation;
import org.ushastoe.fluffy.patches.ProfilePhotoDatePatch;

public final class ProfilePhotoDateHook {

    private ProfilePhotoDateHook() {
    }

    public static CharSequence getPhotoViewerSubtitle(ImageLocation imageLocation) {
        return ProfilePhotoDatePatch.getPhotoViewerSubtitle(imageLocation);
    }
}
