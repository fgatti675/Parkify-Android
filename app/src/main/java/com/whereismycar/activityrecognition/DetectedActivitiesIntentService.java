package com.whereismycar.activityrecognition;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.whereismycar.BuildConfig;
import com.whereismycar.Constants;
import com.whereismycar.R;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import static com.whereismycar.util.NotificationChannelsUtils.DEBUG_CHANNEL_ID;

/**
 * IntentService for handling incoming intents that are generated as a result of requesting
 * activity updates
 */
public class DetectedActivitiesIntentService extends IntentService {

    private static final String TAG = "ActivityRecognition";


    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    /**
     * Handles incoming intents.
     *
     * @param intent The Intent is provided (inside a PendingIntent) when requestActivityUpdates()
     *               is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            Log.d(TAG, "Detected activity: " + mostProbableActivity);

            showDebugNotification(mostProbableActivity);

            // not probable enough
            if (mostProbableActivity.getConfidence() < 80) {
                return;
            }

            if (mostProbableActivity.getType() == DetectedActivity.ON_FOOT && mostProbableActivity.getConfidence() < 90) {
                return;
            }

            if (mostProbableActivity.getType() == DetectedActivity.TILTING || mostProbableActivity.getType() == DetectedActivity.UNKNOWN) {
                return;
            }

            Intent localIntent = new Intent(Constants.INTENT_ACTIVITY_CHANGED);
            localIntent.putExtra(Constants.EXTRA_ACTIVITY, mostProbableActivity);
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        }
    }

    private void showDebugNotification(DetectedActivity mostProbableActivity) {
        if (BuildConfig.DEBUG) {

            long[] pattern = {0, 100, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, DEBUG_CHANNEL_ID) : new Notification.Builder(this))
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.circle_primary)
                            .setColor(getResources().getColor(R.color.theme_primary))
                            .setContentTitle(mostProbableActivity.toString());

            mNotifyMgr.notify(null, (int) (Math.random() * 10000000), mBuilder.build());
        }
    }


}
