package org.ushastoe.fluffy.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.telegram.messenger.ApplicationLoader;
import org.ushastoe.fluffy.hooks.NotificationReactionHook;

public class NotificationReactionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationLoader.postInitApplication();
        if (intent == null) {
            return;
        }
        NotificationReactionHook.handleHeartReactionIntent(
                intent.getLongExtra("dialog_id", 0),
                intent.getIntExtra("max_id", 0),
                intent.getLongExtra("topic_id", 0),
                intent.getIntExtra("currentAccount", 0)
        );
    }
}
