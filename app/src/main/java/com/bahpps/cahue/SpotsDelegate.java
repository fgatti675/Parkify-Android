package com.bahpps.cahue;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.bahpps.cahue.debug.TestParkingSpotsService;
import com.bahpps.cahue.spots.ParkingSpot;
import com.bahpps.cahue.spots.ParkingSpotsService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate extends MarkerDelegate implements Parcelable {

    private static final String TAG = "SpotsDelegate";
    private Set<ParkingSpot> spots;
    private GoogleMap mMap;
    private LatLngBounds currentQueryBounds;
    private LatLngBounds viewPort;

    /**
     * If markers shouldn't be displayed (like zoom is too far)
     */
    private boolean hideMarkers = false;

    private ParkingSpotsService service;

    public static final Parcelable.Creator<SpotsDelegate> CREATOR =
            new Parcelable.Creator<SpotsDelegate>() {
                @Override
                public SpotsDelegate createFromParcel(Parcel parcel) {
                    return new SpotsDelegate(parcel);
                }

                @Override
                public SpotsDelegate[] newArray(int size) {
                    return new SpotsDelegate[size];
                }
            };

    public SpotsDelegate() {
        spots = new HashSet<ParkingSpot>();
    }

    public void setMap(GoogleMap map) {
        this.mMap = map;
    }

    public SpotsDelegate(Parcel parcel) {
        ClassLoader classLoader = SpotsDelegate.class.getClassLoader();
        ParkingSpot[] spotsArray = (ParkingSpot[]) parcel.readParcelableArray(classLoader);
        spots = new HashSet<ParkingSpot>(Arrays.asList(spotsArray));
        currentQueryBounds = parcel.readParcelable(classLoader);
        viewPort = parcel.readParcelable(classLoader);
    }



    /**
     * Set the bounds where
     * @param viewPort
     * @return
     */
    public boolean applyBounds(LatLngBounds viewPort) {

        this.viewPort = viewPort;

        if (currentQueryBounds != null
                && currentQueryBounds.contains(viewPort.northeast)
                && currentQueryBounds.contains(viewPort.southwest)) {
            return false;
        }

        // merge previous with current
        if (currentQueryBounds != null)
            currentQueryBounds = LatLngBounds.builder()
                    .include(currentQueryBounds.northeast)
                    .include(currentQueryBounds.southwest)
                    .include(viewPort.northeast)
                    .include(viewPort.southwest)
                    .build();
        else
            currentQueryBounds = viewPort;

        /**
         * In case there was a query running, cancel it
         */
//        if (service != null) service.cancel(true);

        service = new TestParkingSpotsService(currentQueryBounds, new ParkingSpotsService.ParkingSpotsUpdateListener() {
            @Override
            public synchronized void onLocationsUpdate(Set<ParkingSpot> parkingSpots) {
                spots.addAll(parkingSpots);
                redraw();
            }
        });


        Log.d(TAG, "Starting query for viewPort: " + viewPort);

        service.execute();
        return true;
    }

    public void draw() {
        Log.d(TAG, "Drawing spots");
        if (hideMarkers) return;
        for (ParkingSpot parkingSpot : spots) {
            mMap.addMarker(new MarkerOptions().position(parkingSpot.getPosition()));
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        ParkingSpot[] spotsArray = new ParkingSpot[spots.size()];
        parcel.writeParcelableArray(spots.toArray(spotsArray), 0);
        parcel.writeParcelable(currentQueryBounds, 0);
        parcel.writeParcelable(viewPort, 0);
    }

    public void hideMarkers() {
        hideMarkers = true;
        redraw();
    }

    public void showMarkers() {
        hideMarkers = false;
        redraw();
    }
}
