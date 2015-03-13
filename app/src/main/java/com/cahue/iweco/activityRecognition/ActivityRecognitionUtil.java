package com.cahue.iweco.activityRecognition;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cahue.iweco.util.Util;
import com.google.android.gms.location.DetectedActivity;

/**
 * Created by francesco on 13.03.2015.
 */
public class ActivityRecognitionUtil {

    public static final String INTENT_ACTIVITY_RECOGNIZED = "INTENT_ACTIVITY_RECOGNIZED";
    public static final String INTENT_EXTRA_ACTIVITY = "INTENT_EXTRA_ACTIVITY";

    // activity we are sure the user is doing (or almost)
    public static final String SURE_ACTIVITY_TYPE = "SURE_ACTIVITY_TYPE";
    public static final String SURE_ACTIVITY_CONFIDENCE = "SURE_ACTIVITY_CONFIDENCE";

    public static DetectedActivity getLastDetectedActivity(Context context) {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        int lastAssuredType = prefs.getInt(SURE_ACTIVITY_TYPE, DetectedActivity.UNKNOWN);
        int lastAssuredConfidence = prefs.getInt(SURE_ACTIVITY_CONFIDENCE, -1);

        return new DetectedActivity(lastAssuredType, lastAssuredConfidence);
    }

    public static  void setDetectedActivity(Context context, DetectedActivity detectedActivity) {

        Intent intent = new Intent(INTENT_ACTIVITY_RECOGNIZED);
        intent.putExtra(INTENT_EXTRA_ACTIVITY, detectedActivity);
        context.sendBroadcast(intent);

        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putInt(SURE_ACTIVITY_TYPE, detectedActivity.getType())
                .putInt(SURE_ACTIVITY_CONFIDENCE, detectedActivity.getConfidence())
                .apply();

    }
}
