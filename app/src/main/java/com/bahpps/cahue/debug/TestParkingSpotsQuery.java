package com.bahpps.cahue.debug;

import com.bahpps.cahue.spots.ParkingSpotsQuery;
import com.google.android.gms.maps.model.LatLngBounds;

/**
* Created by Francesco on 23/10/2014.
*/
public class TestParkingSpotsQuery extends ParkingSpotsQuery {


    private static final String TEST_TABLE_ID = "1Pa5hqK1KxKwgZmbgBFJ5opcbRGHELFsCL6CyE8bf";

    /**
     * Create a new service. {@code #execute()} must be called afterwards
     *
     * @param latLngBounds
     * @param listener
     */
    public TestParkingSpotsQuery(LatLngBounds latLngBounds, ParkingSpotsUpdateListener listener) {
        super(latLngBounds, listener);
    }

    @Override
    public String getTableId() {
        return TEST_TABLE_ID;
    }
}
