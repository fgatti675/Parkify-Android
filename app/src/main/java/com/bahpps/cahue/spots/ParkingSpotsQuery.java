package com.bahpps.cahue.spots;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Set;

/**
 * Created by francesco on 13.11.2014.
 */
public abstract class ParkingSpotsQuery extends AsyncTask<Void, Void, Set<ParkingSpot>> {

    protected LatLngBounds latLngBounds;
    protected ParkingSpotsUpdateListener listener;
    protected LatLng center;
    protected int limit;

    public ParkingSpotsQuery(ParkingSpotsUpdateListener listener) {
        this.listener = listener;
    }

    public void retrieveLocationsIn(LatLngBounds latLngBounds){
        this.latLngBounds = latLngBounds;
        execute();
    }

    public void retrieveLocationsCloseTo(LatLng location, int limit) {
        this.center = location;
        this.limit = limit;
        execute();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Set<ParkingSpot> parkingSpots) {
        Log.d("ParkingSpotsQuery", parkingSpots.toString());
        super.onPostExecute(parkingSpots);
        listener.onLocationsUpdate(parkingSpots);
    }

    /**
     * Components that use this service must implement a listener using this interface to get the
     * parking locations
     */
    public interface ParkingSpotsUpdateListener {
        void onLocationsUpdate(Set<ParkingSpot> parkingSpots);
    }
}
