package com.bahpps.cahue.spots.query;

import android.os.AsyncTask;
import android.util.Log;

import com.bahpps.cahue.spots.ParkingSpot;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.Set;

/**
 * Created by francesco on 13.11.2014.
 */
public abstract class ParkingSpotsQuery extends AsyncTask<Void, Void, Set<ParkingSpot>> {

    /**
     * Mode the query is being run
     */
    public enum Mode {
        /**
         * Query the spots in the viewPort
         */
        viewPort,

        /**
         * Query the closest spots
         */
        closestSpots
    }

    protected LatLngBounds latLngBounds;
    protected ParkingSpotsUpdateListener listener;
    protected LatLng center;
    protected Integer limit;
    /**
     * Was there an error in the execution?
     */
    protected boolean error = false;

    protected Mode mode;

    public ParkingSpotsQuery(ParkingSpotsUpdateListener listener) {
        this.listener = listener;
    }

    public void retrieveLocationsIn(LatLngBounds latLngBounds) {
        this.latLngBounds = latLngBounds;
        mode = Mode.viewPort;
        execute();
    }

    public void retrieveNearbySpots(LatLng location, int limit) {
        this.center = location;
        this.limit = limit;
        mode = Mode.closestSpots;
        execute();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Set<ParkingSpot> parkingSpots) {

        if (error)
            listener.onError(this);

        Log.d("ParkingSpotsQuery", parkingSpots.toString());
        super.onPostExecute(parkingSpots);
        listener.onSpotsUpdate(this, parkingSpots);
    }

    /**
     * Components that use this service must implement a listener using this interface to get the
     * parking locations
     */
    public interface ParkingSpotsUpdateListener {
        void onSpotsUpdate(ParkingSpotsQuery query, Set<ParkingSpot> parkingSpots);

        void onError(ParkingSpotsQuery query);
    }
}
