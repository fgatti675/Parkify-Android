package com.bahpps.cahue.spots;

import android.os.Parcel;
import android.os.Parcelable;

import com.bahpps.cahue.R;
import com.bahpps.cahue.debug.TestParkingSpotsService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate implements Parcelable {

    private List<ParkingSpot> spots;
    private GoogleMap mMap;
    private LatLngBounds currentBounds;

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
        spots = new ArrayList<ParkingSpot>();
    }

    public void setMap(GoogleMap map) {
        this.mMap = map;
    }

    public SpotsDelegate(Parcel parcel) {
        ParkingSpot[] spotsArray = (ParkingSpot[]) parcel.readParcelableArray(SpotsDelegate.class.getClassLoader());
        spots = Arrays.asList(spotsArray);
    }

    public boolean applyBounds(LatLngBounds bounds) {
        if (currentBounds != null
                && currentBounds.contains(bounds.northeast)
                && currentBounds.contains(bounds.southwest)) {
            return false;
        }

        currentBounds = bounds;

        /**
         * In case there was a query running, cancel it
         */
        if (service != null) service.cancel(true);

        service = new TestParkingSpotsService(currentBounds, new ParkingSpotsService.ParkingSpotsUpdateListener() {
            @Override
            public synchronized void onLocationsUpdate(List<ParkingSpot> parkingSpots) {
                mMap.clear();
                for (ParkingSpot parkingSpot : parkingSpots) {
                    mMap.addMarker(new MarkerOptions()
                            .position(parkingSpot.location));
                }
            }
        });
        service.execute();
        return true;
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
}
