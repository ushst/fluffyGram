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
        String subtitle = LocaleController.formatDateTime(imageLocation.photo.date, true);
        if (imageLocation.photo.dc_id > 0) {
            subtitle += ", DC" + imageLocation.photo.dc_id;
        }
        return subtitle;
    }
}
