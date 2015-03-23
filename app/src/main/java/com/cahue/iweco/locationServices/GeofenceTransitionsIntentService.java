package com.cahue.iweco.locationServices;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;

/**
 * Used to see when a user gets close to a Car
 */
public class GeofenceTransitionsIntentService extends IntentService {

    private static final String TAG = GeofenceTransitionsIntentService.class.getSimpleName();

    public static final String RECEIVER = BuildConfig.APPLICATION_ID + ".GET_GEOFENCE";
    public static final String GEOFENCE_TRANSITION_KEY = RECEIVER + ".GEOFENCE_TRANSITION";
    public static final String GEOFENCE_TRIGGERING_LOCATION_KEY = RECEIVER + ".GEOFENCE_EVENT";

    public static final int SUCCESS_RESULT = 0;
    public static final int ERROR_RESULT = 1;

    protected ResultReceiver mReceiver;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public GeofenceTransitionsIntentService() {
        super("My Geofence Service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "Geofence intent received");

        mReceiver = intent.getParcelableExtra(RECEIVER);

        Bundle bundle = new Bundle();

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofence error: " + geofencingEvent.getErrorCode());
            mReceiver.send(ERROR_RESULT, bundle);
            return;
        }

        bundle.putInt(GEOFENCE_TRANSITION_KEY, geofencingEvent.getGeofenceTransition());
        Location location = geofencingEvent.getTriggeringLocation();
        bundle.putParcelable(GEOFENCE_TRIGGERING_LOCATION_KEY, location);
        mReceiver.send(SUCCESS_RESULT, bundle);

    }


}
