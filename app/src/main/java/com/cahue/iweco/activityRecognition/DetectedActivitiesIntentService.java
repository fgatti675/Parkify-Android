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
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * IntentService for handling incoming intents that are generated as a result of requesting
 * activity updates using
 * {@link com.google.android.gms.location.ActivityRecognitionApi#requestActivityUpdates}.
 */
public class DetectedActivitiesIntentService extends IntentService {

    static final String PREF_CURRENT_ACTIVITY_TYPE = "PREF_CURRENT_ACTIVITY_TYPE";
    static final String PREF_STILL_COUNTER = "PREF_STILL_COUNTER";
    static final String PREF_VEHICLE_COUNTER = "PREF_VEHICLE_COUNTER";
    private static final String TAG = "Activity recognition";


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

            Intent localIntent = new Intent(Constants.INTENT_ACTIVITY_CHANGED);
            localIntent.putExtra(Constants.EXTRA_ACTIVITY, mostProbableActivity);
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);


        }
    }


}
