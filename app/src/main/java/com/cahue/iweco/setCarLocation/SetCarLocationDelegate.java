package com.cahue.iweco.setCarLocation;

import android.location.Location;

import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.CameraUpdateRequester;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by Francesco on 06/07/2015.
 */
public class SetCarLocationDelegate extends AbstractMarkerDelegate {

    Location carLocation;

    Marker marker;

    @Override
    public void doDraw() {
        if (marker != null) {
            marker.remove();
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {

    }

    @Override
    public void setCameraFollowing(boolean following) {

    }

    @Override
    public void onMapResized() {

    }

    public void setCarLocation(Location carLocation) {
        this.carLocation = carLocation;
        doDraw();
//        detailsViewManager.setDetailsFragment();
    }
}
