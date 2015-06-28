package com.cahue.iweco;

/**
 * Created by francesco on 13.03.2015.
 */
public class Constants {

    /**
     * Minimum desired accuracy
     */
    public final static int ACCURACY_THRESHOLD_M = 25;

    /**
     * Only post locations if the car has stopped for at least a few minutes
     */
    public static final long MINIMUM_STAY_MS = 2 * 60 * 1000;
    /**
     * Distance at which we can set a geofence in the car after we parked, so that we know
     * when the user is approaching it.
     */
    public static final float PARKED_DISTANCE_THRESHOLD = 90;
    public static final float GEOFENCE_RADIUS_IN_METERS = 60;
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = 8 * 60 * 60 * 1000;
    /**
     * Intents
     */

    public static final String INTENT_USER_INFO_UPDATE = BuildConfig.APPLICATION_ID + ".INTENT_USER_INFO_UPDATE";

    // updated address
    public static final String INTENT_ADDRESS_UPDATE = BuildConfig.APPLICATION_ID + ".INTENT_ADDRESS_UPDATE";

    // request to save a car
    public static final String INTENT_SAVE_CAR_REQUEST = BuildConfig.APPLICATION_ID + ".INTENT_SAVE_CAR_REQUEST";

    // a car was updated
    public static final String INTENT_CAR_UPDATED = BuildConfig.APPLICATION_ID + ".INTENT_CAR_UPDATED";

    // extra fields
    public static final String INTENT_CAR_EXTRA_ID = BuildConfig.APPLICATION_ID + ".EXTRA_CAR_ID";
    public static final String INTENT_CAR_EXTRA_ADDRESS = BuildConfig.APPLICATION_ID + ".EXTRA_CAR_ADDRESS";
    public static final String INTENT_CAR_EXTRA_LOCATION = BuildConfig.APPLICATION_ID + ".EXTRA_CAR_LOCATION";


    public static final String EXTRA_START_TIME = BuildConfig.APPLICATION_ID + "EXTRA_START_TIME";

    // billing
    public static final String INTENT_BILLING_READY = BuildConfig.APPLICATION_ID + ".INTENT_BILLING_READY";
    public static final String INTENT_NEW_PURCHASE = BuildConfig.APPLICATION_ID + ".INTENT_NEW_PURCHASE";

    // activity recognition
    public static final String INTENT_ACTIVITY_RECOGNIZED = BuildConfig.APPLICATION_ID + ".INTENT_ACTIVITY_RECOGNIZED";
    public static final String INTENT_EXTRA_ACTIVITIES_RESULT = BuildConfig.APPLICATION_ID + ".INTENT_EXTRA_ACTIVITIES_RESULT";

    /**
     * Average walking speed
     */
    private final static float WALKING_SPEED = 1.319F; // 1.319 m/s -> 4.75 km/h
}
