package com.cahue.iweco.locationServices;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.util.Singleton;
import com.cahue.iweco.util.Requests;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.cahue.iweco.spots.ParkingSpotSender;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;

/**
 * This class is in charge of uploading the location of the car to the server when BT connects
 * and the car starts moving.
 *
 * @author Francesco
 */
public class CarMovedService extends LocationPollerService {

    private final static String TAG = CarMovedService.class.getSimpleName();

    @Override
    protected boolean checkPreconditions(Car car) {
        if(BuildConfig.DEBUG) return true;
        long now = Calendar.getInstance().getTimeInMillis();
        if (car.time == null) return true;
        long parkingTime = car.time.getTime();
        boolean result = now - parkingTime > Constants.MINIMUM_STAY_MS;
        if (!result)
            Log.w(TAG, "Preconditions failed");
        return result;
    }

    @Override
    public void onPreciseFixPolled(Context context, Location spotLocation, Car car) {

        CarDatabase carDatabase = CarDatabase.getInstance(context);

        /**
         * If the accuracy is not good enough, we can check the previous location of the car
         * and if it's close and more accurate, we use it.
         */
        if (spotLocation.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            if (car.location != null && car.location.distanceTo(spotLocation) < Constants.ACCURACY_THRESHOLD_M)
                spotLocation = car.location;
        }

        /**
         * If it's still not accurate, we don't use it
         */
        if (spotLocation.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            ParkingSpotSender.doPostSpotLocation(this, spotLocation, false, car);
        }

        CarsSync.clearLocation(carDatabase, this, car);
        clearGeofence(car);
    }

    private GoogleApiClient mGeofenceApiClient;

    private void clearGeofence(final Car car){
        mGeofenceApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                        Log.d(TAG, "Geofence, onConnected");
                        LocationServices.GeofencingApi.removeGeofences(
                                mGeofenceApiClient,
                                Arrays.asList(car.id)
                        );
                        Log.d(TAG, "Geofence removed");
                        mGeofenceApiClient.disconnect();
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

}
