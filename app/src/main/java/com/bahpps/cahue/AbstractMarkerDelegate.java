package com.bahpps.cahue;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class AbstractMarkerDelegate implements Parcelable {

    private boolean needsRedraw = true;

    public AbstractMarkerDelegate() {

    }

    public AbstractMarkerDelegate(Parcel parcel) {
        needsRedraw = parcel.readByte() > 0;
    }

    public abstract void doDraw();

    public boolean isNeedsRedraw() {
        return needsRedraw;
    }

    protected void setNeedsRedraw(boolean needsRedraw) {
        this.needsRedraw = needsRedraw;
    }

    protected void markAsDirty() {
        setNeedsRedraw(true);
    }


    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeByte((byte) (needsRedraw ? 1 : 0));
    }

    /**
     * Called when the map is ready to be used
     * @param mMap
     */
    public void onMapReady(GoogleMap mMap) {

    }

    /**
     * Should be called on the onPause cycle.
     */
    public void onPause() {

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
     * If any changes are done, the method {@link #markAsDirty()} needs to be called
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
