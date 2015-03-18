package com.cahue.iweco.locationServices;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarsSync;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Date;
import java.util.List;

/**
 * This class is in charge of receiving a location fix when the user parks his car.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarService extends LocationPollerService implements ResultCallback<Status> {

    private final static String TAG = "ParkedCarService";

    private Location carLocation;

    private PendingIntent mGeofencePendingIntent;

    /**
     * Result receiver that will send a notification when we are approaching a parked car.
     */
    private ResultReceiver geofenceResultReceiver = new ResultReceiver(new Handler()) {
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            Log.d(TAG, "Geofence result received");

            if (resultCode == GeofenceTransitionsIntentService.SUCCESS_RESULT) {
                Log.d(TAG, "Geofence result SUCCESS");
                Location location = resultData.getParcelable(GeofenceTransitionsIntentService.GEOFENCE_TRIGGERING_LOCATION_KEY);
                notifyApproachingCar(location, getCar());
            }

            // remove the geofence we just entered
            LocationServices.GeofencingApi.removeGeofences(
                    getGoogleApiClient(),
                    mGeofencePendingIntent
            );
        }
    };

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onFirstPreciseFixPolled(Context context, Location location, Car car) {

        Log.i(TAG, "Received : " + location);

        car.location = location;
        car.address = null;
        car.time = new Date();

        carLocation = location;

        CarDatabase carDatabase = CarDatabase.getInstance(context);
        CarsSync.storeCar(carDatabase, context, car);

        /**
         * If the location of the car is not good enough we cannot set a geofence afterwards so
         * we can finish it
         */
        if (carLocation.getAccuracy() > Constants.ACCURACY_THRESHOLD_M)
            finish();

    }

    @Override
    public void onActivitiesDetected(Context context, List<DetectedActivity> detectedActivities, Location lastLocation, Car car) {

        if (carLocation == null
                || carLocation.getAccuracy() > Constants.ACCURACY_THRESHOLD_M
                || lastLocation.getAccuracy() > Constants.ACCURACY_THRESHOLD_M) {
            Log.d(TAG, "Geofence not added because accuracy is not good enough");
            return;
        }

        if (carLocation.distanceTo(lastLocation) < Constants.PARKED_DISTANCE_THRESHOLD) {
            Log.d(TAG, "Geofence not added because we are not far enough from the car");
            return;
        }

        /**
         * Look for the most frequent activity
         */
        int[] frequencies = new int[9];
        for (DetectedActivity activity : detectedActivities)
            frequencies[activity.getType()]++;

        int mostProbable = DetectedActivity.UNKNOWN;
        int mostProbableFrequency = 0;
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] > mostProbableFrequency) {
                mostProbable = i;
                mostProbableFrequency = frequencies[i];
            }
        }

        /**
         * If the user didn't walk after parking we stop
         */
        if (mostProbable != DetectedActivity.ON_FOOT) {
            Log.d(TAG, "Geofence not added because most probable activity is: " + mostProbable);
            return;
        }

        /**
         * Create a geofence around the car
         */
        Geofence geofence = new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this geofence.
                .setRequestId(car.id)
                .setCircularRegion(
                        carLocation.getLatitude(),
                        carLocation.getLongitude(),
                        Constants.GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build();

        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build();

        LocationServices.GeofencingApi.addGeofences(
                getGoogleApiClient(),
                geofencingRequest,
                getGeofencePendingIntent(car)
        ).setResultCallback(this);

        Log.d(TAG, "Geofence added");

    }

    private PendingIntent getGeofencePendingIntent(Car car) {

        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        intent.putExtra(EXTRA_CAR, car);
        intent.putExtra(GeofenceTransitionsIntentService.RECEIVER, geofenceResultReceiver);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    @Override
    public void onResult(Status status) {

    }

    private void notifyGeofenceAdded(Location location, Car car) {

        long[] pattern = {0, 110, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_action_maps_my_location)
                        .setContentTitle("Geofence set for " + car.name);

        int id = (int) Math.random();
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }

    private void notifyApproachingCar(Location location, Car car) {

        long[] pattern = {0, 110, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_car_white_36dp)
                        .setContentTitle("Approaching " + car.name)
                        .setContentText(String.format("Distance: %d meters", location.distanceTo(car.location)));

        int id = (int) Math.random();
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }
}
