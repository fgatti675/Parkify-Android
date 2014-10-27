package com.bahpps.cahue.spots;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.bahpps.cahue.debug.TestParkingSpotsService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate implements Parcelable {

    private static final String TAG = "SpotsDelegate";
    private Set<ParkingSpot> spots;
    private GoogleMap mMap;
    private LatLngBounds currentBounds;

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
        ParkingSpot[] spotsArray = (ParkingSpot[]) parcel.readParcelableArray(SpotsDelegate.class.getClassLoader());
        spots = new HashSet<ParkingSpot>(Arrays.asList(spotsArray));
    }

    public boolean applyBounds(LatLngBounds bounds) {
        if (currentBounds != null
                && currentBounds.contains(bounds.northeast)
                && currentBounds.contains(bounds.southwest)) {
            draw();
            return false;
        }

        currentBounds = bounds;

        /**
         * In case there was a query running, cancel it
         */
        if (service != null) service.cancel(true);

        service = new TestParkingSpotsService(currentBounds, new ParkingSpotsService.ParkingSpotsUpdateListener() {
            @Override
            public synchronized void onLocationsUpdate(Set<ParkingSpot> parkingSpots) {
                spots = new HashSet<ParkingSpot>(parkingSpots);
                draw();
            }
        });


        Log.d(TAG, "Starting query for bounds: " + bounds);

        service.execute();
        return true;
    }

    public synchronized void draw() {
        Log.d(TAG, "Drawing spots");
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
    }

    public void hideMarkers() {
        hideMarkers = true;
    }

}
