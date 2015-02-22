package com.bahpps.cahue.activityRecognition;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service in charge of detecting when a user is approaching his car.
 *
 */
public class ApproachingCarService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
