package com.cahue.iweco;

import android.app.Fragment;
import android.location.Location;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class AbstractMarkerDelegate extends Fragment implements CameraUpdateRequester, LocationListener {

    protected Location userLocation;

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

    @Override
    public final void onLocationChanged(Location userLocation) {
        this.userLocation = userLocation;
        onUserLocationChanged(userLocation);
    }

    protected abstract void onUserLocationChanged(Location userLocation);

    protected LatLng getUserLatLng() {
        if (userLocation == null) return null;
        return new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
    }

}
