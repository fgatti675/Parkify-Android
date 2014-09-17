package com.bahpps.cahue.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * This class is in charge of receiving location updates, after and store it as the cars position.
 *
 * @author Francesco
 */
public class ParkedCarLocationReceiver extends LocationPoller {

    private final static String TAG = "ParkedCarPositionReceiver";

    @Override
    public void onLocationPolled(Context context, Location location) {
        Log.i(TAG, "Received : " + location);
        CarLocationManager.saveLocation(context, location);
    }


}
