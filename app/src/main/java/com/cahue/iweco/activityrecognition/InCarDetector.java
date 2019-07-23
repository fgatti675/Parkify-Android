package com.cahue.iweco.activityrecognition;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;


/**
 * Created by f.gatti.gomez on 17.08.17.
 */

public class InCarDetector {

    private static final String TAG = InCarDetector.class.getSimpleName();

    public void startActivityDetectionIfOnCar(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(ActivityRecognition.API)
                .build();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Awareness.SnapshotApi.getDetectedActivity(googleApiClient)
                        .setResultCallback(detectedActivityResult -> {
                            if (!detectedActivityResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Could not get the current activity.");
                                return;
                            }
                            ActivityRecognitionResult ar = detectedActivityResult.getActivityRecognitionResult();
                            DetectedActivity probableActivity = ar.getMostProbableActivity();
                            Log.i(TAG, probableActivity.toString());
                        });
                googleApiClient.disconnect();
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        });
        googleApiClient.blockingConnect();
    }
}
