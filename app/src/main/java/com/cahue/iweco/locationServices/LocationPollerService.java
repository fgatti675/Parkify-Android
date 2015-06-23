package com.cahue.iweco.locationServices;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
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


    private final static int SERVICE_TIMEOUT_MS = 60000;

    /**
     * Minimum desired accuracy
     */
    private final static int ACCURACY_THRESHOLD_M = 22;

    private final static String TAG = LocationPollerService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    private Date startTime;

    // Car related to this service
    private Car car;

    private Handler handler;

//    private PendingIntent pActivityRecognitionIntent;

    /**
     * Best location polled so far
     */
    private Location bestAccuracyLocation;

    private List<DetectedActivity> detectedActivities;

    // Use a broadcast receiver to get the detected activities
    BroadcastReceiver activityReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
            DetectedActivity detectedActivity = intent.getParcelableExtra(ActivityRecognitionIntentService.DETECTED_ACTIVITY_DATA_KEY);
            Log.d(TAG, "Received : " + detectedActivity);
            detectedActivities.add(detectedActivity);
        }
    };
    // Is the broadcast receiver listening for activity updates ?
    private boolean listeningActivities = false;
    private Runnable finishTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Handler finished " + LocationPollerService.this.getClass().getSimpleName() + " service");
            if (bestAccuracyLocation != null)
                notifyFixLocation(bestAccuracyLocation);
            else
                stopSelf();
        }
    };

    @Override
    public void onCreate() {
        detectedActivities = new ArrayList<>();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
//                .addApi(ActivityRecognition.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            car = CarDatabase.getInstance(this).find(intent.getExtras().getString(EXTRA_CAR));
            if (car == null) {
                Log.e(TAG, "CAR NOT FOUND");
            }
            start();

            handler.postDelayed(finishTimeoutRunnable,
                    SERVICE_TIMEOUT_MS);
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
//        Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
//        pActivityRecognitionIntent = PendingIntent.getService(this, 84727, intent, PendingIntent.FLAG_UPDATE_CURRENT);

//        listeningActivities = true;
//        registerReceiver(activityReceiver, new IntentFilter(ActivityRecognitionIntentService.INTENT_ACTIVITY_DETECTED));

//        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(mGoogleApiClient, 2000, pActivityRecognitionIntent);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, getClass().getSimpleName() + " onDestroy");
//        if (listeningActivities)
//            unregisterReceiver(activityReceiver);
//        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, pActivityRecognitionIntent);
        if (mGoogleApiClient.isConnected())
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        handler.removeCallbacks(finishTimeoutRunnable);
    }

    @Override
    public void onLocationChanged(Location location) {

        Date now = new Date();

        Log.v(TAG, location.toString());

        if (location.getAccuracy() < ACCURACY_THRESHOLD_M) {
            notifyFixLocation(location);
            return;
        }

        if (bestAccuracyLocation == null || location.getAccuracy() < bestAccuracyLocation.getAccuracy()) {
            bestAccuracyLocation = location;
        }

        if (now.getTime() - startTime.getTime() > PRECISE_FIX_TIMEOUT_MS) {
            notifyFixLocation(bestAccuracyLocation);
            return;
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
        Log.i(TAG, "\tafter " + (System.currentTimeMillis() - startTime.getTime()) + " ms") ;
        onPreciseFixPolled(this, location, car, mGoogleApiClient);
        stopSelf();
    }


    /**
     * Called after the first precise enough fix is received, or after {@link #PRECISE_FIX_TIMEOUT_MS}
     * is reached.
     *
     * @param context
     * @param location
     * @param car
     * @param googleApiClient A connected GoogleApiClient
     */
    public abstract void onPreciseFixPolled(Context context, Location location, Car car, GoogleApiClient googleApiClient);


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
