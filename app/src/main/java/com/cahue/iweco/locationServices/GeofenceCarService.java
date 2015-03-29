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

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Arrays;

/**
 * This service is in charge of detecting if the user is far away enough after parking, and if so,
 * setting a geofence around it.
 */
public class GeofenceCarService extends LocationPollerService {

    private static final String TAG = GeofenceCarService.class.getSimpleName();

    private PendingIntent mGeofencePendingIntent;

    private GoogleApiClient mGeofenceApiClient;

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

                ParkingSpotSender.doPostSpotLocation(GeofenceCarService.this, getCar().location, true, getCar());
            }

            // remove the geofence we just entered
            LocationServices.GeofencingApi.removeGeofences(
                    mGeofenceApiClient,
                    mGeofencePendingIntent
            );

            mGeofenceApiClient.disconnect();
        }
    };

    @Override
    protected boolean checkPreconditions(Car car) {
        return car != null && car.location != null;
    }

    @Override
    public void onPreciseFixPolled(Context context, Location location, Car car) {
        
        if (car.location == null) return;

        if (car.location.getAccuracy() > Constants.ACCURACY_THRESHOLD_M
                || location.getAccuracy() > Constants.ACCURACY_THRESHOLD_M) {
            String msg = "Geofence not added because accuracy is not good enough: Car " + car.location.getAccuracy() + " / User " + location.getAccuracy();
            Log.d(TAG, msg);
            if (BuildConfig.DEBUG) {
                notifyGeofenceError(car, msg);
            }
            return;
        }

        float distanceTo = car.location.distanceTo(location);
        if (distanceTo < Constants.PARKED_DISTANCE_THRESHOLD) {
            String msg = "GF Error: Too close to car: " + distanceTo;
            Log.d(TAG, msg);
            if (BuildConfig.DEBUG) {
                notifyGeofenceError(car, msg);
            }
            return;
        }

//        /**
//         * Look for the most frequent activity
//         */
//        int[] frequencies = new int[9];
//        for (DetectedActivity activity : detectedActivities)
//            frequencies[activity.getType()]++;
//
//        int mostProbableActivityType = DetectedActivity.UNKNOWN;
//        int mostProbableFrequency = 0;
//        for (int i = 0; i < frequencies.length; i++) {
//            if (frequencies[i] > mostProbableFrequency) {
//                mostProbableActivityType = i;
//                mostProbableFrequency = frequencies[i];
//            }
//        }

//        /**
//         * If the user didn't walk after parking we stop
//         */
//        if (mostProbableActivityType != DetectedActivity.ON_FOOT) {
//            Log.d(TAG, "Geofence not added because most probable activity is: " + mostProbableActivityType);
//            if (BuildConfig.DEBUG) {
//                notifyGeofenceError(car, "Detected activity: " + getTypeAsText(mostProbableActivityType));
//            }
//            return;
//        }

        addGeofence(car);
    }

    private void addGeofence(final Car car) {

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

                        /**
                         * Create a geofence around the car
                         */
                        Geofence geofence = new Geofence.Builder()
                                // Set the request ID of the geofence. This is a string to identify this geofence.
                                .setRequestId(car.id)
                                .setCircularRegion(
                                        car.location.getLatitude(),
                                        car.location.getLongitude(),
                                        Constants.GEOFENCE_RADIUS_IN_METERS
                                )
                                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL)
                                .setLoiteringDelay(2000)
                                .build();

                        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                                .addGeofence(geofence)
                                .build();

                        LocationServices.GeofencingApi.addGeofences(
                                mGeofenceApiClient,
                                geofencingRequest,
                                getGeofencePendingIntent(car)
                        );

                        if (BuildConfig.DEBUG) {
                            notifyGeofenceAdded(car);
                        }

                        Log.i(TAG, "Geofence added");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Geofence, connection suspended");
                    }
                })
                .build();

        mGeofenceApiClient.connect();
    }

    private PendingIntent getGeofencePendingIntent(Car car) {

        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        intent.putExtra(EXTRA_CAR, car.id);
        intent.putExtra(GeofenceTransitionsIntentService.RECEIVER, geofenceResultReceiver);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    private void notifyApproachingCar(Location location, Car car) {

        long[] pattern = {0, 1000, 200, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_car_white_36dp)
                        .setContentTitle("Approaching " + car.name);

        if (location != null)
            mBuilder.setContentText(String.format("Distance: %d meters", location.distanceTo(car.location)));

        int id = (int) Math.random();
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }

    private void notifyGeofenceError(Car car, String error) {

        long[] pattern = {0, 110, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_action_navigation_close)
                        .setContentTitle("Geofence ERROR for " + car.name)
                        .setContentText(error);

        int id = (int) Math.random();
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }

    private void notifyGeofenceAdded(Car car) {

        long[] pattern = {0, 1000, 200, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_action_maps_my_location)
                        .setContentTitle("Geofence set for " + car.name);

        int id = (int) Math.random();
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }

    private String getTypeAsText(int type) {
        if (type == DetectedActivity.UNKNOWN)
            return "Unknown";
        else if (type == DetectedActivity.IN_VEHICLE)
            return "In Vehicle";
        else if (type == DetectedActivity.ON_BICYCLE)
            return "On Bicycle";
        else if (type == DetectedActivity.ON_FOOT)
            return "On Foot";
        else if (type == DetectedActivity.STILL)
            return "Still";
        else if (type == DetectedActivity.TILTING)
            return "Tilting";
        else if (type == DetectedActivity.WALKING)
            return "Walking";
        else if (type == DetectedActivity.RUNNING)
            return "Running";
        else
            return "";
    }

}