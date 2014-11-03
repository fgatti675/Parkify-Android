package com.bahpps.cahue;

import com.google.android.gms.maps.model.CameraPosition;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class AbstractMarkerDelegate {

    private MapsMarkersManager delegatesManager;
    private boolean needsRedraw = true;

    public abstract void doDraw();

    public MapsMarkersManager getDelegatesManager() {
        return delegatesManager;
    }

    public void setDelegatesManager(MapsMarkersManager delegatesManager) {
        this.delegatesManager = delegatesManager;
    }

    public boolean isNeedsRedraw() {
        return needsRedraw;
    }

    protected void setNeedsRedraw(boolean needsRedraw) {
        this.needsRedraw = needsRedraw;
    }

    protected void markAsDirty() {
        setNeedsRedraw(true);
    }

    public void onResume() {

    }

    /**
     * Should be called on the onPause cycle.
     */
    public void onPause(){

    }

    /**
     * Indicate this delegate that the camera of the map has changed.
     * This method need to implement the logic to load any new data if necessary (asynchronously).
     * If any changes are done, the method {@link #markAsDirty()} needs to be called
     *
     * @param cameraPosition
     */
    public abstract void onCameraChange(CameraPosition cameraPosition);


}
