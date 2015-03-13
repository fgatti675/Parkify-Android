package com.cahue.iweco;

import android.app.Fragment;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class AbstractMarkerDelegate extends Fragment implements CameraUpdateRequester, LocationListener {

    public abstract void doDraw();

    /**
     * Called when the map is ready to be used
     * @param mMap
     */
    public void onMapReady(GoogleMap mMap) {

    }

    /**
     * Called when the details view is closed
     */
    public void onDetailsClosed() {

    }

    /**
     * Called when a marker is clicked
     *
     * @param marker
     * @return
     */
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

}
