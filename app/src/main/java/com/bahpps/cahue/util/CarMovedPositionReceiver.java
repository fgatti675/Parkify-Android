package com.bahpps.cahue.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class is in charge of receiving location updates, and store it as the cars position. It is in charge of deciding
 * the most accurate position (where we think the car is), based on wifi and gps position providers.
 *
 * @author Francesco
 */
public class CarMovedPositionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CarLocationManager.postLocation(context);
    }

}
