package com.bahpps.cahue;

/**
 * Created by francesco on 28.10.2014.
 */
public abstract class MarkerDelegate {

    protected MapsMarkersDelegatesManager delegatesManager;

    public abstract void draw();

    public MapsMarkersDelegatesManager getDelegatesManager() {
        return delegatesManager;
    }

    public void setDelegatesManager(MapsMarkersDelegatesManager delegatesManager) {
        this.delegatesManager = delegatesManager;
    }
}
