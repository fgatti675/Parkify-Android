package com.bahpps.cahue;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.bahpps.cahue.debug.TestParkingSpotsService;
import com.bahpps.cahue.spots.ParkingSpot;
import com.bahpps.cahue.spots.ParkingSpotsService;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
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
public class SpotsDelegate extends MarkerDelegate implements Parcelable {


    /**
     * If zoom is more far than this, we don't display the markers
     */
    public static final float MAX_ZOOM = 13.5F;

    private static final String TAG = "SpotsDelegate";
    private Set<ParkingSpot> spots;
    private GoogleMap mMap;
    private List<LatLngBounds> queriedBounds = new ArrayList<LatLngBounds>();
    private LatLngBounds viewBounds;
    private LatLngBounds extendedViewBounds;

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

    public void init(GoogleMap map) {
        this.mMap = map;
    }

    public SpotsDelegate(Parcel parcel) {
        ClassLoader classLoader = SpotsDelegate.class.getClassLoader();
        ParkingSpot[] spotsArray = (ParkingSpot[]) parcel.readParcelableArray(classLoader);
        spots = new HashSet<ParkingSpot>(Arrays.asList(spotsArray));
        LatLngBounds[] boundsArray = (LatLngBounds[]) parcel.readParcelableArray(classLoader);
        queriedBounds = Arrays.asList(boundsArray);
        viewBounds = parcel.readParcelable(classLoader);
    }


    /**
     * Set the bounds where
     *
     * @param viewPort What the user is actually seeing right now
     * @param queryPort A broader space we want to query
     * @return
     */
    private synchronized boolean applyBounds(LatLngBounds viewPort, LatLngBounds queryPort) {

        this.viewBounds = viewPort;
        this.extendedViewBounds = queryPort;

        /**
         * Check if this query is already contained in another one
         */
        for (LatLngBounds latLngBounds : queriedBounds) {
            if (latLngBounds.contains(extendedViewBounds.northeast)
                    && latLngBounds.contains(extendedViewBounds.southwest)) {
                Log.d(TAG, "NO need to query again");
                return false;
            }
        }

        // merge previous with current
        queriedBounds.add(extendedViewBounds);

        /**
         * In case there was a query running, cancel it
         */
//        if (service != null) service.cancel(true);

        service = new TestParkingSpotsService(extendedViewBounds, new ParkingSpotsService.ParkingSpotsUpdateListener() {
            @Override
            public synchronized void onLocationsUpdate(Set<ParkingSpot> parkingSpots) {
                spots.addAll(parkingSpots);
                redraw();
            }
        });


        Log.d(TAG, "Starting query for queryPort: " + extendedViewBounds);

        service.execute();
        return true;
    }

    public void draw() {
        Log.d(TAG, "Drawing spots");
        if (hideMarkers) return;
        for (ParkingSpot parkingSpot : spots) {
            LatLng spotPosition = parkingSpot.getPosition();
            if (extendedViewBounds.contains(spotPosition) || viewBounds.contains(spotPosition))
                mMap.addMarker(new MarkerOptions().position(spotPosition));
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        float zoom = mMap.getCameraPosition().zoom;
        Log.d(TAG, "zoom: " + zoom);

        /**
         * Query for current camera position
         */
        if (zoom >= MAX_ZOOM) {
            Log.d(TAG, "querying: " + zoom);

            LatLngBounds viewPort = mMap.getProjection().getVisibleRegion().latLngBounds;
            viewPort.northeast.
            LatLngBounds expanded = LatLngBounds.builder()
                    .include(getOffsetLatLng(viewPort.getCenter(), 2000, 2000))
                    .include(getOffsetLatLng(viewPort.getCenter(), -2000, -2000))
                    .build();
            applyBounds(viewPort, expanded);

            showMarkers();
        }
        /**
         * Too far
         */
        else {
            hideMarkers();
        }

        redraw();
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        ParkingSpot[] spotsArray = new ParkingSpot[spots.size()];
        parcel.writeParcelableArray(spots.toArray(spotsArray), 0);
        LatLngBounds[] boundsArray = new LatLngBounds[queriedBounds.size()];
        parcel.writeParcelableArray(queriedBounds.toArray(boundsArray), 0);
        parcel.writeParcelable(viewBounds, 0);
    }

    private void hideMarkers() {
        hideMarkers = true;
    }

    private void showMarkers() {
        hideMarkers = false;
    }

    public LatLng getOffsetLatLng(LatLng original, double offsetNorth, double offsetEast) {

        //Earth’s radius, sphere
        double R = 6378137;

        //Coordinate offsets in radians
        double dLat = offsetNorth / R;
        double dLon = offsetEast / (R * Math.cos(Math.PI * original.latitude / 180));

        //OffsetPosition, decimal degrees
        double nLat = original.latitude + dLat * 180 / Math.PI;
        double nLon = original.longitude + dLon * 180 / Math.PI;

        return new LatLng(nLat, nLon);
    }

}
