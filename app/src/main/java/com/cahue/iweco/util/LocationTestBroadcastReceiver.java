package com.cahue.iweco.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.cahue.iweco.Constants;
import com.cahue.iweco.locationservices.CarMovedReceiver;
import com.cahue.iweco.locationservices.LocationUpdatesService;
import com.cahue.iweco.locationservices.ParkedCarReceiver;
import com.cahue.iweco.model.Car;

/**
 * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
 */
public class LocationTestBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        LocationUpdatesService.startLocationUpdate(context,
                ParkedCarReceiver.ACTION,
                Car.OTHER_ID);
    }

}
