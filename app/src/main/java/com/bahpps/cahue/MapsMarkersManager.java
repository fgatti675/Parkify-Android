package com.bahpps.cahue;

import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by francesco on 28.10.2014.
 */
public class MapsMarkersManager {

    public final static String TAG = "MapsMarkersManager";

    List<AbstractMarkerDelegate> delegates = new ArrayList();

    GoogleMap mMap;

    public MapsMarkersManager(GoogleMap map) {
        this.mMap = map;
    }

    public void add(AbstractMarkerDelegate markerDelegate) {
        markerDelegate.setDelegatesManager(this);
        delegates.add(markerDelegate);
    }

    public synchronized void drawIfNecessary() {

        for (AbstractMarkerDelegate delegate : delegates) {
            if (delegate.isNeedsRedraw()) {
                Log.i(TAG, delegate.getClass().getSimpleName() + " is being redrawn because ");
                delegate.doDraw();
                delegate.setNeedsRedraw(false);
            }
        }
    }

    public void onCameraChange(CameraPosition cameraPosition) {
        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onCameraChange(cameraPosition);
        }
        drawIfNecessary();
    }

    public void onResume(){
        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onResume();
        }
    }

    public void onPause(){
        for (AbstractMarkerDelegate delegate : delegates) {
            delegate.onPause();
        }
    }
}
