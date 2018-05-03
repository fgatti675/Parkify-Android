package com.cahue.iweco.locationservices;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.cahue.iweco.util.FetchAddressDelegate;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

/**
 * This class is in charge of uploading the location of the car to the server when BT connects
 * and the car starts moving.
 *
 * @author Francesco
 */
public class CarMovedReceiver extends AbstractLocationUpdatesBroadcastReceiver {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".CAR_MOVED_ACTION";

    private final static String TAG = CarMovedReceiver.class.getSimpleName();

    private void clearGeofence(Context context, @NonNull final String carId) {
        GeofencingClient mGeofencingClient = LocationServices.getGeofencingClient(context);

        // remove the geofence we just entered
        mGeofencingClient.removeGeofences(
                Collections.singletonList(carId)
        );
        Log.d(TAG, "Geofence removed");
    }

    @Override
    protected void onPreciseFixPolled(Context context, final Location spotLocation, Bundle extras) {

        CarDatabase carDatabase = CarDatabase.getInstance();
        String carId = extras.getString(Constants.EXTRA_CAR_ID, null);

        Date startTime = (Date) extras.getSerializable(Constants.EXTRA_START_TIME);

        Util.showBlueToast(context, R.string.thanks_free_spot, Toast.LENGTH_SHORT);

        carDatabase.removeCarLocation(carId);
        clearGeofence(context, carId);

        Tracking.sendEvent(Tracking.CATEGORY_PARKING, Tracking.ACTION_BLUETOOTH_FREED_SPOT);

        ParkingSpotSender.doPostSpotLocation(context, spotLocation, false, carId);
        ParkingSpotSender.fetchAddressAndSave(context, spotLocation, startTime, carId, false);

    }
}
