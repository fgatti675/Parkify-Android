package com.cahue.iweco.locationservices;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.cahue.iweco.R;
import com.cahue.iweco.activityrecognition.ActivityRecognitionService;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cahue.iweco.Constants.EXTRA_CAR_ID;
import static com.cahue.iweco.util.NotificationChannelsUtils.LOCATION_CHANNEL_ID;

/**
 * Created by f.gatti.gomez on 02.07.17.
 */
public abstract class AbstractLocationUpdatesService extends Service {


    public static final int MINIMUM_ACCURACY = 25;

    public static final long MAX_DURATION = 20 * 1000;

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

    private static final String TAG = AbstractLocationUpdatesService.class.getSimpleName();

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

    private String carId;

    private Date startTime;

    private Location mostAccurateLocation = null;

    private boolean notified = false;



    private LocationCallback locationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {

            long elapsedTime = new Date().getTime() - startTime.getTime();

            onLocationChanged( carId, locationResult.getLocations());


            if (elapsedTime > MAX_DURATION) {

                FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(AbstractLocationUpdatesService.this);
                Bundle bundle = new Bundle();
                bundle.putBoolean("has_location", mostAccurateLocation != null);
                firebaseAnalytics.logEvent("location_timed_out", bundle);

                if (mostAccurateLocation == null) {
                    Log.w(TAG, "Timed out and no location");
                    stopSelf();
                } else {
                    notifyFixLocationAndStop(carId, mostAccurateLocation);
                }
            }

            Log.d(TAG, "elapsed time: " + elapsedTime);
        }
    };


    private void onLocationChanged(@Nullable String carId, @NonNull List<Location> locations) {

        Log.d(TAG, "onLocationChanged: " + locations);

        for (Location loc : locations) {
            Log.d(TAG, "Location polled: " + loc);
            if (mostAccurateLocation == null || loc.getAccuracy() < mostAccurateLocation.getAccuracy())
                mostAccurateLocation = loc;
        }

        if (mostAccurateLocation == null) {
            return;
        }

        if (mostAccurateLocation.hasAccuracy() && mostAccurateLocation.getAccuracy() < MINIMUM_ACCURACY) {
            notifyFixLocationAndStop(carId, mostAccurateLocation);
        }


    }

    private void notifyFixLocationAndStop( @Nullable String carId, Location location) {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putInt("accuracy", (int) location.getAccuracy());
        firebaseAnalytics.logEvent("location_polled", bundle);

        Log.i(TAG, "Notifying location polled: " + location);
        Log.i(TAG, "\tafter " + (System.currentTimeMillis() - startTime.getTime()) + " ms");
        if (!notified)
            onPreciseFixPolled(location, carId, startTime);
        notified = true;

        stopSelf();
    }

    protected abstract void onPreciseFixPolled(Location location, String carId, Date startTime);

    private FusedLocationProviderClient fusedLocationProviderClient;

    public AbstractLocationUpdatesService() {
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {

        Log.v(TAG, "onCreate");

        fusedLocationProviderClient = new FusedLocationProviderClient(this);
        startTime = new Date();

        doStartForeground();


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Tracking.sendException(TAG, "No location permissions granted", false);
            stopSelf();
        }

    }

    private void doStartForeground() {

        createNotificationChannel();

        PendingIntent cancelActRecPendingIntent = PendingIntent.getBroadcast(
                this,
                74553,
                new Intent(this, ActivityRecognitionService.StopActivityRecognitionBroadcastReceiver.class),
                PendingIntent.FLAG_CANCEL_CURRENT);

        startForeground(27513, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, LOCATION_CHANNEL_ID) : new Notification.Builder(this))
                .setContentTitle(getString(R.string.location_in_progress))
                .setContentText(getString(R.string.location_instructions))
                .setSmallIcon(R.drawable.ic_car_primary_16dp)
                .setColor(getResources().getColor(R.color.theme_primary))
                .addAction(R.drawable.ic_exit_to_app_24dp, getString(R.string.stop), cancelActRecPendingIntent)
                .build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotifyMgr = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
            /* Create or update. */
            NotificationChannel channel = new NotificationChannel(LOCATION_CHANNEL_ID, this.getString(R.string.location_channel_description),
                    NotificationManager.IMPORTANCE_MIN);
            mNotifyMgr.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        doStartForeground();

        Log.i(TAG, "Starting location updates");

        this.carId = intent.getStringExtra(EXTRA_CAR_ID);

        /*
         * Start location updates request
         */
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

            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Removing location updates");
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    public static class StopLocationPollBroadcastReceiver extends BroadcastReceiver {
        public StopLocationPollBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "StopLocationPollBroadcastReceiver: on stop clicked");
            context.stopService(new Intent(context, AbstractLocationUpdatesService.class));
        }
    }

}
