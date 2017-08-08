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
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.cahue.iweco.util.Util;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;

/**
 * This class is in charge of uploading the location of the car to the server when BT connects
 * and the car starts moving.
 *
 * @author Francesco
 */
public class CarMovedReceiver extends AbstractLocationUpdatesBroadcastReceiver {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".CAR_MOVED_ACTION";

    private final static String TAG = CarMovedReceiver.class.getSimpleName();

    private GoogleApiClient mGeofenceApiClient;

    private void clearGeofence(Context context, @NonNull final Car car) {
        mGeofenceApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                        LocationServices.GeofencingApi.removeGeofences(
                                mGeofenceApiClient,
                                Arrays.asList(car.id)
                        );
                        Log.d(TAG, "Geofence removed");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Geofence, connection suspended");
                        mGeofenceApiClient.disconnect();
                    }
                })
                .build();

        mGeofenceApiClient.connect();
    }

    @Override
    protected void onPreciseFixPolled(Context context, Location spotLocation, Bundle extras) {

        CarDatabase carDatabase = CarDatabase.getInstance();
        String carId = extras.getString(Constants.EXTRA_CAR_ID, null);
        Car car = carDatabase.findCar(context, carId);

        /**
         * If the accuracy is not good enough, we can check the previous location of the car
         * and if it's close and more accurate, we use it.
         */
        if (spotLocation.getAccuracy() > Constants.ACCURACY_THRESHOLD_M) {
            if (car.location != null && car.location.distanceTo(spotLocation) < Constants.ACCURACY_THRESHOLD_M)
                spotLocation = car.location;
        }

        /**
         * If it's accurate enough we notify
         */
        if (spotLocation.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            ParkingSpotSender.doPostSpotLocation(context, spotLocation, false, car);
            Util.showBlueToastWithLogo(context, R.string.thanks_free_spot, Toast.LENGTH_SHORT);
        }

        CarsSync.clearLocation(carDatabase, context, car);
        clearGeofence(context, car);
    }
}
