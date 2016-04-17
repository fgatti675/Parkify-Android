package com.cahue.iweco;

import android.annotation.TargetApi;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.cahue.iweco.util.Tracking;

/**
 * Created by f.gatti.gomez on 17/04/16.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationListener extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Tracking.sendEvent(Tracking.CATEGORY_NOTIFICATION_ACT_RECOG, Tracking.ACTION_REMOVED);
    }
}

