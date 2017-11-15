package com.cahue.iweco.activityrecognition;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.locationservices.LocationUpdatesHelper;
import com.cahue.iweco.locationservices.PossibleParkedCarReceiver;
import com.cahue.iweco.util.NotificationChannelsUtils;
import com.cahue.iweco.util.PreferencesUtil;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.cahue.iweco.util.NotificationChannelsUtils.DEBUG_CHANNEL_ID;
import static com.google.android.gms.location.DetectedActivity.IN_VEHICLE;


/**
 * Created by Francesco on 27/06/2015.
 */
public class ActivityRecognitionService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final int IN_CAR_DETECTION_JOB_ID = 1000;
    private static final long DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE = 4000;
    private static final String TAG = ActivityRecognitionService.class.getSimpleName();

    // How many times the user has been still in a row
    public int stillCounter = 0;
    public int vehicleCounter = 0;
    public int currentActivity = DetectedActivity.UNKNOWN;
    private GoogleApiClient mGoogleApiClient;
    /**
     * Used when requesting or removing activity detection updates.
     */
    private PendingIntent mActivityDetectionPendingIntent;
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onNewActivityDetected(intent.getParcelableExtra(Constants.EXTRA_ACTIVITY));
        }
    };

    /**
     * Only fetch updates if BT is off and the user has requested so
     *
     * @param context
     */
    public static void startCheckingActivityRecognition(@NonNull Context context) {
        addActivityRecognitionFences(context);
    }


    /**
     * Start checking periodically the awareness API for car detection
     */
    public static void addActivityRecognitionFences(Context context) {

        AwarenessFence vehicleDuringFence = DetectedActivityFence.during(getInVehicleRelatedActivities());
        AwarenessFence vehicleStartingFence = DetectedActivityFence.starting(getInVehicleRelatedActivities());

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();

        Intent intent = new Intent(context, InVehicleReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 7629, intent, FLAG_UPDATE_CURRENT);

        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Awareness.getFenceClient(context).updateFences(
                        new FenceUpdateRequest.Builder()
                                .addFence("vehicleDuringFenceKey", vehicleDuringFence, pendingIntent)
                                .addFence("vehicleStartingFenceKey", vehicleStartingFence, pendingIntent)
                                .build())
                        .addOnCompleteListener(task -> {
                            Log.i(TAG, "Fence was successfully registered.");
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Fence could not be registered: " + e);
                        });
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        });
        googleApiClient.connect();

    }

    private static int[] getInVehicleRelatedActivities() {
        if (BuildConfig.DEBUG)
            return new int[]{DetectedActivityFence.IN_VEHICLE, DetectedActivityFence.ON_BICYCLE};
        else
            return new int[]{DetectedActivityFence.IN_VEHICLE};

    }

    public static class InVehicleReceiver extends BroadcastReceiver {
        public InVehicleReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive: ");


            FenceState fenceState = FenceState.extract(intent);

            switch (fenceState.getCurrentState()) {
                case FenceState.TRUE:
                    Log.i(TAG, "In vehicle fence detected TRUE");
                    BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
                    if (defaultAdapter != null && !defaultAdapter.isEnabled() && PreferencesUtil.isMovementRecognitionEnabled(context)) {
                        startActivityRecognition(context);
                    }
                    break;
                case FenceState.FALSE:
                    Log.i(TAG, "In vehicle fence FALSE");
                    break;
                case FenceState.UNKNOWN:
                    Log.i(TAG, "In vehicle fence UNKNOWN");
                    break;
            }

            if (BuildConfig.DEBUG) {
                NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(context, NotificationChannelsUtils.DEBUG_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_action_action_settings_dark)
                                .setContentTitle("In vehicle fence fired");
                mNotifyMgr.notify(null, 6472837, mBuilder.build());
            }
        }
    }

    public static void startActivityRecognition(@NonNull Context context) {
        Intent intent = new Intent(context, ActivityRecognitionService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
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

        PendingIntent cancelActRecPendingIntent = PendingIntent.getBroadcast(
                this,
                74524,
                new Intent(this, StopActivityRecognitionBroadcastReceiver.class),
                PendingIntent.FLAG_CANCEL_CURRENT);

        startForeground(27572, new NotificationCompat.Builder(this, NotificationChannelsUtils.ACT_RECOG_CHANNEL_ID)
                .setContentTitle(getString(R.string.motion_recognition_in_progress))
                .setContentText(getString(R.string.motion_recognition_instructions))
                .setSmallIcon(R.drawable.ic_in_car)
                .setColor(getResources().getColor(R.color.theme_primary))
                .addAction(R.drawable.ic_exit_to_app_24dp, getString(R.string.stop), cancelActRecPendingIntent)
                .build());

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver, new IntentFilter(Constants.INTENT_ACTIVITY_CHANGED));

        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancel(IN_CAR_DETECTION_JOB_ID);

    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy");

        Log.i(TAG, "Stopping activity recognition");

        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(mGoogleApiClient, getActivityDetectionPendingIntent());

        if (BuildConfig.DEBUG) {
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, NotificationChannelsUtils.DEBUG_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_action_action_settings_dark)
                            .setContentTitle("Recognition Stopped");
            mNotifyMgr.notify(null, 6472837, mBuilder.build());
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        mGoogleApiClient.disconnect();

        ActivityRecognitionService.startCheckingActivityRecognition(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG, "onStartCommand");

        mGoogleApiClient.connect();

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onConnected(Bundle bundle) {

        Log.i(TAG, "Starting activity recognition : " + DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE);

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                mGoogleApiClient,
                DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE,
                getActivityDetectionPendingIntent()
        );
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
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

    private void onNewActivityDetected(DetectedActivity detectedActivity) {

        if (isStill(detectedActivity)) {
            stillCounter++;
            if (stillCounter > 6) {
                currentActivity = DetectedActivity.ON_FOOT;
                vehicleCounter = 0;
                handleIdle();
            }
        }

        // User in vehicle
        else if (isVehicleRelated(detectedActivity)) {
            vehicleCounter += detectedActivity.getConfidence() > 90 ? 2 : 1;
            stillCounter = 0;
            if (vehicleCounter > 3 && currentActivity != DetectedActivity.IN_VEHICLE) {
                currentActivity = DetectedActivity.IN_VEHICLE;
                handleFootToVehicle();
            }
        }

        // User on foot
        else if (isOnFoot(detectedActivity)) {
            vehicleCounter = 0;
            stillCounter = 0;
            if (currentActivity == DetectedActivity.IN_VEHICLE) {
                handleVehicleToFoot();
            }
            currentActivity = DetectedActivity.ON_FOOT;
        }

        // Log each activity.
        Log.d(TAG, "Activities detected: " + detectedActivity.toString());

    }

    private boolean isVehicleRelated(@NonNull DetectedActivity detectedActivity) {
        if (BuildConfig.DEBUG && detectedActivity.getType() == DetectedActivity.ON_BICYCLE)
            return true;
        return detectedActivity.getType() == IN_VEHICLE;
    }

    private boolean isOnFoot(@NonNull DetectedActivity detectedActivity) {
        return detectedActivity.getType() == DetectedActivity.ON_FOOT;
    }

    private boolean isStill(@NonNull DetectedActivity detectedActivity) {
        return detectedActivity.getType() == DetectedActivity.STILL && detectedActivity.getConfidence() > 90;
    }

    private void handleIdle() {
        Log.i(TAG, "handleIdle: ");
        stopSelf();
    }

    private void handleVehicleToFoot() {
        Log.i(TAG, "handleVehicleToFoot: ");

        LocationUpdatesHelper helper = new LocationUpdatesHelper(this, PossibleParkedCarReceiver.ACTION);
        helper.startLocationUpdates(null);

        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 100, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, DEBUG_CHANNEL_ID)
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.ic_access_time_black_18px)
                            .setColor(getResources().getColor(R.color.theme_primary))
                            .setContentTitle("Vehicle -> Foot");

            mNotifyMgr.notify(null, 564543, mBuilder.build());
        }

        stopSelf();
    }

    private void handleFootToVehicle() {

        Log.i(TAG, "handleFootToVehicle: ");

        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 100, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, DEBUG_CHANNEL_ID)
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.ic_access_time_black_18px)
                            .setColor(getResources().getColor(R.color.theme_primary))
                            .setContentTitle("Foot -> Vehicle - " + vehicleCounter);

            mNotifyMgr.notify(null, 564544, mBuilder.build());
        }
    }

    public static class StopActivityRecognitionBroadcastReceiver extends BroadcastReceiver {
        public StopActivityRecognitionBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "StopActivityRecognitionBroadcastReceiver: on stop clicked");
            context.stopService(new Intent(context, ActivityRecognitionService.class));
        }
    }


}
