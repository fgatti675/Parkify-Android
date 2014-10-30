package com.bahpps.cahue;

import com.google.android.gms.maps.model.CameraPosition;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class MarkerDelegate {

    private MapsMarkersDelegatesManager delegatesManager;

    public abstract void draw();

    public MapsMarkersDelegatesManager getDelegatesManager() {
        return delegatesManager;
    }

    public void setDelegatesManager(MapsMarkersDelegatesManager delegatesManager) {
        this.delegatesManager = delegatesManager;
    }

    protected void redraw(){
        delegatesManager.draw();
    }

    public abstract void onCameraChange(CameraPosition cameraPosition);


}
