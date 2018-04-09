package com.cahue.iweco.activityrecognition;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.locationservices.LocationUpdatesHelper;
import com.cahue.iweco.locationservices.PossibleParkedCarReceiver;
import com.cahue.iweco.util.PreferencesUtil;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.fence.AwarenessFence;
import com.google.android.gms.awareness.fence.DetectedActivityFence;
import com.google.android.gms.awareness.fence.FenceState;
import com.google.android.gms.awareness.fence.FenceUpdateRequest;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;

import static android.app.Notification.PRIORITY_MIN;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.cahue.iweco.util.NotificationChannelsUtils.ACT_RECOG_CHANNEL_ID;
import static com.cahue.iweco.util.NotificationChannelsUtils.DEBUG_CHANNEL_ID;
import static com.google.android.gms.location.DetectedActivity.IN_VEHICLE;


/**
 * Created by Francesco on 27/06/2015.
 */
public class ActivityRecognitionService extends Service {

    public static final int IN_CAR_DETECTION_JOB_ID = 1000;
    private static final long DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE = 0;
    private static final String TAG = ActivityRecognitionService.class.getSimpleName();

    // How many times the user has been still in a row
    public int stillCounter = 0;
    public int vehicleCounter = 0;
    public int currentActivity = DetectedActivity.UNKNOWN;

    ActivityRecognitionClient activityRecognitionClient;

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
     * Start checking periodically the awareness API for car detection
     *
     * @param context
     */
    public static void startCheckingActivityRecognition(@NonNull Context context) {

        AwarenessFence vehicleDuringFence = DetectedActivityFence.during(getInVehicleRelatedActivities());
        AwarenessFence vehicleStartingFence = DetectedActivityFence.starting(getInVehicleRelatedActivities());

        Intent intent = new Intent(context, InVehicleReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 7629, intent, FLAG_UPDATE_CURRENT);

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

                    if (PreferencesUtil.isMovementRecognitionEnabled(context)) {
                        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (defaultAdapter != null) {
                            if (!defaultAdapter.isEnabled()
                                    ||
                                    defaultAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) != BluetoothHeadset.STATE_CONNECTED) {
                                startActivityRecognition(context);
                            }
                        }
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
                Notification.Builder mBuilder =
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, DEBUG_CHANNEL_ID) : new Notification.Builder(context))
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

        activityRecognitionClient = ActivityRecognition.getClient(this);
        Log.i(TAG, "Starting activity recognition : " + DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE);

        activityRecognitionClient.requestActivityUpdates(
                DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE,
                getActivityDetectionPendingIntent()
        );

        PendingIntent cancelActRecPendingIntent = PendingIntent.getBroadcast(
                this,
                74524,
                new Intent(this, StopActivityRecognitionBroadcastReceiver.class),
                PendingIntent.FLAG_CANCEL_CURRENT);

        startForeground(27572, (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, ACT_RECOG_CHANNEL_ID) : new Notification.Builder(this))
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

        activityRecognitionClient.removeActivityUpdates(getActivityDetectionPendingIntent());

        if (BuildConfig.DEBUG) {
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, DEBUG_CHANNEL_ID) : new Notification.Builder(this))
                            .setSmallIcon(R.drawable.ic_action_action_settings_dark)
                            .setContentTitle("Recognition Stopped");
            mNotifyMgr.notify(null, 6472837, mBuilder.build());
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        ActivityRecognitionService.startCheckingActivityRecognition(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.v(TAG, "onStartCommand");

        return Service.START_NOT_STICKY;
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
            if (vehicleCounter > 2 && currentActivity != DetectedActivity.IN_VEHICLE) {
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

        if (BuildConfig.DEBUG)
            showDebugNotification(detectedActivity);

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
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, DEBUG_CHANNEL_ID) : new Notification.Builder(this))
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
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, DEBUG_CHANNEL_ID) : new Notification.Builder(this))
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.ic_access_time_black_18px)
                            .setColor(getResources().getColor(R.color.theme_primary))
                            .setContentTitle("Foot -> Vehicle - " + vehicleCounter);

            mNotifyMgr.notify(null, 564544, mBuilder.build());
        }
    }

    private void showDebugNotification(@NonNull DetectedActivity mostProbableActivity) {
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, DEBUG_CHANNEL_ID) : new Notification.Builder(this))
                        .setPriority(PRIORITY_MIN)
                        .setSmallIcon(R.drawable.ic_navigation_cancel)
                        .setContentTitle(mostProbableActivity.toString())
                        .setContentText("V: " + vehicleCounter + " S: " + stillCounter + " Curr:" + new DetectedActivity(currentActivity, 100));

        mNotifyMgr.notify(null, 7908772, mBuilder.build());
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
