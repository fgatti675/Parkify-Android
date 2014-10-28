package com.bahpps.cahue;

import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by francesco on 28.10.2014.
 */
public class MapsMarkersDelegatesManager {

    List<MarkerDelegate> delegates = new ArrayList();

    GoogleMap mMap;

    public MapsMarkersDelegatesManager(GoogleMap map) {
        this.mMap = map;
    }

    public void add(MarkerDelegate markerDelegate) {
        markerDelegate.setDelegatesManager(this);
        delegates.add(markerDelegate);
    }

    public synchronized void draw(){
        mMap.clear();
        for(MarkerDelegate delegate:delegates)
            delegate.draw();
    }

}
