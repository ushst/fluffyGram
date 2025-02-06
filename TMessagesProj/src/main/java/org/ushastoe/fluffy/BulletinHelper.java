package org.ushastoe.fluffy;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;

import static org.telegram.messenger.LocaleController.getString;
import static org.ushastoe.fluffy.fluffyConfig.frontCamera;

public class BulletinHelper {

    public static Bulletin currentBulletin;

    public static void showFrontCameraNotification(@NonNull BaseFragment fragment) {
        try {
            Drawable drawable = fragment.getContext().getDrawable(R.drawable.menu_camera_retake);
            String text = frontCamera ? "Front camera enabled" : "Back camera enabled";
            currentBulletin = BulletinFactory.of(fragment).createSimpleBulletin(
                    drawable,
                    text,
                    frontCamera ? "Back" : "Front",
                    () -> {
                        fluffyConfig.cameraSwitch();
                        BulletinHelper.showFrontCameraNotification(fragment);
                    }
            );
            currentBulletin.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showRestartNotification(@NonNull BaseFragment fragment) {
        try {
            Drawable drawable = fragment.getContext().getDrawable(R.drawable.filled_info);
            String text = getString(R.string.restartAlarm);
            currentBulletin = BulletinFactory.of(fragment).createSimpleBulletin(
                    drawable,
                    text
            );
            currentBulletin.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void hideFrontCameraNotification() {
        if (currentBulletin != null) {
            currentBulletin.hide();
            currentBulletin = null;
        }
    }

}
