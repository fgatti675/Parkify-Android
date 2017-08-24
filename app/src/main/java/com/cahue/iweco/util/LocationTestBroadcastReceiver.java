package com.cahue.iweco.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.cahue.iweco.Constants;
import com.cahue.iweco.locationservices.LocationUpdatesHelper;
import com.cahue.iweco.locationservices.ParkedCarReceiver;
import com.cahue.iweco.model.Car;

/**
 * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
 */
public class LocationTestBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        LocationUpdatesHelper helper = new LocationUpdatesHelper(context, ParkedCarReceiver.ACTION);
        Bundle extras = new Bundle();
        extras.putString(Constants.EXTRA_CAR_ID, Car.OTHER_ID);
        helper.startLocationUpdates(extras);
    }

}
