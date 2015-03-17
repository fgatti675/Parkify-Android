package com.cahue.iweco.debug;

import android.location.Location;

/**
* Created by francesco on 17.03.2015.
*/
public interface ServiceListener {
    public void onNewLocation(Location location);
    public void onLocationPost();
}
