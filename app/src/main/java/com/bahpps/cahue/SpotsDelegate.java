package com.bahpps.cahue;

import android.os.Handler;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate extends AbstractMarkerDelegate implements Parcelable {

    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    // Earth’s radius, sphere
    private final static double EARTH_RADIUS = 6378137;
    private static final long TIMEOUT_MS = 5000;

    private final Handler handler = new Handler();

    /**
     * If zoom is more far than this, we don't display the markers
     */
    public static final float MAX_ZOOM = 13.5F;

    private static final String TAG = "SpotsDelegate";
    private Set<ParkingSpot> spots;
    private Map<ParkingSpot, Marker> spotMarkersMap;
    private GoogleMap mMap;
    private List<LatLngBounds> queriedBounds = new ArrayList<LatLngBounds>();
    private LatLngBounds viewBounds;
    private LatLngBounds extendedViewBounds;

    private Date lastResetTaskRequestTime;
    private ScheduledFuture scheduledResetTask;

    // in the next fetching of spots, clear the previous state
    private boolean shouldBeReset = false;

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
        lastResetTaskRequestTime = new Date();
    }

    public SpotsDelegate(Parcel parcel) {
        ClassLoader classLoader = SpotsDelegate.class.getClassLoader();
        ParkingSpot[] spotsArray = (ParkingSpot[]) parcel.readParcelableArray(classLoader);
        spots = new HashSet<ParkingSpot>(Arrays.asList(spotsArray));
        LatLngBounds[] boundsArray = (LatLngBounds[]) parcel.readParcelableArray(classLoader);
        queriedBounds = Arrays.asList(boundsArray);
        viewBounds = parcel.readParcelable(classLoader);
        shouldBeReset = parcel.readByte() != 0;
        lastResetTaskRequestTime = (Date) parcel.readSerializable();
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
        parcel.writeByte((byte) (shouldBeReset ? 1 : 0));
        parcel.writeSerializable(lastResetTaskRequestTime);
    }

    public void setMap(GoogleMap map) {
        this.mMap = map;
    }

    public void init() {
        if (mMap == null)
            throw new IllegalStateException("Please set a GoogleMap instance before calling the method init()");

        spotMarkersMap = new HashMap<ParkingSpot, Marker>();
        if (scheduledResetTask == null
                || scheduledResetTask.isDone()
                || System.currentTimeMillis() - lastResetTaskRequestTime.getTime() > TIMEOUT_MS) {
            reset();
            createResetTask();
        }
    }

    public void createResetTask() {
        Log.d(TAG, "Creating new reset task");
        lastResetTaskRequestTime = new Date();

        scheduledResetTask = scheduledExecutorService.schedule(
                new Callable() {
                    public Object call() throws Exception {
                        shouldBeReset = true;
                        createResetTask();
                        return "Called!";
                    }
                },
                TIMEOUT_MS,
                TimeUnit.MILLISECONDS);

    }

    private void reset() {
        Log.d(TAG, "spots reset");
        queriedBounds.clear();
        spots.clear();
        spotMarkersMap.clear();
    }

    private boolean repeatLastQuery() {
        return applyBounds(viewBounds, extendedViewBounds);
    }


    /**
     * Set the bounds where
     *
     * @param viewPort  What the user is actually seeing right now
     * @param queryPort A broader space we want to query so that data is there when we move the camera
     * @return
     */
    private synchronized boolean applyBounds(LatLngBounds viewPort, LatLngBounds queryPort) {

        this.viewBounds = viewPort;
        this.extendedViewBounds = queryPort;

        /**
         * Check if this query is already contained in another one
         */
        for (LatLngBounds latLngBounds : queriedBounds) {
            if (latLngBounds.contains(viewBounds.northeast) && latLngBounds.contains(viewBounds.southwest)) {
                Log.d(TAG, "NO need to query again");
                return false;
            }
        }

        // we keep a reference of the current query to prevent repeating it
        queriedBounds.add(extendedViewBounds);

        /**
         * In case there was a query running, cancel it
         */
//        if (service != null) service.cancel(true);

        service = new TestParkingSpotsService(extendedViewBounds, new ParkingSpotsService.ParkingSpotsUpdateListener() {
            @Override
            public synchronized void onLocationsUpdate(Set<ParkingSpot> parkingSpots) {
                if (shouldBeReset) {
                    reset();
                    repeatLastQuery();
                    shouldBeReset = false;
                }
                spots.addAll(parkingSpots);
                doDraw();
            }
        });


        Log.d(TAG, "Starting query for queryPort: " + extendedViewBounds);

        service.execute();
        return true;
    }

    public void doDraw() {

        // clear first
        clear();

        if (hideMarkers) return;

        Log.d(TAG, "Drawing spots");
        for (ParkingSpot parkingSpot : spots) {
            LatLng spotPosition = parkingSpot.getPosition();

            Marker marker = spotMarkersMap.get(parkingSpot);
            boolean fadeIn = false; // the marker will fade in when appearing for the first time

            if (marker == null) {
                marker = mMap.addMarker(new MarkerOptions().position(spotPosition));
                marker.setVisible(false);
                spotMarkersMap.put(parkingSpot, marker);
                fadeIn = true;
            }

            if (!marker.isVisible() && viewBounds.contains(spotPosition)) {
                makeMarkerVisible(marker, fadeIn);
            }
        }
    }


    private void clear() {
        for (Marker marker : spotMarkersMap.values()) {
            if (hideMarkers || !viewBounds.contains(marker.getPosition()))
                marker.setVisible(false);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "scheduledResetTask canceled");
        scheduledResetTask.cancel(true);
    }

    private void makeMarkerVisible(final Marker marker, boolean fadeIn) {

        marker.setVisible(true);

        if (fadeIn) {
            marker.setAlpha(0);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    float alpha = marker.getAlpha() + 0.05F;
                    if (alpha < 1) {
                        // Post again 12ms later.
                        marker.setAlpha(alpha);
                        handler.postDelayed(this, 12);
                    } else {
                        marker.setAlpha(1);
                        // animation ended
                    }
                }
            });
        }
    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        float zoom = mMap.getCameraPosition().zoom;
        Log.v(TAG, "zoom: " + zoom);

        /**
         * Query for current camera position
         */
        if (zoom >= MAX_ZOOM) {
            Log.d(TAG, "Querying because we are close enough");

            LatLngBounds viewPort = mMap.getProjection().getVisibleRegion().latLngBounds;
            LatLngBounds expanded = LatLngBounds.builder()
                    .include(getOffsetLatLng(viewPort.northeast, 500, 500))
                    .include(getOffsetLatLng(viewPort.southwest, -500, -500))
                    .build();
            applyBounds(viewPort, expanded);

            showMarkers();
        }
        /**
         * Too far
         */
        else {
            Log.d(TAG, "Too far to query locations");
            hideMarkers();
        }

        markAsDirty();

    }


    private void hideMarkers() {
        hideMarkers = true;
    }

    private void showMarkers() {
        hideMarkers = false;
    }

    public LatLng getOffsetLatLng(LatLng original, double offsetNorth, double offsetEast) {

        // Coordinate offsets in radians
        double dLat = offsetNorth / EARTH_RADIUS;
        double dLon = offsetEast / (EARTH_RADIUS * Math.cos(Math.PI * original.latitude / 180));

        // OffsetPosition, decimal degrees
        double nLat = original.latitude + dLat * 180 / Math.PI;
        double nLon = original.longitude + dLon * 180 / Math.PI;

        return new LatLng(nLat, nLon);
    }

}
