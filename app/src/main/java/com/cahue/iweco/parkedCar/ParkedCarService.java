package com.cahue.iweco.parkedcar;

import android.content.Context;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationservices.GeofenceCarService;
import com.cahue.iweco.locationservices.LocationPollerService;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.Util;
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
    public void onPreciseFixPolled(@NonNull Context context, Location location, @NonNull Car car, Date startTime, GoogleApiClient googleApiClient) {

        Log.i(TAG, "Received : " + location);

        car.spotId = null;
        car.location = location;
        car.address = null;
        car.time = new Date();

        CarDatabase carDatabase = CarDatabase.getInstance(context);
        CarsSync.storeCar(carDatabase, context, car);

        /**
         * If the location of the car is good enough we can set a geofence afterwards.
         */
        if (car.location.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            GeofenceCarService.startDelayedGeofenceService(context, car.id);
        }

        Util.showBlueToastWithLogo(ParkedCarService.this, getString(R.string.car_location_registered, car.name), Toast.LENGTH_SHORT);

    }


}
