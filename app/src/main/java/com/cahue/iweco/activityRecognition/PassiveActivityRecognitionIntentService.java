package com.cahue.iweco.activityRecognition;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.R;
import com.cahue.iweco.util.Util;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.LinkedList;

public class PassiveActivityRecognitionIntentService extends IntentService {

    private String TAG = this.getClass().getSimpleName();

    public PassiveActivityRecognitionIntentService() {
        super("My Activity Recognition Service");
    }

    private static int lastActivityType = DetectedActivity.UNKNOWN;

    @Override
    protected void onHandleIntent(Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            int type = mostProbableActivity.getType();
            int confidence = mostProbableActivity.getConfidence();

            if (!isMovementRelated(type)) return;

            DetectedActivity previousActivity = ActivityRecognitionUtil.getLastDetectedActivity(this);

            if (BuildConfig.DEBUG) {

                if (type == lastActivityType && type != previousActivity.getType() && confidence > 95) {

                    String typeString = getTypeAsText(type);

                    String previousText = "Previous: " + getTypeAsText(previousActivity.getType()) + " (" + previousActivity.getConfidence() + "%)\n";

                    StringBuilder stringBuilder = new StringBuilder(previousText);
                    for (DetectedActivity detectedActivity : result.getProbableActivities()) {
                        stringBuilder.append(getTypeAsText(detectedActivity.getType()) + " (" + detectedActivity.getConfidence() + "%)\n");
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

                    ActivityRecognitionUtil.setDetectedActivity(this, mostProbableActivity);

                }
            }

            lastActivityType = type;
        }
    }


    private String getTypeAsText(int type) {
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
