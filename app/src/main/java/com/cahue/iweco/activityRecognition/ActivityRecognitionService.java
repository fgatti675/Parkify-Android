package com.cahue.iweco.activityRecognition;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.DetectedActivityCreator;


/**
 * Created by Francesco on 27/06/2015.
 */
public class ActivityRecognitionService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 2000;
    private static final String TAG = ActivityRecognitionService.class.getSimpleName();

    private static final String PREF_PREVIOUS_ACTIVITY_TYPE = "PREF_PREVIOUS_ACTIVITY_CONFIDENCE";
    private static final String PREF_PREVIOUS_ACTIVITY_CONFIDENCE = "PREF_PREVIOUS_ACTIVITY_CONFIDENCE";


    private BroadcastReceiver mBroadcastReceiver;
    private GoogleApiClient mGoogleApiClient;

    private DetectedActivity previousActivity;

    /**
     * Used when requesting or removing activity detection updates.
     */
    private PendingIntent mActivityDetectionPendingIntent;

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {

        Log.v(TAG, "onCreate");

        previousActivity = getStoredDetectedActivity();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

        // Get a receiver for broadcasts from ActivityDetectionIntentService.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ActivityRecognitionResult result = (ActivityRecognitionResult) intent.getExtras().get(Constants.INTENT_EXTRA_ACTIVITIES_RESULT);
                handleDetectedActivities(result);
            }
        };

        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        mActivityDetectionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.INTENT_ACTIVITY_RECOGNIZED));
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, mActivityDetectionPendingIntent);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand");
        mGoogleApiClient.connect();
        return Service.START_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.v(TAG, "onConnected");
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                DETECTION_INTERVAL_IN_MILLISECONDS,
                mActivityDetectionPendingIntent
        );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }


    private void handleDetectedActivities(ActivityRecognitionResult result) {

        // Log each activity.
        Log.d(TAG, "Activities detected");
        for (DetectedActivity da : result.getProbableActivities()) {
            Log.v(TAG, getActivityString(da.getType()) + " " + da.getConfidence() + "%");
        }

        DetectedActivity mostProbableActivity = result.getMostProbableActivity();

        if (!isMovementRelated(mostProbableActivity))
            return;


        if (previousActivity == null ||
                (mostProbableActivity.getType() != previousActivity.getType() && mostProbableActivity.getConfidence() > 90)) {

            if (BuildConfig.DEBUG) {
                showDebugNotification(result, mostProbableActivity);
            }

            if(mostProbableActivity.getType() == DetectedActivity.ON_FOOT &&
                    previousActivity.getType() == DetectedActivity.IN_VEHICLE){
                // we create an intent to start the location poller service, as declared in manifest
                Intent intent = new Intent();
                intent.setClass(this, ParkedCarRequestedService.class);
                this.startService(intent);
            }

            previousActivity = mostProbableActivity;
            savePreviousActivity(previousActivity);
        }

    }


    private void showDebugNotification(ActivityRecognitionResult result, DetectedActivity mostProbableActivity) {
        String typeString = getActivityString(mostProbableActivity.getType());

        String previousText = previousActivity != null ?
                "Previous: " + getActivityString(previousActivity.getType()) + " (" + previousActivity.getConfidence() + "%)\n" :
                "Previous unknown\n";

        StringBuilder stringBuilder = new StringBuilder(previousText);
        for (DetectedActivity detectedActivity : result.getProbableActivities()) {
            stringBuilder.append(getActivityString(detectedActivity.getType()) + " (" + detectedActivity.getConfidence() + "%)\n");
        }

        long[] pattern = {0, 100, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_navigation_cancel)
                        .setContentTitle(typeString + " (" + mostProbableActivity.getConfidence() + "%)")
                        .setStyle(new Notification.BigTextStyle().bigText(stringBuilder.toString()))
                        .setContentText(previousText);

        int id = (int) Math.random();
        mNotifyMgr.notify("" + id, id, mBuilder.build());
    }


    private String getActivityString(int type) {
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

    private boolean isMovementRelated(DetectedActivity detectedActivity) {
        switch (detectedActivity.getType()) {
            case DetectedActivity.IN_VEHICLE:
            case DetectedActivity.ON_BICYCLE:
            case DetectedActivity.ON_FOOT:
                return true;
            default:
                return false;
        }
    }


    private void savePreviousActivity(DetectedActivity previousActivity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putInt(PREF_PREVIOUS_ACTIVITY_TYPE, previousActivity.getType())
                .putInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, previousActivity.getConfidence())
                .apply();
    }

    private DetectedActivity getStoredDetectedActivity(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int type = prefs.getInt(PREF_PREVIOUS_ACTIVITY_TYPE, DetectedActivity.UNKNOWN);
        int confidence = prefs.getInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, 0);
        return new DetectedActivity(type, confidence);
    }

}
