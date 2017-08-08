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
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.google.android.gms.location.LocationResult;

import java.util.Date;
import java.util.List;

import static com.cahue.iweco.Constants.ACCURACY_THRESHOLD_M;
import static com.cahue.iweco.locationservices.LocationUpdatesHelper.PRECISE_FIX_TIMEOUT_MS;

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
    private Intent intent;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.intent = intent;
        if (intent != null) {

            final String action = intent.getAction();

            LocationResult result = LocationResult.extractResult(intent);

            if (result != null) {
//                onLocationChanged(context, action, intent.getBundleExtra(Constants.EXTRA_BUNDLE), result.getLocations());
                // TODO: change to extras when this is fixed: https://issuetracker.google.com/issues/37013793
                Bundle bundle = new Bundle();
                Uri data = intent.getData();
                bundle.putSerializable(Constants.EXTRA_START_TIME, new Date(Long.parseLong(data.getQueryParameter("startTime"))));
                String carId = data.getQueryParameter("car");
                bundle.putString(Constants.EXTRA_CAR_ID, carId);
                onLocationChanged(context, action, bundle, result.getLocations());
            } else {
                Log.e(TAG, "No location result");
            }
        }
    }

    public void onLocationChanged(Context context, String action, Bundle extras, @NonNull List<Location> locations) {

        Log.d(TAG, "onLocationChanged: " + locations);

        Location location = null;

        for (Location loc : locations) {
            Log.d(TAG, "Location polled: " + loc);
            if (location == null || loc.getAccuracy() < location.getAccuracy())
                location = loc;
        }
        if (location == null) {
            return;
        }

        Date now = new Date();

//        // do nothing before a few seconds
//        if (now.getTime() - startTime.getTime() < LocationUpdatesHelper.MINIMUM_TIME_MS) {
//            Log.d(TAG, "Doing nothing because not enough time passed");
//            return;
//        }

        Log.v(TAG, location.toString());

        if (location.getAccuracy() < ACCURACY_THRESHOLD_M) {
            notifyFixLocationAndStop(context, extras, action, location);
            return;
        }

        Location bestAccuracyLocation = getBestAccuracyLocation(context);

        if (bestAccuracyLocation == null || location.getAccuracy() < bestAccuracyLocation.getAccuracy()) {
            saveBestAccuracyLocation(context, location);
        }

        Date startTime = (Date) extras.getSerializable(Constants.EXTRA_START_TIME);
        if (now.getTime() - startTime.getTime() > PRECISE_FIX_TIMEOUT_MS) {
            notifyFixLocationAndStop(context, extras, action, bestAccuracyLocation);
        }

    }

    private void saveBestAccuracyLocation(Context context, Location location) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (location == null) {
            sharedPreferences.edit().remove("LOCATION_LAT").apply();
            sharedPreferences.edit().remove("LOCATION_LON").apply();
            sharedPreferences.edit().remove("LOCATION_PROVIDER").apply();
            sharedPreferences.edit().remove("LOCATION_ACC").apply();
        } else {
            sharedPreferences.edit().putFloat("LOCATION_LAT", (float) location.getLatitude()).apply();
            sharedPreferences.edit().putFloat("LOCATION_LON", (float) location.getLongitude()).apply();
            sharedPreferences.edit().putFloat("LOCATION_ACC", location.getAccuracy()).apply();
            sharedPreferences.edit().putString("LOCATION_PROVIDER", location.getProvider()).apply();
        }
    }

    private Location getBestAccuracyLocation(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Float lat = sharedPreferences.getFloat("LOCATION_LAT", -1);
        Float lon = sharedPreferences.getFloat("LOCATION_LON", -1);
        Float acc = sharedPreferences.getFloat("LOCATION_ACC", -1);
        Location location = null;
        if (lat != -1 && lon != -1) {
            String provider = sharedPreferences.getString("LOCATION_PROVIDER", null);
            location = new Location(provider);
            location.setLatitude(lat);
            location.setLongitude(lon);
            location.setAccuracy(acc);
        }
        return location;
    }

    /**
     * Notify the location of the event (like user parked or drove off)
     *
     * @param location
     */
    private void notifyFixLocationAndStop(Context context, Bundle extras, String action, @NonNull Location location) {
        Log.i(TAG, "Notifying location polled: " + location);
        Date startTime = (Date) extras.getSerializable(Constants.EXTRA_START_TIME);
        Log.i(TAG, "\tafter " + (System.currentTimeMillis() - startTime.getTime()) + " ms");
        onPreciseFixPolled(context, location, extras);
        new LocationUpdatesHelper(context, action).stopLocationUpdates(intent);
    }

    protected abstract void onPreciseFixPolled(Context context, Location location, Bundle extras);
}
