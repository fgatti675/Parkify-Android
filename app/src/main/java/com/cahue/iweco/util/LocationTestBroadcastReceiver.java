package com.cahue.iweco.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import com.cahue.iweco.locationservices.AbstractLocationUpdatesService;
import com.cahue.iweco.locationservices.ParkedCarService;
import com.cahue.iweco.model.Car;

import static com.cahue.iweco.Constants.EXTRA_CAR_ID;

/**
 * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
 */
public class LocationTestBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        Intent i = new Intent(context, ParkedCarService.class);
        i.putExtra(EXTRA_CAR_ID,Car.OTHER_ID);
        ContextCompat.startForegroundService(context, intent);

    }

}
