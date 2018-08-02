/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cahue.iweco.locationservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.google.android.gms.location.LocationResult;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Date;
import java.util.List;

/**
 * Receiver for handling location updates.
 * <p>
 * For apps targeting API level O
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should not be used.
 * <p>
 * Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 * less frequently than the interval specified in the
 * {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 * foreground.
 */
public abstract class AbstractLocationUpdatesBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "LUBroadcastReceiver";
    public static final int MINIMUM_ACCURACY = 25;
    public static final long MAX_DURATION = 20 * 1000;
    private Location location = null;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent != null) {

            final String action = intent.getAction();

            LocationResult result = LocationResult.extractResult(intent);

            // TODO: change to extras when this is fixed: https://issuetracker.google.com/issues/37013793
            Bundle extras = new Bundle();
            Uri data = intent.getData();
            Date startTime = new Date(Long.parseLong(data.getQueryParameter("startTime")));

            long elapsedTime = new Date().getTime() - startTime.getTime();
            extras.putSerializable(Constants.EXTRA_START_TIME, startTime);

            if (result != null) {
                String carId = data.getQueryParameter("car");
                extras.putString(Constants.EXTRA_CAR_ID, carId);
                onLocationChanged(context, action, extras, result.getLocations());
            } else {
                Log.w(TAG, "No location result");
            }

            if (elapsedTime > MAX_DURATION) {

                FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
                Bundle bundle = new Bundle();
                bundle.putBoolean("has_location", location != null);
                firebaseAnalytics.logEvent("location_timed_out", bundle);

                if (location == null) {
                    Log.w(TAG, "Timed out and no location");
                    stop(context);
                } else {
                    notifyFixLocationAndStop(context, extras, action, location);
                }
            }

            Log.d(TAG, "elapsed time: " + elapsedTime);

        }

    }


    private void onLocationChanged(Context context, String action, Bundle extras, @NonNull List<Location> locations) {

        Log.d(TAG, "onLocationChanged: " + locations);

        for (Location loc : locations) {
            Log.d(TAG, "Location polled: " + loc);
            if (location == null || loc.getAccuracy() < location.getAccuracy())
                location = loc;
        }

        if (location == null) {
            return;
        }

        if (location.hasAccuracy() && location.getAccuracy() < MINIMUM_ACCURACY) {
            notifyFixLocationAndStop(context, extras, action, location);
        }


    }

    private boolean notified = false;

    /**
     * Notify the location of the event (like user parked or drove off)
     *
     * @param location
     */
    private void notifyFixLocationAndStop(Context context, Bundle extras, String action, @NonNull Location location) {

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putInt("accuracy", (int) location.getAccuracy());
        firebaseAnalytics.logEvent("location_polled", bundle);

        Log.i(TAG, "Notifying location polled: " + location);
        Date startTime = (Date) extras.getSerializable(Constants.EXTRA_START_TIME);
        Log.i(TAG, "\tafter " + (System.currentTimeMillis() - startTime.getTime()) + " ms");
        if (!notified)
            onPreciseFixPolled(context, location, extras);
        notified = true;

        stop(context);

    }

    private void stop(Context context) {
        context.stopService(new Intent(context, AbstractLocationUpdatesService.class));
    }

    protected abstract void onPreciseFixPolled(Context context, Location location, Bundle extras);
}
