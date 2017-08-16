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

package com.cahue.iweco.activityrecognition;

import android.app.IntentService;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.locationservices.LocationUpdatesHelper;
import com.cahue.iweco.locationservices.PossibleParkedCarReceiver;
import com.cahue.iweco.util.PreferencesUtil;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import static android.app.Notification.PRIORITY_MIN;
import static com.google.android.gms.location.DetectedActivity.IN_VEHICLE;

/**
 * IntentService for handling incoming intents that are generated as a result of requesting
 * activity updates using
 * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService {

    private static final String TAG = "Activity recognition";

    private static final String PREF_CURRENT_ACTIVITY_TYPE = "PREF_CURRENT_ACTIVITY_TYPE";
    private static final String PREF_STILL_COUNTER = "PREF_STILL_COUNTER";
    private static final String PREF_VEHICLE_COUNTER = "PREF_VEHICLE_COUNTER";

    // How many times the user has been still in a row
    public int stillCounter = 0;
    public int vehicleCounter = 0;

    public int currentActivity = DetectedActivity.UNKNOWN;

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

            restoreCurrentState();

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            Log.d(TAG, "Detected activity: " + mostProbableActivity);

            // not probable enough
            if (mostProbableActivity.getConfidence() < 75) {
                return;
            }

            // not probable enough
            if (mostProbableActivity.getType() == DetectedActivity.STILL && mostProbableActivity.getConfidence() < 80) {
                return;
            }

            if (mostProbableActivity.getType() == DetectedActivity.TILTING || mostProbableActivity.getType() == DetectedActivity.UNKNOWN) {
                return;
            }

            onNewActivityDetected(mostProbableActivity);

            saveCurrentState();

        }
    }

    private void onNewActivityDetected(DetectedActivity detectedActivity) {

        if (isStill(detectedActivity)) {
            stillCounter++;
            if (stillCounter > 5) {
                currentActivity = DetectedActivity.ON_FOOT;
                vehicleCounter = 0;
            }
//            ActivityRecognitionService.startIfEnabled(this);
        }

        // User in vehicle
        else if (isVehicleRelated(detectedActivity)) {
            vehicleCounter += detectedActivity.getConfidence() > 90 ? 2 : 1;
            stillCounter = 0;
            if (vehicleCounter > 5 && currentActivity != DetectedActivity.IN_VEHICLE) {
                currentActivity = DetectedActivity.IN_VEHICLE;
                handleFootToVehicle();
            }
//            ActivityRecognitionService.startIfEnabledFastRecognition(this);
        }

        // User on foot
        else if (isOnFoot(detectedActivity)) {
            vehicleCounter = 0;
            stillCounter = 0;
            if (currentActivity == DetectedActivity.IN_VEHICLE) {
                handleVehicleToFoot();
            }
            currentActivity = DetectedActivity.ON_FOOT;
//            ActivityRecognitionService.startIfEnabled(this);
        }

        // Log each activity.
        Log.d(TAG, "Activities detected: " + detectedActivity.toString());

        if (BuildConfig.DEBUG) {
            showDebugNotification(detectedActivity);
        }

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


    private void handleVehicleToFoot() {
        // notify change
        Intent activityChangedIntent = new Intent(Constants.INTENT_ACTIVITY_CHANGED);
        activityChangedIntent.setAction(Constants.ACTION_VEHICLE_TO_FOOT);
        sendBroadcast(activityChangedIntent);

        LocationUpdatesHelper helper = new LocationUpdatesHelper(this, PossibleParkedCarReceiver.ACTION);
        helper.startLocationUpdates(null);

        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 100, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.ic_access_time_black_18px)
                            .setColor(getResources().getColor(R.color.theme_primary))
                            .setContentTitle("Vehicle -> Foot");

            mNotifyMgr.notify(null, 564543, mBuilder.build());
        }
    }

    private void handleFootToVehicle() {

        // notify change
        Intent activityChangedIntent = new Intent(Constants.INTENT_ACTIVITY_CHANGED);
        activityChangedIntent.setAction(Constants.ACTION_FOOT_TO_VEHICLE);
        sendBroadcast(activityChangedIntent);

        // enable BT is requested in preferences
        if (PreferencesUtil.isBtOnEnteringVehicleEnabled(this)) {
            BluetoothAdapter.getDefaultAdapter().enable();
            ActivityRecognitionService.stop(this);
        }

        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 100, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.ic_access_time_black_18px)
                            .setColor(getResources().getColor(R.color.theme_primary))
                            .setContentTitle("Foot -> Vehicle - " + vehicleCounter);

            mNotifyMgr.notify(null, 564544, mBuilder.build());
        }
    }

//
//    private void showDebugNotification(@NonNull DetectedActivity mostProbableActivity) {
//        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this, DEBUG_CHANNEL_ID)
//                        .setPriority(PRIORITY_MIN)
//                        .setSmallIcon(R.drawable.ic_navigation_cancel)
//                        .setContentTitle(mostProbableActivity.toString())
//                        .setContentText(String.valueOf(mostProbableActivity.getConfidence()));
//
//        mNotifyMgr.notify(null, 7908772, mBuilder.build());
//    }

    private void showDebugNotification(@NonNull DetectedActivity mostProbableActivity) {
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setPriority(PRIORITY_MIN)
                        .setSmallIcon(R.drawable.ic_navigation_cancel)
                        .setContentTitle(mostProbableActivity.toString())
                        .setContentText("V: " + vehicleCounter + " S: " + stillCounter +" Curr:" + new DetectedActivity(currentActivity, 100));

        mNotifyMgr.notify(null, 7908772, mBuilder.build());
    }


    private void saveCurrentState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit()
                .putInt(PREF_CURRENT_ACTIVITY_TYPE, currentActivity)
                .putInt(PREF_STILL_COUNTER, stillCounter)
                .putInt(PREF_VEHICLE_COUNTER, vehicleCounter)
                .apply();
    }

    private void restoreCurrentState() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        currentActivity = prefs.getInt(PREF_CURRENT_ACTIVITY_TYPE, DetectedActivity.UNKNOWN);
        stillCounter = prefs.getInt(PREF_STILL_COUNTER, 0);
        vehicleCounter = prefs.getInt(PREF_VEHICLE_COUNTER, 0);
    }


}
