package com.cahue.iweco.util;

import com.google.android.gms.maps.model.LatLng;

/**
 * Date: 30.04.2015
 *
 * @author francesco
 */
public class LatLngUtil {


    // Earth’s radius, sphere
    private final static double EARTH_RADIUS = 6378137;

    public static LatLng getOffsetLatLng(LatLng original, double offsetNorth, double offsetEast) {

        // Coordinate offsets in radians
        double dLat = offsetNorth / EARTH_RADIUS;
        double dLon = offsetEast / (EARTH_RADIUS * Math.cos(Math.PI * original.latitude / 180));

        // OffsetPosition, decimal degrees
        double nLat = original.latitude + dLat * 180 / Math.PI;
        double nLon = original.longitude + dLon * 180 / Math.PI;

        return new LatLng(nLat, nLon);
    }
}
