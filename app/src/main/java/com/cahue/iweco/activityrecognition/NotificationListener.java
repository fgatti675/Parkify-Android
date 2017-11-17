package com.cahue.iweco.activityrecognition;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.cahue.iweco.locationservices.PossibleParkedCarReceiver;
import com.cahue.iweco.util.Tracking;

/**
 * Created by f.gatti.gomez on 17/04/16.
 */
public class NotificationListener extends NotificationListenerService {

    private static final String TAG = NotificationListener.class.getSimpleName();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        Log.i(TAG, "onNotificationRemoved: " + sbn.toString());
        if (sbn.getId() == PossibleParkedCarReceiver.NOTIFICATION_ID) {
            Tracking.sendEvent(Tracking.CATEGORY_NOTIFICATION_ACT_RECOG, Tracking.ACTION_NOTIFICATION_REMOVED);
        }
    }
}

