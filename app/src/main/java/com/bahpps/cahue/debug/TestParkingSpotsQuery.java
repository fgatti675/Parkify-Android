package com.bahpps.cahue.debug;

import com.bahpps.cahue.spots.query.FusionParkingSpotsQuery;

/**
* Created by Francesco on 23/10/2014.
*/
public class TestParkingSpotsQuery extends FusionParkingSpotsQuery {


    private static final String TEST_TABLE_ID = "1Pa5hqK1KxKwgZmbgBFJ5opcbRGHELFsCL6CyE8bf";

    /**
     * Create a new service. {@code #execute()} must be called afterwards
     *
     * @param listener
     */
    public TestParkingSpotsQuery( ParkingSpotsUpdateListener listener) {
        super(listener);
    }

    @Override
    public String getTableId() {
        return TEST_TABLE_ID;
    }
}
