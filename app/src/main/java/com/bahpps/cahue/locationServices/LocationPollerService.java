package com.bahpps.cahue.locationServices;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

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
     * Include the start time when sending intents
     */
    public static final String EXTRA_START_TIME = "extra_time";

    /**
     * Receive the address of the BT device
     */
    public static final String EXTRA_BT_ID = "extra_bt_id";

    public static final String EXTRA_BT_NAME = "extra_bt_name";

    /**
     * Timeout after we consider the location may have changed too much
     */
    private final static int TIMEOUT_MS = 18500;

    /**
     * Minimum desired accuracy
     */
    private final static int ACCURACY_THRESHOLD_M = 16;

    private final static String TAG = "LocationPoller";

    private GoogleApiClient mGoogleApiClient;

    private Date startTime;

    // BT address used as an ID for cars
    private String id;

    private String name;

    private Location bestAccuracyLocation;


    @Override
    public void onCreate() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            id = intent.getExtras().getString(EXTRA_BT_ID);
            name = intent.getExtras().getString(EXTRA_BT_NAME);
            start();
        }
        return START_STICKY_COMPATIBILITY;
    }

    protected void start() {
        if (checkPreconditions(id)) {
            startTime = new Date();
            mGoogleApiClient.connect();
        } else{
            stopSelf();
        }
    }

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

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GoogleApiClient connection has failed");
        stopSelf();
    }

    @Override
    public void onLocationChanged(Location location) {

        Date now = new Date();

        if (location.getAccuracy() < ACCURACY_THRESHOLD_M) {
            notifyLocation(location);
            return;
        }

        if (bestAccuracyLocation == null || location.getAccuracy() < bestAccuracyLocation.getAccuracy()) {
            bestAccuracyLocation = location;
        }

        if (now.getTime() - startTime.getTime() > TIMEOUT_MS) {
            notifyLocation(bestAccuracyLocation);
        }

    }

    protected abstract boolean checkPreconditions(String id);

    private void notifyLocation(Location location) {
        Bundle extras = new Bundle();
        extras.putSerializable(EXTRA_START_TIME, startTime);
        location.setExtras(extras);
        Log.i(TAG, "Notifying location polled: " + location);
        onLocationPolled(this, location, id, name);
        mGoogleApiClient.disconnect();
        stopSelf();
    }

    public abstract void onLocationPolled(Context context, Location location, String id, String name);
}
