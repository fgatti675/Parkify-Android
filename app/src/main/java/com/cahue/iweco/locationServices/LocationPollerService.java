package com.cahue.iweco.locationServices;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.cahue.iweco.cars.Car;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by francesco on 17.09.2014.
 */
public abstract class LocationPollerService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    /**
     * Include the start time when sending intents
     */
    public static final String EXTRA_START_TIME = "extra_time";

    /**
     * Receive the address of the BT device
     */
    public static final String EXTRA_CAR = "extra_bt_car";

    /**
     * Timeout after we consider the location may have changed too much for the initial fix
     */
    private final static int PRECISE_FIX_TIMEOUT_MS = 18500;

    /**
     * Minimum desired accuracy
     */
    private final static int ACCURACY_THRESHOLD_M = 18;

    private final static String TAG = LocationPollerService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private Date startTime;

    // Car related to this service
    private Car car;

    private PendingIntent pActivityRecognitionIntent;

    /**
     * Best location polled so far
     */
    private Location bestAccuracyLocation;

    private List<DetectedActivity> detectedActivities;

    /**
     * Flag to indicate that the first precise fix has been notified
     */
    private boolean fixNotified = false;
    private boolean finalFixNotified = false;

    // Is the broadcast receiver listening for activity updates ?
    private boolean listeningActivities = false;

    // Use a broadcast receiver to get the detected activities
    BroadcastReceiver activityReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            DetectedActivity detectedActivity = intent.getParcelableExtra(ActivityRecognitionIntentService.DETECTED_ACTIVITY_DATA_KEY);
            Log.d(TAG, "Received : " + detectedActivity);
            detectedActivities.add(detectedActivity);
        }
    };


    @Override
    public void onCreate() {
        detectedActivities = new ArrayList<>();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            car = intent.getExtras().getParcelable(EXTRA_CAR);
            start();
        }
        return START_STICKY_COMPATIBILITY;
    }

    protected void start() {
        if (checkPreconditions(car)) {
            startTime = new Date();
            mGoogleApiClient.connect();
        } else {
            stopSelf();
        }
    }

    /**
     * Check that this location request must actually be executed.
     *
     * @param car
     * @return
     */
    protected abstract boolean checkPreconditions(Car car);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "onConnected");

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1500);

        /**
         * Start location updates request
         */
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        /**
         * Start activity recognition
         */
        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
        pActivityRecognitionIntent = PendingIntent.getService(this, 84727, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        listeningActivities = true;
        registerReceiver(activityReceiver, new IntentFilter(ActivityRecognitionIntentService.INTENT_ACTIVITY_DETECTED));

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 2000, pActivityRecognitionIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (listeningActivities)
            unregisterReceiver(activityReceiver);
    }

    @Override
    public void onLocationChanged(Location location) {

        Date now = new Date();

        /**
         * If the precise fix wasn't yet sent.
         */
        if (!fixNotified) {

            if (location.getAccuracy() < ACCURACY_THRESHOLD_M) {
                notifyFixLocation(location);
                return;
            }

            if (bestAccuracyLocation == null || location.getAccuracy() < bestAccuracyLocation.getAccuracy()) {
                bestAccuracyLocation = location;
            }

            if (now.getTime() - startTime.getTime() > PRECISE_FIX_TIMEOUT_MS) {
                notifyFixLocation(bestAccuracyLocation);
            }
        }
    }

    /**
     * Notify the location of the event (like user parked or drove off)
     *
     * @param location
     */
    private void notifyFixLocation(Location location) {
        Bundle extras = new Bundle();
        extras.putSerializable(EXTRA_START_TIME, startTime);
        location.setExtras(extras);
        Log.i(TAG, "Notifying location polled: " + location);
        onPreciseFixPolled(this, location, car);
        fixNotified = true;
        finish();
    }


    protected void finish() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, pActivityRecognitionIntent);
        mGoogleApiClient.disconnect();
        stopSelf();
    }

    /**
     * Called after the first precise enough fix is received, or after {@link #PRECISE_FIX_TIMEOUT_MS}
     * is reached.
     *
     * @param context
     * @param location
     * @param car
     */
    public abstract void onPreciseFixPolled(Context context, Location location, Car car);


    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed");
        stopSelf();
    }

    public Car getCar() {
        return car;
    }
}
