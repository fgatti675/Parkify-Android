package com.cahue.iweco.activityrecognition;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.R;
import com.cahue.iweco.util.PreferencesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;

import static android.content.Context.NOTIFICATION_SERVICE;


/**
 * Created by Francesco on 27/06/2015.
 */
public class ActivityRecognitionService {

    private static final int DETECTION_INTERVAL_IN_MILLISECONDS_ON_FOOT = 15000;
    private static final int DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE = 4000;

    private static final String TAG = "Activity recognition";


    /**
     * Only fetch updates if BT is off and the user has requested so
     *
     * @param context
     */
    public static void startIfNoBT(@NonNull Context context) {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (defaultAdapter != null && !defaultAdapter.isEnabled()) {
            startIfEnabled(context);
        }
    }

    public static void startIfEnabled(@NonNull Context context) {
        startIfEnabled(context, DETECTION_INTERVAL_IN_MILLISECONDS_ON_FOOT);
    }

    public static void startIfEnabledFastRecognition(@NonNull Context context) {
        startIfEnabled(context, DETECTION_INTERVAL_IN_MILLISECONDS_IN_VEHICLE);

    }

    private static void startIfEnabled(@NonNull final Context context, final int detectionInterval) {

        if (!PreferencesUtil.isMovementRecognitionEnabled(context)) {
            return;
        }

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context);
        builder.addApi(ActivityRecognition.API);
        final GoogleApiClient googleApiClient = builder.build();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Log.i(TAG, "Starting activity recognition : " + detectionInterval);

                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                        googleApiClient,
                        detectionInterval,
                        getActivityDetectionPendingIntent(context)
                );

                if (BuildConfig.DEBUG) {

                    NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_action_action_settings_dark)
                                    .setContentTitle("Recognition Activated")
                                    .setContentText(String.valueOf(detectionInterval));
                    mNotifyMgr.notify(null, 6472837, mBuilder.build());
                }

                googleApiClient.disconnect();
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        });
        googleApiClient.connect();

    }

    public static void stop(@NonNull final Context context) {
        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context);
        builder.addApi(ActivityRecognition.API);
        final GoogleApiClient googleApiClient = builder.build();
        googleApiClient.registerConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                Log.i(TAG, "Stopping activity recognition");

                ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(googleApiClient, getActivityDetectionPendingIntent(context));

                if (BuildConfig.DEBUG) {
                    NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_action_action_settings_dark)
                                    .setContentTitle("Recognition Stopped");
                    mNotifyMgr.notify(null, 6472837, mBuilder.build());
                }
            }

            @Override
            public void onConnectionSuspended(int i) {

            }
        });
        googleApiClient.connect();
    }



    private static PendingIntent getActivityDetectionPendingIntent(Context context) {
        Intent intent = new Intent(context, DetectedActivitiesIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


}
