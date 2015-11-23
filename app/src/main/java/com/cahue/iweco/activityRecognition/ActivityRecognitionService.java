package com.cahue.iweco.activityRecognition;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.util.PreferencesUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;


/**
 * Created by Francesco on 27/06/2015.
 */
public class ActivityRecognitionService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 5000;

    private static final String TAG = ActivityRecognitionService.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;

    /**
     * Used when requesting or removing activity detection updates.
     */
    private PendingIntent mActivityDetectionPendingIntent;
    private Intent intent;
    private int startId;

    /**
     * Only fetch updates if BT is off and the user has requested so
     *
     * @param context
     */
    public static void startIfNecessary(Context context) {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()
                && PreferencesUtil.isMovementRecognitionEnabled(context)
                && !CarDatabase.getInstance(context).isEmpty()) {

            Intent intent = new Intent(context, ActivityRecognitionService.class);
            intent.setAction(Constants.ACTION_START_ACTIVITY_RECOGNITION);
            context.startService(intent);
            if (BuildConfig.DEBUG) {
                NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                Notification.Builder mBuilder =
                        new Notification.Builder(context)
                                .setSmallIcon(R.drawable.ic_action_action_settings_dark)
                                .setContentTitle("Recognition Activated");
                mNotifyMgr.notify(null, 6472837, mBuilder.build());
            }
        }

    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, ActivityRecognitionService.class);
        intent.setAction(Constants.ACTION_STOP_ACTIVITY_RECOGNITION);
        context.startService(intent);
        if (BuildConfig.DEBUG) {
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder mBuilder =
                    new Notification.Builder(context)
                            .setSmallIcon(R.drawable.ic_navigation_cancel)
                            .setContentTitle("Recognition Stopped");
            mNotifyMgr.notify(null, 6472837, mBuilder.build());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {

        Log.v(TAG, "onCreate");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();

    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");
        mGoogleApiClient.disconnect();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        this.intent = intent;
        this.startId = startId;

        Log.v(TAG, "onStartCommand");

        if (intent.getAction() != null)
            mGoogleApiClient.connect();
        else
            stopSelf(startId);

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.v(TAG, "onConnected");

        if (intent.getAction().equals(Constants.ACTION_START_ACTIVITY_RECOGNITION)) {
            startActivityRecognition();
        } else if (intent.getAction().equals(Constants.ACTION_STOP_ACTIVITY_RECOGNITION)) {
            stopActivityDetection();
        } else {
            throw new RuntimeException("ActivityRecognitionService must be started with a valid action");
        }

        stopSelf(startId);
    }

    private void startActivityRecognition() {

        Log.i(TAG, "Starting activity recognition");

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent()
        );

    }

    private void stopActivityDetection() {

        Log.i(TAG, "Stopping activity recognition");

        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent());

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private PendingIntent getActivityDetectionPendingIntent() {
        if (mActivityDetectionPendingIntent == null) {
            Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // requestActivityUpdates() and removeActivityUpdates().
            mActivityDetectionPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return mActivityDetectionPendingIntent;
    }


}
