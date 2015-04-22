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
     * Average walking speed
     */
    private final static float WALKING_SPEED = 1.319F; // 1.319 m/s -> 4.75 km/h

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

    // updated address
    public static final String INTENT_ADDRESS_UPDATE = BuildConfig.APPLICATION_ID + ".INTENT_ADDRESS_UPDATE";
    public static final String INTENT_EXTRA_ADDRESS = BuildConfig.APPLICATION_ID + ".EXTRA_ADDRESS";

    // updated car
    public static final String INTENT_CAR_UPDATE = BuildConfig.APPLICATION_ID +  ".INTENT_CAR_UPDATED";
    public static final String INTENT_CAR_EXTRA_ID = BuildConfig.APPLICATION_ID + ".EXTRA_CAR_ID";

    // billing
    public static final String INTENT_BILLING_READY = BuildConfig.APPLICATION_ID +  ".INTENT_BILLING_READY";
    public static final String INTENT_NEW_PURCHASE = BuildConfig.APPLICATION_ID +  ".INTENT_NEW_PURCHASE";
}
