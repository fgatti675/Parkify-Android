package com.whereismycar;

import com.whereismycar.BuildConfig;

/**
 * Created by francesco on 13.03.2015.
 */
public class Constants {

    /**
     * Minimum desired accuracy
     */
    public final static int ACCURACY_THRESHOLD_M = 30;

    /**
     * Only post locations if the car has stopped for at least a few seconds
     */
    public static final long MINIMUM_STAY_MS = 60 * 1000;

    /**
     * Distance at which we can set a geofence in the car after we parked, so that we know
     * when the user is approaching it.
     */
    public static final float PARKED_DISTANCE_THRESHOLD = 90;
    public static final float GEOFENCE_RADIUS_IN_METERS = 60;

    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 48 * 60 * 60 * 1000;

    // action telling the main activity that activity recognition has a possible spot
    public static final String ACTION_POSSIBLE_PARKED_CAR = BuildConfig.APPLICATION_ID + ".ACTION_POSSIBLE_PARKED_CAR";

    public static final String ACTION_STOP_ACTIVITY_RECOGNITION = BuildConfig.APPLICATION_ID + ".ACTION_STOP_ACTIVITY_RECOGNITION";

    public static final String EXTRA_LOCATION_RECEIVER = BuildConfig.APPLICATION_ID + ".INTENT_LOCATION_RECEIVER";
    public static final String EXTRA_LOCATION = BuildConfig.APPLICATION_ID + ".INTENT_EXTRA_LOCATION";

    // extra fields
    public static final String EXTRA_CAR_ID = BuildConfig.APPLICATION_ID + ".INTENT_CAR_EXTRA_ID";
    public static final String EXTRA_CAR_ADDRESS = BuildConfig.APPLICATION_ID + ".INTENT_CAR_EXTRA_ADDRESS";
    public static final String EXTRA_CAR_LOCATION = BuildConfig.APPLICATION_ID + ".INTENT_CAR_EXTRA_LOCATION";
    public static final String EXTRA_CAR_TIME = BuildConfig.APPLICATION_ID + ".INTENT_CAR_EXTRA_TIME";

    public static final String EXTRA_SPOT = BuildConfig.APPLICATION_ID + ".INTENT_SPOT_EXTRA";
    public static final String EXTRA_ACTIVITY = BuildConfig.APPLICATION_ID + ".INTENT_SPOT_EXTRA";

    public static final String EXTRA_START_TIME = BuildConfig.APPLICATION_ID + "EXTRA_START_TIME";
    public static final String EXTRA_BUNDLE = BuildConfig.APPLICATION_ID + "EXTRA_BUNDLE";

    // billing
    public static final String INTENT_ADS_REMOVED = BuildConfig.APPLICATION_ID + ".INTENT_ADS_REMOVED";

    // Activity recognition change. Currently not received by anyone
    public static final String INTENT_ACTIVITY_CHANGED = BuildConfig.APPLICATION_ID + ".INTENT_ACTIVITY_CHANGED";

    // request to save a car
    public static final String INTENT_ENABLE_NOTIFICATION_SOUND = BuildConfig.APPLICATION_ID + ".INTENT_ENABLE_NOTIFICATION_SOUND";

    /**
     * Average walking speed
     */
    private final static float WALKING_SPEED = 1.319F; // 1.319 m/s -> 4.75 km/h
}
