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
import android.util.Log;

import java.util.LinkedList;

public class ActivityRecognitionService extends IntentService {

    public static final String INTENT_ACTIVITY_RECOGNIZED = "INTENT_ACTIVITY_RECOGNIZED";
    public static final String ACTIVITY_TYPE = "Activity";
    public static final String CONFIDENCE = "Confidence";
    private static final String LAST_ACTIVITY_TYPE = "LAST_ACTIVITY_TYPE";
    private static final String LAST_ACTIVITY_CONFIDENCE = "LAST_ACTIVITY_CONFIDENCE";

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
            int confidence = result.getMostProbableActivity().getConfidence();

            if (!isMovementRelated(type)) return;

            SharedPreferences prefs = Util.getSharedPreferences(this);

            int previousType = prefs.getInt(LAST_ACTIVITY_TYPE, -1);
            int previousConfidence = prefs.getInt(LAST_ACTIVITY_CONFIDENCE, -1);

            if (type != previousType) {

                String typeString = getType(type);

                Log.i(TAG, typeString + "\t" + result.getMostProbableActivity().getConfidence());
                Intent i = new Intent(INTENT_ACTIVITY_RECOGNIZED);
                i.putExtra(ACTIVITY_TYPE, type);
                i.putExtra(CONFIDENCE, result.getMostProbableActivity().getConfidence());
                sendBroadcast(i);

                String previousText = "Previous: " + getType(previousType) + " (" + previousConfidence + "%)\n";

                StringBuilder stringBuilder = new StringBuilder(previousText);
                for(DetectedActivity detectedActivity:result.getProbableActivities()) {
                    stringBuilder.append(getType(detectedActivity.getType()) + " (" + detectedActivity.getConfidence() + "%)\n");
                }

                long[] pattern = {0, 100, 1000};
                NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                Notification.Builder mBuilder =
                        new Notification.Builder(this)
                                .setVibrate(pattern)
                                .setSmallIcon(R.drawable.ic_navigation_cancel)
                                .setContentTitle(typeString + " (" + result.getMostProbableActivity().getConfidence() + "%)")
                                .setStyle(new Notification.BigTextStyle().bigText(stringBuilder.toString()))
                                .setContentText(previousText)
                                .setNumber(++count);

                int id = (int) Math.random();
                mNotifyMgr.notify("" + id, id, mBuilder.build());

                prefs.edit()
                        .putInt(LAST_ACTIVITY_TYPE, type)
                        .putInt(LAST_ACTIVITY_CONFIDENCE, confidence)
                        .apply();

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
        else if (type == DetectedActivity.WALKING)
            return "Walking";
        else if (type == DetectedActivity.RUNNING)
            return "Running";
        else
            return "";
    }

    private boolean isMovementRelated(int type) {
        switch (type) {
            case DetectedActivity.IN_VEHICLE:
            case DetectedActivity.ON_BICYCLE:
            case DetectedActivity.ON_FOOT:
            case DetectedActivity.STILL:
            case DetectedActivity.WALKING:
            case DetectedActivity.RUNNING:
                return true;
        }
        return false;
    }

    private static final String LAST_ACTIVITIES_COUNT_KEY = "LAST_ACTIVITIES_COUNT";
    private static final String LAST_ACTIVITIES_KEY_PREFIX = "LAST_ACTIVITIES_";
    private static final int LAST_ACTIVITIES_MAX_COUNT = 20;

    private LinkedList<Integer> getLastActivities(SharedPreferences prefs) {
        LinkedList<Integer> list = new LinkedList<>();
        int count = prefs.getInt(LAST_ACTIVITIES_COUNT_KEY, 0);
        for (int i = 0; i < count; i++) {
            list.add(prefs.getInt(LAST_ACTIVITIES_KEY_PREFIX + i, i));
        }
        return list;
    }

    private void putLastActivities(SharedPreferences prefs, LinkedList<Integer> list) {

        int i = 0;
        for (Integer value : list) {
            prefs.edit().putInt(LAST_ACTIVITIES_KEY_PREFIX + i, value);
            i++;
        }
        prefs.edit().putInt(LAST_ACTIVITIES_COUNT_KEY, list.size());
        prefs.edit().apply();
    }

}
