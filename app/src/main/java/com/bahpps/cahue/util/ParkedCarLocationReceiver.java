package com.bahpps.cahue.util;

import android.content.Context;
import android.location.Location;
import android.util.Log;

/**
 * This class is in charge of receiving location updates, after and store it as the cars position.
 *
 * @author Francesco
 */
public class ParkedCarLocationReceiver extends LocationPollerService {

    private final static String TAG = "ParkedCarPositionReceiver";

    @Override
    public void onLocationPolled(Context context, Location location) {
        Log.i(TAG, "Received : " + location);
        CarLocationManager.saveLocation(context, location);
    }


}
