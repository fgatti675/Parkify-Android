package com.cahue.iweco.locationServices;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.cahue.iweco.BuildConfig;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
* Created by Francesco on 16/03/2015.
*/
public class ActivityRecognitionIntentService extends IntentService {

    private String TAG = this.getClass().getSimpleName();

    public static final int SUCCESS_RESULT = 0;

    public static final String RECEIVER = BuildConfig.APPLICATION_ID + ".GET_ACTIVITY";
    public static final String DETECTED_ACTIVITY_DATA_KEY = BuildConfig.APPLICATION_ID + RECEIVER + ".DETECTED_ACTIVITY_DATA_KEY";

    public ActivityRecognitionIntentService() {
        super("My Activity Recognition Service");
    }

    protected ResultReceiver mReceiver;

    @Override
    protected void onHandleIntent(Intent intent) {

        mReceiver = intent.getParcelableExtra(RECEIVER);

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult detectedActivity = ActivityRecognitionResult.extractResult(intent);

            Bundle bundle = new Bundle();
            bundle.putParcelable(DETECTED_ACTIVITY_DATA_KEY, detectedActivity.getMostProbableActivity());
            mReceiver.send(SUCCESS_RESULT, bundle);
        }

    }


}
