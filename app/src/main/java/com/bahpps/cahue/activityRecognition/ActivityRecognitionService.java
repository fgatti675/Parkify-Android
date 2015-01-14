package com.bahpps.cahue.activityRecognition;

import com.bahpps.cahue.R;
import com.bahpps.cahue.util.Util;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.Date;

public class ActivityRecognitionService extends IntentService {

    public static final String INTENT_ACTIVITY_RECOGNIZED = "INTENT_ACTIVITY_RECOGNIZED";
    public static final String ACTIVITY_TYPE = "Activity";
    public static final String CONFIDENCE = "Confidence";
    private static final String LAST_ACTIVITY_TYPE = "LAST_ACTIVITY_TYPE";

    private String TAG = this.getClass().getSimpleName();

    public ActivityRecognitionService() {
        super("My Activity Recognition Service");
    }

    private static int count = 0;

    @Override
    protected void onHandleIntent(Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            int type = result.getMostProbableActivity().getType();

            if (!isMovementRelated(type)) return;

            SharedPreferences prefs = Util.getSharedPreferences(this);

            int previousType = prefs.getInt(LAST_ACTIVITY_TYPE, -1);

            if (type != previousType) {

                String typeString = getType(type);

                Log.i(TAG, typeString + "\t" + result.getMostProbableActivity().getConfidence());
                Intent i = new Intent(INTENT_ACTIVITY_RECOGNIZED);
                i.putExtra(ACTIVITY_TYPE, type);
                i.putExtra(CONFIDENCE, result.getMostProbableActivity().getConfidence());
                sendBroadcast(i);

                long[] pattern = {0, 100, 1000};
                NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification.Builder mBuilder =
                        new Notification.Builder(this)
                                .setVibrate(pattern)
                                .setSmallIcon(R.drawable.ic_navigation_cancel)
                                .setContentTitle(typeString + " (" + result.getMostProbableActivity().getConfidence() + "%)")
                                .setContentText("Previous: " + getType(previousType))
                                .setNumber(++count);

                int id = (int) Math.random();
                mNotifyMgr.notify("" + id, id, mBuilder.build());

                prefs.edit().putInt(LAST_ACTIVITY_TYPE, type).apply();

            }
        }
    }

    private String getType(int type) {
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
        else
            return "";
    }

    private boolean isMovementRelated(int type) {
        switch (type) {
            case DetectedActivity.IN_VEHICLE:
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.STILL:
                return true;
        }
        return false;
    }

}
