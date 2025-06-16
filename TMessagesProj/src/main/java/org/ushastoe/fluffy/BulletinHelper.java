package org.ushastoe.fluffy;
import org.telegram.messenger.R;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
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

    /**
     * Displays a simple bulletin with a title and subtitle using BulletinFactory.
     *
     * @param fragment The BaseFragment to display the bulletin on.
     * @param title    The title text for the bulletin.
     * @param subtitle The subtitle text for the bulletin (can be null).
     */
    public static void showSimpleBulletin(@NonNull BaseFragment fragment, CharSequence title, CharSequence subtitle) {
        if (fragment.getParentActivity() == null) {
            return;
        }

        try {
            // BulletinFactory.createSimpleBulletin(icon, title, subtitle)
            currentBulletin = BulletinFactory.of(fragment).createSimpleBulletin(
                    null, // В этом случае иконка не нужна для простого сообщения
                    title,
                    subtitle
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