package com.cahue.iweco.spots.query;

import com.cahue.iweco.spots.ParkingSpot;

import java.util.List;
import java.util.Set;

/**
 * Created by Francesco on 30/03/2015.
 */
public class QueryResult {

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
