package com.cahue.iweco;

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.spots.MarkerFactory;
import com.cahue.iweco.spots.SpotDetailsFragment;
import com.cahue.iweco.spots.query.ParkingQueryResult;
import com.cahue.iweco.spots.query.ParkingSpotsQuery;
import com.cahue.iweco.util.GMapV2Direction;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Delegate in charge of querying and drawing parking spots in the map.
 * <p>
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate extends AbstractMarkerDelegate
        implements ParkingSpotsQuery.ParkingSpotsUpdateListener,
        CameraUpdateRequester {

    public static final String FRAGMENT_TAG = "SPOTS_DELEGATE";
    // distance we are adding to the bounds query on each one of the 4 sides to get also results outside the screen
    private static final int OFFSET_METERS = 2000;

    /**
     * If zoom is more far than this, we don't display the markers
     */
    private final static float MAX_ZOOM = 13F;
    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private static final String TAG = "SpotsDelegate";
    private static final String QUERY_TAG = "SpotsDelegateQuery";

    // Earth’s radius, sphere
    private final static double EARTH_RADIUS = 6378137;

    // time after we consider the query is outdated and need to repeat
    private final static long TIMEOUT_MS = 60000;

    // max number of spots displayed at once.
    private static final int MARKERS_LIMIT = 40;

    private static final float MAX_DIRECTIONS_DISTANCE = 40000; // 40 km

    private Set<ParkingSpot> spots;

    private Map<ParkingSpot, Marker> spotMarkersMap;

    @Nullable
    private Marker selectedMarker;

    // In the next spots update, clear the previous state
    private boolean resetOnNextUpdate = false;

    private List<LatLngBounds> queriedBounds;

    private LatLngBounds viewBounds;
    private LatLngBounds extendedViewBounds;

    private Date lastResetTaskRequestTime;
    private ScheduledFuture scheduledResetTask;

    private boolean following = false;

    /**
     * If markers shouldn't be displayed (like zoom is too far)
     */
    private boolean areMarkersDisplayed = false;

    @Nullable
    private ParkingSpot selectedSpot;

    private float maxZoom;

    /**
     * Directions delegate
     */
    private DirectionsDelegate directionsDelegate;

    @NonNull
    public static SpotsDelegate newInstance() {
        SpotsDelegate fragment = new SpotsDelegate();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        setRetainInstance(true);

        queriedBounds = new ArrayList<>();
        spots = new HashSet<>();
        lastResetTaskRequestTime = new Date();

        spotMarkersMap = new HashMap<>(MARKERS_LIMIT);

        maxZoom = BuildConfig.DEBUG ? 0 : MAX_ZOOM;

        directionsDelegate = new DirectionsDelegate(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setUpResetTask();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "onMapReady");
        directionsDelegate.setMap(map);
        reset(false);
        doDraw();
    }

    public void setUpResetTask() {

        Log.v(TAG, "Setting up repetitive reset task");

        long timeFromLastTimeout = System.currentTimeMillis() - lastResetTaskRequestTime.getTime();
        long nextTimeOut = TIMEOUT_MS - timeFromLastTimeout;
        if (nextTimeOut < 0) nextTimeOut = 0;

        Log.d(TAG, "Next time out (ms): " + nextTimeOut);

        scheduledResetTask = scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {

                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "scheduledResetTask run");
                                lastResetTaskRequestTime = new Date();
                                resetOnNextUpdate = true;
                                queryCameraView();
                            }
                        });
                    }
                },
                nextTimeOut,
                TIMEOUT_MS,
                TimeUnit.MILLISECONDS);

    }

    private void reset(boolean clearSpots) {
        Log.d(TAG, "Reset: " + clearSpots);
        for (Marker marker : spotMarkersMap.values()) {
            marker.remove();
        }
        queriedBounds.clear();
        if (clearSpots) spots.clear();
        spotMarkersMap.clear();
    }

    /**
     * Set the bounds where the camera is currently looking.
     * Retrieve parking spots from the current viewport
     *
     * @return
     */
    public boolean queryCameraView() {

        if (getMap() == null) return false;

        // What the user is actually seeing right now
        setUpViewBounds();

        Location ne = new Location("");
        ne.setLatitude(viewBounds.northeast.latitude);
        ne.setLongitude(viewBounds.northeast.longitude);

        Location sw = new Location("");
        sw.setLatitude(viewBounds.northeast.latitude);
        sw.setLongitude(viewBounds.northeast.longitude);

        // distance we are adding to the bounds query on each one of the 4 sides to get also results outside the screen
        float offset = Math.min(ne.distanceTo(sw) / 2, OFFSET_METERS);

        // A broader space we want to query so that data is there when we move the camera
        this.extendedViewBounds = LatLngBounds.builder()
                .include(getOffsetLatLng(viewBounds.northeast, offset, offset))
                .include(getOffsetLatLng(viewBounds.southwest, -offset, -offset))
                .build();

        /**
         * Check if this query is already contained in another one
         */
        if (!resetOnNextUpdate) {
            for (LatLngBounds latLngBounds : queriedBounds) {
                if (latLngBounds.contains(viewBounds.northeast) && latLngBounds.contains(viewBounds.southwest)) {
                    Log.v(QUERY_TAG, "No need to query again camera");
                    return false;
                }
            }
        }

        // we keep a reference of the current query to prevent repeating it
        queriedBounds.add(extendedViewBounds);

        ParkingSpotsQuery areaQuery = new ParkingSpotsQuery(extendedViewBounds, this);

        Log.d(QUERY_TAG, "Starting query for queryBounds: " + extendedViewBounds);
        areaQuery.execute();

        return true;

    }

    private void setUpViewBounds() {
        this.viewBounds = getViewPortBounds();
    }


    /**
     * Called when new parking spots are received
     *
     * @param result
     */
    @Override
    public void onSpotsUpdate(ParkingSpotsQuery query, @NonNull ParkingQueryResult result) {

        Log.v(TAG, "onSpotsUpdate");

        if (isMapReady() && isResumed() && result.moreResults) {
            maxZoom = getMap().getCameraPosition().zoom;
            Log.d(TAG, "maxZoom set to " + maxZoom);
            if (BuildConfig.DEBUG)
                Toast.makeText(getActivity(), "maxZoom set to " + maxZoom, Toast.LENGTH_SHORT).show();
        }

        Set<ParkingSpot> parkingSpots = result.spots;

        if (resetOnNextUpdate) {
            reset(true);
            resetOnNextUpdate = false;
        }

        /**
         * We can consider that after an update, all
         */
        if (!parkingSpots.isEmpty() && !result.moreResults) {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (ParkingSpot spot : parkingSpots) {
                builder.include(spot.getLatLng());
            }
            queriedBounds.add(builder.build());
        }

        spots.addAll(parkingSpots);

        doDraw();
    }

    /**
     * On getting an error when retrieving spots
     *
     * @param statusCode
     * @param reasonPhrase
     */
    @Override
    public void onServerError(ParkingSpotsQuery query, int statusCode, String reasonPhrase) {
        if (BuildConfig.DEBUG)
            Toast.makeText(getActivity(), "Error: " + statusCode, Toast.LENGTH_SHORT).show();
    }

    public void doDraw() {

        if (!isMapReady() || !isResumed()) return;

        Log.v(TAG, "doDraw");

        setUpViewBounds();

        if (!areMarkersDisplayed) {
            Log.v(TAG, "Abort drawing spots. Markers are hidden");
            for (Marker marker : spotMarkersMap.values()) {
                marker.setVisible(false);
            }
            return;
        }

        if (selectedSpot != null) {
            drawDirections();
            drawSelectedMarker();
        }

        int displayedMarkers = 0;

        for (final ParkingSpot parkingSpot : spots) {

            LatLng spotPosition = parkingSpot.getLatLng();

            Marker marker = spotMarkersMap.get(parkingSpot);
            if (viewBounds.contains(spotPosition)) {

                Log.v(TAG, parkingSpot.toString());

                if (displayedMarkers > MARKERS_LIMIT) {
                    Log.v(TAG, "Marker display limit reached");
                    break;
                }

                // if there is no marker we create it
                if (marker == null) {
                    marker = getMap().addMarker(MarkerFactory.getSpotMarker(parkingSpot, getActivity()));
                    revealMarker(marker);
                    spotMarkersMap.put(parkingSpot, marker);
                }

                // else we may need to update it
                else {
                    updateMarker(parkingSpot, marker);
                    marker.setVisible(true);
                }
                displayedMarkers++;

                marker.setTag(parkingSpot);

            } else {
                if (marker != null) marker.setVisible(false);
            }

        }
    }


    private void updateMarker(@NonNull ParkingSpot parkingSpot, @NonNull Marker marker) {
        MarkerOptions markerOptions = MarkerFactory.getSpotMarker(parkingSpot, getActivity());
        marker.setIcon(markerOptions.getIcon());
    }

    private void drawDirections() {
        if (selectedSpot != null && isActive) {

            LatLng userLatLng = getUserLatLng();
            updateTooFar(selectedSpot.getLatLng(), userLatLng);

            // don't set if they are too far
            if (isTooFar()) return;

            if (userLatLng != null) {
                directionsDelegate.setColor(getResources().getColor(selectedSpot.getMarkerType().colorId));
                directionsDelegate.drawDirections(userLatLng, selectedSpot.getLatLng(), GMapV2Direction.MODE_DRIVING);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        clearSelectedSpot();

        Object object = marker.getTag();
        if (object instanceof ParkingSpot) {

            isActive = true;

            // apply new style and tell listener
            selectedSpot = (ParkingSpot) object;

            drawSelectedMarker();
            drawDirections();

            detailsViewManager.setDetailsFragment(this, SpotDetailsFragment.newInstance(selectedSpot, userLocation));

            Tracking.sendEvent(Tracking.CATEGORY_MAP, Tracking.ACTION_FREE_SPOT_SELECTED);

            return true;
        } else {
            selectedSpot = null;
            return false;
        }

    }

    private void drawSelectedMarker() {

        if (!isMapReady() || !isResumed()) return;

        if (selectedMarker != null) {
            selectedMarker.remove();
        }

        if (selectedSpot != null) {
            Marker spotMarker = spotMarkersMap.get(selectedSpot);
            if (spotMarker != null) spotMarker.remove();
            selectedMarker = getMap().addMarker(MarkerFactory.getSelectedMarker(getActivity(), selectedSpot.getLatLng()));

            spotMarker = getMap().addMarker(MarkerFactory.getSpotMarker(selectedSpot, getActivity()));
            spotMarkersMap.put(selectedSpot, spotMarker);
        }
    }


    @Override
    protected void onUserLocationChanged(Location userLocation) {
        drawDirections();
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        Log.v(TAG, "scheduledResetTask canceled");
        scheduledResetTask.cancel(true);
    }

    @Override
    protected void onActiveStatusChanged(boolean active) {
        if (active) {
            drawDirections();
        } else {
            clearSelectedSpot();
        }
    }

    private void revealMarker(@NonNull final Marker marker) {

        marker.setVisible(true);
//        final float dAlpha = 0.04F;
//        marker.setAlpha(0);
//
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//                float alpha = marker.getAlpha() + dAlpha;
//                if (alpha < 1) {
//                    // Post again 12ms later.
//                    marker.setAlpha(alpha);
//                    handler.postDelayed(this, 16);
//                } else {
//                    marker.setAlpha(1);
//                    // animation ended
//                }
//            }
//        }, (long) (Math.random() * 100));

    }


    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

        if (!isMapReady() || !isResumed()) return;

        float zoom = getMap().getCameraPosition().zoom;
        Log.v(TAG, "zoom: " + zoom);

        /**
         * Query for current camera position
         */
        if (zoom >= maxZoom) {
            Log.v(TAG, "Querying because we are close enough");

            queryCameraView();

            areMarkersDisplayed = true;
        }
        /**
         * Too far
         */
        else {
            Log.d(TAG, "Too far to query locations");
            areMarkersDisplayed = false;
        }

        doDraw();

    }

    @Override
    public void onMapResized() {
        updateCameraIfFollowing();
    }

    /**
     * Clear previously selected spot
     */
    private void clearSelectedSpot() {

        Log.d(TAG, "Clearing selected spot");

        // clear previous selection
        if (selectedMarker != null) {
            selectedMarker.remove();
            selectedMarker = null;
        }

        directionsDelegate.hide(true);
        selectedSpot = null;
    }

    private boolean updateCameraIfFollowing() {

        if (!isMapReady() || !isResumed()) return false;

        if (following) {
            return zoomToSeeBoth();
        }

        return false;
    }

    @NonNull
    private LatLng getOffsetLatLng(@NonNull LatLng original, float offsetNorth, float offsetEast) {

        // Coordinate offsets in radians
        double dLat = offsetNorth / EARTH_RADIUS;
        double dLon = offsetEast / (EARTH_RADIUS * Math.cos(Math.PI * original.latitude / 180));

        // OffsetPosition, decimal degrees
        double nLat = original.latitude + dLat * 180 / Math.PI;
        double nLon = original.longitude + dLon * 180 / Math.PI;

        return new LatLng(nLat, nLon);
    }

    public void setCameraFollowing(boolean following) {
        Log.v(TAG, "Setting camera following mode to " + following);

        this.following = following;

        updateCameraIfFollowing();
    }

    /**
     * This method zooms to see both user and the car.
     */
    private boolean zoomToSeeBoth() {

        if (!isAdded()) return false;

        Log.v(TAG, "zoomToSeeBoth");

        LatLng spotPosition = selectedSpot.getLatLng();
        LatLng userPosition = getUserLatLng();

        if (spotPosition == null || userPosition == null) return false;

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(spotPosition)
                .include(userPosition);

        for (LatLng latLng : directionsDelegate.getDirectionPoints())
            builder.include(latLng);

        delegateManager.doCameraUpdate(CameraUpdateFactory.newLatLngBounds(builder.build(), 100), this);

        return true;
    }

    @Override
    public float getDirectionsMaxDistance() {
        return MAX_DIRECTIONS_DISTANCE;
    }

    public boolean isFollowing() {
        return following;
    }


}
