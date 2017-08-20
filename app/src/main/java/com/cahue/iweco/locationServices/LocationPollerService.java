package com.cahue.iweco.locationservices;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Date;

/**
 * Created by francesco on 17.09.2014.
 */
public abstract class LocationPollerService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {


    public static final int FAILURE_RESULT = 0;
    public static final int SUCCESS_RESULT = 1;
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL = 2000;
    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value, but they may be less frequent.
     */
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL / 2;
    /**
     * The max time before batched results are delivered by location services. Results may be
     * delivered sooner than this interval.
     */
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;
    /**
     * Do nothing before this time has passed. Useful to avoid stale locations
     */
    private final static int MINIMUM_TIME_MS = 4000;
    /**
     * Timeout after we consider the location may have changed too much for the initial fix
     */
    private final static int PRECISE_FIX_TIMEOUT_MS = 18500;
    /**
     * Time after which the service dies
     */
    private final static int SERVICE_TIMEOUT_MS = 60000;
    /**
     * Minimum desired accuracy
     */
    private final static int ACCURACY_THRESHOLD_M = 22;
    private final static String TAG = LocationPollerService.class.getSimpleName();
    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    private Date startTime;

    // Car related to this service
    @Nullable
    private Car car;

    private Handler handler;

    /**
     * Best location polled so far
     */
    private Location bestAccuracyLocation;

    @Nullable
    private final Runnable finishTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Handler finished " + LocationPollerService.this.getClass().getSimpleName() + " service");
            if (bestAccuracyLocation != null)
                notifyFixLocationAndStop(bestAccuracyLocation);
            else
                stopSelf();
        }
    };

    @Override
    public void onCreate() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        handler = new Handler();
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {

            if (intent.getExtras() != null) {
                String carId = intent.getExtras().getString(Constants.EXTRA_CAR_ID);
                if (carId != null) {
                    car = CarDatabase.getInstance().findCar(this, carId);
                    if (car == null) {
                        Log.e(TAG, "Car not found");
                    }
                }
            }

            start();

        }

        handler.postDelayed(finishTimeoutRunnable, SERVICE_TIMEOUT_MS);

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

        /**
         * Start location updates request
         */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Tracking.sendException(TAG, "No location permissions granted", false);
            return;
        }


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, getClass().getSimpleName() + " onDestroy");
        mGoogleApiClient.disconnect();
        handler.removeCallbacks(finishTimeoutRunnable);
    }

    /**
     * Notify the location of the event (like user parked or drove off)
     *
     * @param location
     */
    private void notifyFixLocationAndStop(@NonNull Location location) {
        Bundle extras = new Bundle();
        extras.putSerializable(Constants.EXTRA_START_TIME, startTime);
        location.setExtras(extras);
        Log.i(TAG, "Notifying location polled: " + location);
        Log.i(TAG, "\tafter " + (System.currentTimeMillis() - startTime.getTime()) + " ms");
        onPreciseFixPolled(this, location, car, startTime);
        stopSelf();
    }


    /**
     * Called after the first precise enough fix is received, or after {@link #PRECISE_FIX_TIMEOUT_MS}
     * is reached.
     *
     * @param context
     * @param location  The location fetched as a result
     * @param car       The car associated with this request (can be null)
     * @param startTime The time this request startes
     */
    public abstract void onPreciseFixPolled(Context context,
                                            Location location,
                                            Car car,
                                            Date startTime);

    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "GoogleApiClient connection has failed");
        stopSelf();
    }

    @NonNull
    public Car getCar() {
        return car;
    }

}
