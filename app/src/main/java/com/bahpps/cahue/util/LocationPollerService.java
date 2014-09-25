package com.bahpps.cahue.util;

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
     * Timeout after we consider the location may have changed too much
     */
    private final static int TIMEOUT_MS = 8000;

    /**
     * Minimum desired accuracy
     */
    private final static int ACCURACY_THRESHOLD_M = 20;

    private final static String TAG = "LocationPoller";

    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    private Date startTime;

    private Location lastLocation;


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
        startTime = new Date();
        mGoogleApiClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000);

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
        }

        else if(now.getTime() - startTime.getTime() > TIMEOUT_MS) {
            Location bestAccuracyLocation = location;
            if(lastLocation != null && lastLocation.getAccuracy() > bestAccuracyLocation.getAccuracy()){
                bestAccuracyLocation = lastLocation;
            }
            notifyLocation(bestAccuracyLocation);
        }

        lastLocation = location;
    }

    private void notifyLocation(Location location) {
        Log.i(TAG, "Notifying location polled: " + location);
        onLocationPolled(this, location);
        mGoogleApiClient.disconnect();
        stopSelf();
    }

    public abstract void onLocationPolled(Context context, Location location);
}
