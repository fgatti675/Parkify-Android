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
    public static final long MINIMUM_STAY_MS = 180000;

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
}
