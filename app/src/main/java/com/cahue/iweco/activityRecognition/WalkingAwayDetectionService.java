package com.cahue.iweco.activityRecognition;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service meant to detect if a user is walking for enough time.
 *
 * This is used to detect if a user is far enough from the car he just parked, so that we can set
 * a Geofence around it
 */
public class WalkingAwayDetectionService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
