/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cahue.iweco.activityRecognition;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.R;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * IntentService for handling incoming intents that are generated as a result of requesting
 * activity updates using
 * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService {

    protected static final String TAG = DetectedActivitiesIntentService.class.getSimpleName();

    private static final String PREF_PREVIOUS_ACTIVITY_TYPE = "PREF_PREVIOUS_ACTIVITY_CONFIDENCE";
    private static final String PREF_PREVIOUS_ACTIVITY_CONFIDENCE = "PREF_PREVIOUS_ACTIVITY_CONFIDENCE";


    private static DetectedActivity previousActivity;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (previousActivity == null)
            previousActivity = getStoredDetectedActivity();
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
            handleDetectedActivities(result);
//            Intent localIntent = new Intent(Constants.INTENT_ACTIVITY_RECOGNIZED);
//            localIntent.putExtra(Constants.INTENT_EXTRA_ACTIVITIES_RESULT, result);
//
//            // Broadcast the list of detected activities.
//            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
        }
    }


    private void handleDetectedActivities(ActivityRecognitionResult result) {

        DetectedActivity mostProbableActivity = result.getMostProbableActivity();

        // Log each activity.
        Log.d(TAG, "Activities detected");
        for (DetectedActivity da : result.getProbableActivities()) {
            Log.v(TAG, getActivityString(da.getType()) + " " + da.getConfidence() + "%");
        }

        if (!isOnFoot(mostProbableActivity) && !isVehicleRelated(mostProbableActivity))
            return;

        if ((previousActivity == null || mostProbableActivity.getType() != previousActivity.getType())
                && mostProbableActivity.getConfidence() == 100) {

            if (BuildConfig.DEBUG) {
                showDebugNotification(result, mostProbableActivity);
            }

            // If switched to on foot, previously in vehicle
            if (isOnFoot(mostProbableActivity) && isVehicleRelated(previousActivity)) {
                // we create an intent to start the location poller service, as declared in manifest
                Intent intent = new Intent(this, ParkedCarRequestedService.class);
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

        mNotifyMgr.notify(null, 7908772, mBuilder.build());
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

    private boolean isVehicleRelated(DetectedActivity detectedActivity) {
        if (BuildConfig.DEBUG && detectedActivity.getType() == DetectedActivity.ON_BICYCLE)
            return true;
        return detectedActivity.getType() == DetectedActivity.IN_VEHICLE;
    }

    private boolean isOnFoot(DetectedActivity detectedActivity) {
        return detectedActivity.getType() == DetectedActivity.ON_FOOT;
    }


    private void savePreviousActivity(DetectedActivity previousActivity) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putInt(PREF_PREVIOUS_ACTIVITY_TYPE, previousActivity.getType())
                .putInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, previousActivity.getConfidence())
                .apply();
    }

    private DetectedActivity getStoredDetectedActivity() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int type = prefs.getInt(PREF_PREVIOUS_ACTIVITY_TYPE, DetectedActivity.UNKNOWN);
        int confidence = prefs.getInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, 0);
        return new DetectedActivity(type, confidence);
    }


}