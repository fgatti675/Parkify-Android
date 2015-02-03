package com.bahpps.cahue;

import android.app.Fragment;
import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class AbstractMarkerDelegate extends Fragment {


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
     * Called when the go to my location button is pressed
     */
    public void onZoomToMyLocation() {

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

    /**
     * Indicate this delegate that the camera of the map has changed.
     * This method need to implement the logic to load any new data if necessary (asynchronously).
     *
     * @param cameraPosition
     * @param justFinishedAnimating
     */
    public abstract void onCameraChange(CameraPosition cameraPosition, boolean justFinishedAnimating);

    /**
     * Called when the user changes location
     *
     * @param location
     */
    public abstract void onLocationChanged(Location location);

}
