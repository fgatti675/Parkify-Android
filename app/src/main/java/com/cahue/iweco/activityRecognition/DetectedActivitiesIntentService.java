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
import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.util.PreferencesUtil;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * IntentService for handling incoming intents that are generated as a result of requesting
 * activity updates using
 * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService {

    private static final String TAG = "Activity recognition";

    private static final String PREF_PREVIOUS_ACTIVITY_TYPE = "PREF_PREVIOUS_ACTIVITY_TYPE";
    private static final String PREF_PREVIOUS_ACTIVITY_CONFIDENCE = "PREF_PREVIOUS_ACTIVITY_CONFIDENCE";


    @Nullable
    private static DetectedActivity previousActivity;

    // How many times the user has been still in a row
    private static int stillCounter = 0;
    private static int vehicleCounter = 0;

    // flag to indicate is definitely in a vehicle, not caused by false positives
    private static boolean definitelyInAVehicle = false;

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
        Log.v(TAG, "Activity recognition intent");

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity = result.getMostProbableActivity();

            // If not on foot or in vehicle we are not interested
            if (!isOnFoot(mostProbableActivity) && !isVehicleRelated(mostProbableActivity))
                return;

//            if (previousActivity == null)
//                previousActivity = getStoredDetectedActivity();

            // Check if still
//        if (isStill(mostProbableActivity)) {
//            stillCounter++;
//            if (stillCounter > 5) {
//                // reset if still for too long
//                previousActivity = null;
//                savePreviousActivity(null);
//            }
//        } else {
//            stillCounter = 0;
//        }

            // check if really in a vehicle
            if (isVehicleRelated(mostProbableActivity)) {
                vehicleCounter++;
                if (vehicleCounter > 5) definitelyInAVehicle = true;
                ActivityRecognitionService.startIfEnabledFastRecognition(this);
            } else {
                vehicleCounter = 0;
                ActivityRecognitionService.startIfEnabled(this);
            }

            // Log each activity.
            Log.d(TAG, "Activities detected: " + mostProbableActivity.toString());
            for (DetectedActivity da : result.getProbableActivities()) {
                Log.v(TAG, da.toString());
            }

            if (BuildConfig.DEBUG) {
                showDebugNotification(mostProbableActivity);
            }

            if ((previousActivity == null || mostProbableActivity.getType() != previousActivity.getType())
                    && mostProbableActivity.getConfidence() >= 75) {

                if (previousActivity != null) {

                    // Vehicle --> Foot
                    if (isVehicleRelated(previousActivity)  && isOnFoot(mostProbableActivity) && definitelyInAVehicle) {
                        handleVehicleToFoot();
                    }
                    // Foot --> Vehicle
                    else if (isOnFoot(previousActivity) && isVehicleRelated(mostProbableActivity)) {
                        handleFootToVehicle();
                    }

                    // reset this
                    definitelyInAVehicle = false;
                }

                previousActivity = mostProbableActivity;

//                savePreviousActivity(previousActivity);
            }

        }
    }

    private void handleVehicleToFoot() {
        // notify change
        Intent activityChangedIntent = new Intent(Constants.INTENT_ACTIVITY_CHANGED);
        activityChangedIntent.setAction(Constants.ACTION_VEHICLE_TO_FOOT);
        sendBroadcast(activityChangedIntent);

        ActivityRecognitionService.startIfEnabled(this);

        // start the location poller service, as declared in manifest
        this.startService(new Intent(this, PossibleParkedCarService.class));

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

        ActivityRecognitionService.startIfEnabledFastRecognition(this);

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
                            .setContentTitle("Foot -> Vehicle");

            mNotifyMgr.notify(null, 564544, mBuilder.build());
        }
    }


    private void showDebugNotification(@NonNull DetectedActivity mostProbableActivity) {
        String previousText = previousActivity != null ?
                "Previous: " + previousActivity.toString() + "\n" :
                "Previous unknown\n";

        long[] pattern = {0, 100, 1000};
        NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification.Builder mBuilder =
                new Notification.Builder(this)
                        .setVibrate(pattern)
                        .setSmallIcon(R.drawable.ic_navigation_cancel)
                        .setContentTitle(mostProbableActivity.toString())
                        .setContentText(previousText);

        mNotifyMgr.notify(null, 7908772, mBuilder.build());
    }

    private boolean isStill(@NonNull DetectedActivity detectedActivity) {
        return detectedActivity.getType() == DetectedActivity.STILL && detectedActivity.getConfidence() > 90;
    }

    private boolean isVehicleRelated(@NonNull DetectedActivity detectedActivity) {
        if (BuildConfig.DEBUG && detectedActivity.getType() == DetectedActivity.ON_BICYCLE)
            return true;
        return detectedActivity.getType() == DetectedActivity.IN_VEHICLE;
    }

    private boolean isOnFoot(@NonNull DetectedActivity detectedActivity) {
        return detectedActivity.getType() == DetectedActivity.ON_FOOT;
    }


//    private void savePreviousActivity(@Nullable DetectedActivity previousActivity) {
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        if (previousActivity != null) {
//            prefs.edit()
//                    .putInt(PREF_PREVIOUS_ACTIVITY_TYPE, previousActivity.getType())
//                    .putInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, previousActivity.getConfidence())
//                    .apply();
//        } else {
//            prefs.edit()
//                    .remove(PREF_PREVIOUS_ACTIVITY_TYPE)
//                    .remove(PREF_PREVIOUS_ACTIVITY_CONFIDENCE)
//                    .apply();
//        }
//    }
//
//    private DetectedActivity getStoredDetectedActivity() {
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//        if (!prefs.contains(PREF_PREVIOUS_ACTIVITY_TYPE)) return null;
//        int type = prefs.getInt(PREF_PREVIOUS_ACTIVITY_TYPE, -1);
//        int confidence = prefs.getInt(PREF_PREVIOUS_ACTIVITY_CONFIDENCE, 0);
//        return new DetectedActivity(type, confidence);
//    }


}