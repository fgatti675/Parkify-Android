package com.cahue.iweco.locationServices;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Date;

/**
 * Created by francesco on 17.09.2014.
 */
public abstract class LocationPollerService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {


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

    /**
     * Best location polled so far
     */
    private Location bestAccuracyLocation;


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

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {

            if(intent.getExtras() != null) {
                String carId = intent.getExtras().getString(Constants.INTENT_CAR_EXTRA_ID);
                if (carId != null) {
                    car = CarDatabase.getInstance(this).find(carId);
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

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1500);
        mLocationRequest.setFastestInterval(500);

        /**
         * Start location updates request
         */
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, getClass().getSimpleName() + " onDestroy");
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
        extras.putSerializable(Constants.EXTRA_START_TIME, startTime);
        location.setExtras(extras);
        Log.i(TAG, "Notifying location polled: " + location);
        Log.i(TAG, "\tafter " + (System.currentTimeMillis() - startTime.getTime()) + " ms");
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
        Log.w(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.w(TAG, "GoogleApiClient connection has failed");
        stopSelf();
    }

    public Car getCar() {
        return car;
    }

}
