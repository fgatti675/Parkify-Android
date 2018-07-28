package com.cahue.iweco.locationservices;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.cahue.iweco.R;
import com.cahue.iweco.activityrecognition.ActivityRecognitionService;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.cahue.iweco.Constants.EXTRA_CAR_ID;
import static com.cahue.iweco.util.NotificationChannelsUtils.LOCATION_CHANNEL_ID;

/**
 * Created by f.gatti.gomez on 02.07.17.
 */
public class LocationUpdatesService extends Service {

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

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

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

    private PendingIntent pendingIntent;

    private String action;
    private String carId;


    public static void startLocationUpdate(@NonNull Context context, String action, String carId) {
        Intent intent = new Intent(context, LocationUpdatesService.class);
        intent.putExtra("EXTRA_ACTION", action);
        intent.putExtra(EXTRA_CAR_ID, carId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private Map<String, Class<? extends BroadcastReceiver>> receiversMap = new HashMap<String, Class<? extends BroadcastReceiver>>() {{
        put(CarMovedReceiver.ACTION, CarMovedReceiver.class);
        put(ParkedCarReceiver.ACTION, ParkedCarReceiver.class);
        put(PossibleParkedCarReceiver.ACTION, PossibleParkedCarReceiver.class);
        put(GeofenceCarReceiver.ACTION, GeofenceCarReceiver.class);
    }};

    private FusedLocationProviderClient fusedLocationProviderClient;



    public LocationUpdatesService() {
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Starting location updates");

        this.action = intent.getStringExtra("EXTRA_ACTION");
        this.carId = intent.getStringExtra(EXTRA_CAR_ID);

        /**
         * Start location updates request
         */
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Tracking.sendException(TAG, "No location permissions granted", false);
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        pendingIntent = getPendingIntent(getLocationIntent());

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
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Removing location updates");
        fusedLocationProviderClient.removeLocationUpdates(pendingIntent);
    }


    private PendingIntent getPendingIntent(Intent intent) {
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private Intent getLocationIntent() {
        Intent intent = new Intent(this, receiversMap.get(action));
        intent.setAction(action);

        // TODO: change to extras when this is fixed: https://issuetracker.google.com/issues/37013793
        // Passing data as a URI for now
        Uri.Builder builder = new Uri.Builder().scheme("parkify").path("location")
                .appendQueryParameter("startTime", String.valueOf(new Date().getTime()));

        if (carId != null) {
            builder.appendQueryParameter("car", String.valueOf(carId));
        }
        intent.setData(builder.build());

//        if (extras != null) extras = new Bundle();
//        extras.putSerializable(Constants.EXTRA_START_TIME, new Date());
//        intent.putExtra(Constants.EXTRA_BUNDLE, extras);
        return intent;
    }

    public static class StopLocationPollBroadcastReceiver extends BroadcastReceiver {
        public StopLocationPollBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "StopLocationPollBroadcastReceiver: on stop clicked");
            context.stopService(new Intent(context, LocationUpdatesService.class));
        }
    }

}
