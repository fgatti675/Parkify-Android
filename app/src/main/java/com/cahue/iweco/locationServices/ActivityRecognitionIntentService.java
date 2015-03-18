package com.cahue.iweco.locationServices;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.google.android.gms.location.ActivityRecognitionResult;

/**
 * Created by Francesco on 16/03/2015.
 */
public class ActivityRecognitionIntentService extends IntentService {

    private String TAG = this.getClass().getSimpleName();

    public static final String INTENT_ACTIVITY_DETECTED = BuildConfig.APPLICATION_ID + ".ACTIVITY_DETECTED";
    public static final String DETECTED_ACTIVITY_DATA_KEY = BuildConfig.APPLICATION_ID + INTENT_ACTIVITY_DETECTED + ".DETECTED_ACTIVITY_DATA_KEY";

    public ActivityRecognitionIntentService() {
        super("My Activity Recognition Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.v(TAG, "Activity intent received");

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult detectedActivity = ActivityRecognitionResult.extractResult(intent);

            Intent result = new Intent(INTENT_ACTIVITY_DETECTED);
            result.putExtra(DETECTED_ACTIVITY_DATA_KEY, detectedActivity.getMostProbableActivity());
            sendBroadcast(result);

            Log.v(TAG, detectedActivity.getMostProbableActivity().toString());

        } else {
            Log.w(TAG, "Activity NOT detected");
        }

    }

}
