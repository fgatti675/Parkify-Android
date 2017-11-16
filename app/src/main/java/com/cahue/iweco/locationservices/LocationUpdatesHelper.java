package com.cahue.iweco.locationservices;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.cahue.iweco.util.Tracking;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.cahue.iweco.Constants.EXTRA_CAR_ID;

/**
 * Created by f.gatti.gomez on 02.07.17.
 */
public class LocationUpdatesHelper {

    /**
     * Do nothing before this time has passed. Useful to avoid stale locations
     */
    final static int MINIMUM_TIME_MS = 4000;

    /**
     * Timeout after we consider the location may have changed too much for the initial fix
     */
    final static int PRECISE_FIX_TIMEOUT_MS = 18500;

    /**
     * Time after which the service dies
     */
    final static int SERVICE_TIMEOUT_MS = 60000;

    private static final String TAG = LocationUpdatesHelper.class.getSimpleName();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL = 3000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value, but they may be less frequent.
     */
    private static final long FASTEST_UPDATE_INTERVAL = UPDATE_INTERVAL;

    /**
     * The max time before batched results are delivered by location services. Results may be
     * delivered sooner than this interval.
     */
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3;

    private final Context context;

    private PendingIntent pendingIntent;

    private String action;

    private Map<String, Class<? extends BroadcastReceiver>> receiversMap = new HashMap<String, Class<? extends BroadcastReceiver>>() {{
        put(CarMovedReceiver.ACTION, CarMovedReceiver.class);
        put(ParkedCarReceiver.ACTION, ParkedCarReceiver.class);
        put(PossibleParkedCarReceiver.ACTION, PossibleParkedCarReceiver.class);
        put(GeofenceCarReceiver.ACTION, GeofenceCarReceiver.class);
    }};

    private Bundle extras;
    private FusedLocationProviderClient fusedLocationProviderClient;

    public LocationUpdatesHelper(Context context, String action) {
        this.context = context;
        this.action = action;
        fusedLocationProviderClient = new FusedLocationProviderClient(context);
    }

    public void startLocationUpdates(Bundle extras) {

        this.extras = extras;

        Log.i(TAG, "Starting location updates");

        /**
         * Start location updates request
         */
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Tracking.sendException(TAG, "No location permissions granted", false);
            return;
        }

        pendingIntent = getPendingIntent(context, getIntent());

        LocationRequest locationRequest = new LocationRequest();

        locationRequest.setInterval(UPDATE_INTERVAL);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        locationRequest.setMaxWaitTime(MAX_WAIT_TIME);

        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, pendingIntent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removal of location updates.
     */
    public void stopLocationUpdates(Intent intent) {
        Log.i(TAG, "Removing location updates");
        pendingIntent = getPendingIntent(context, intent);

        fusedLocationProviderClient.removeLocationUpdates(pendingIntent);
    }

    private PendingIntent getPendingIntent(Context context, Intent intent) {
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private Intent getIntent() {
        Intent intent = new Intent(context, receiversMap.get(action));
        intent.setAction(action);

        // TODO: change to extras when this is fixed: https://issuetracker.google.com/issues/37013793
        // Passing data as a URI for now
        Uri.Builder builder = new Uri.Builder().scheme("parkify").path("location")
                .appendQueryParameter("startTime", String.valueOf(new Date().getTime()));

        if (extras != null && extras.containsKey(EXTRA_CAR_ID)) {
            builder.appendQueryParameter("car", extras.getString(EXTRA_CAR_ID));
        }
        intent.setData(builder.build());

//        if (extras != null) extras = new Bundle();
//        extras.putSerializable(Constants.EXTRA_START_TIME, new Date());
//        intent.putExtra(Constants.EXTRA_BUNDLE, extras);
        return intent;
    }
}
