package org.ushastoe.fluffy.patches;

import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;

public final class ProfilePhotoDatePatch {

    private ProfilePhotoDatePatch() {
    }

    public static CharSequence getPhotoViewerSubtitle(ImageLocation imageLocation) {
        if (imageLocation == null || imageLocation.photo == null || imageLocation.photo.date <= 0) {
            return "";
        }
        return LocaleController.formatDateTime(imageLocation.photo.date, true);
    }
}
