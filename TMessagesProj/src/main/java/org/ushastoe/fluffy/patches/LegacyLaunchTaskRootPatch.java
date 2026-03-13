package org.ushastoe.fluffy.patches;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;

import org.telegram.ui.LaunchActivity;

import java.util.List;

public final class LegacyLaunchTaskRootPatch {

    private static final String LEGACY_ROOT_CLASS = "org.ushastoe.fluffy.ui.FluffyDefaultLaunchActivity";
    private static final String EXTRA_MIGRATED = "fluffy_legacy_task_root_migrated";

    private LegacyLaunchTaskRootPatch() {
    }

    public static boolean relaunchIfNeeded(LaunchActivity activity, Intent currentIntent) {
        if (activity == null || currentIntent == null) {
            return false;
        }
        if (currentIntent.getBooleanExtra(EXTRA_MIGRATED, false)) {
            return false;
        }
        if (!hasLegacyTaskRoot(activity)) {
            return false;
        }

        Intent relaunchIntent = new Intent(currentIntent);
        relaunchIntent.setComponent(new ComponentName(activity, LaunchActivity.class));
        relaunchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        relaunchIntent.putExtra(EXTRA_MIGRATED, true);
        activity.startActivity(relaunchIntent);
        activity.finish();
        activity.overridePendingTransition(0, 0);
        return true;
    }

    private static boolean hasLegacyTaskRoot(LaunchActivity activity) {
        ActivityManager activityManager = activity.getSystemService(ActivityManager.class);
        if (activityManager == null) {
            return false;
        }
        List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        if (appTasks == null || appTasks.isEmpty()) {
            return false;
        }
        int currentTaskId = activity.getTaskId();
        for (ActivityManager.AppTask appTask : appTasks) {
            ActivityManager.RecentTaskInfo taskInfo = appTask.getTaskInfo();
            if (taskInfo == null || taskInfo.id != currentTaskId) {
                continue;
            }
            if (isLegacyComponent(taskInfo.baseActivity) || isLegacyComponent(taskInfo.topActivity)) {
                return true;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && isLegacyComponent(taskInfo.origActivity)) {
                return true;
            }
            return false;
        }
        return false;
    }

    private static boolean isLegacyComponent(ComponentName componentName) {
        return componentName != null && LEGACY_ROOT_CLASS.equals(componentName.getClassName());
    }
}
