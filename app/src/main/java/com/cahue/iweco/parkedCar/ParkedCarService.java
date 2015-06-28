package com.cahue.iweco.parkedCar;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.IwecoApp;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.locationServices.GeofenceCarService;
import com.cahue.iweco.locationServices.LocationPollerService;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.Date;

/**
 * This class is in charge of receiving a location fix when the user parks his car.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarService extends LocationPollerService {

    private final static String TAG = ParkedCarService.class.getSimpleName();

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onPreciseFixPolled(Context context, Location location, Car car, GoogleApiClient googleApiClient) {

        Log.i(TAG, "Received : " + location);

        car.spotId = null;
        car.location = location;
        car.address = null;
        car.time = new Date();

        CarDatabase carDatabase = CarDatabase.getInstance(context);
        CarsSync.storeCar(carDatabase, context, car);

        Tracker tracker = ((IwecoApp) getApplication()).getTracker();
        tracker.setScreenName(TAG);
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory("IO")
                .setAction("post")
                .setLabel("Car update")
                .build());

        /**
         * If the location of the car is good enough we can set a geofence afterwards.
         */
        if (car.location.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            GeofenceCarService.startDelayedGeofenceService(context, car.id);
        }

    }


}
