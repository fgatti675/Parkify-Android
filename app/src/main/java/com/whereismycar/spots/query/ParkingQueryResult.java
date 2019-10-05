package com.whereismycar.spots.query;

import com.whereismycar.model.ParkingSpot;

import java.util.Set;

/**
 * Created by Francesco on 30/03/2015.
 */
public class ParkingQueryResult {

    /**
     * Was there an error retrieving data
     */
    public boolean error = false;

    /**
     * The results included are not complete
     */
    public boolean moreResults = false;

    /**
     * Spots
     */
    public Set<ParkingSpot> spots;

}
