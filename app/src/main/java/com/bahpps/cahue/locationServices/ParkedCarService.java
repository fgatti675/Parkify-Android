package com.bahpps.cahue.locationServices;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.bahpps.cahue.util.CarLocationManager;

/**
 * This class is in charge of receiving location updates, after and store it as the cars position.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarService extends LocationPollerService {

    private final static String TAG = "ParkedCarService";

    @Override
    protected boolean checkPreconditions() {
        return true;
    }

    @Override
    public void onLocationPolled(Context context, Location location) {
        Log.i(TAG, "Received : " + location);
        CarLocationManager.saveLocation(context, location);
    }


}
