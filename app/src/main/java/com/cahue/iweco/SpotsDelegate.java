package com.cahue.iweco;

import android.app.Activity;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.spots.MarkerFactory;
import com.cahue.iweco.spots.ParkingSpot;
import com.cahue.iweco.spots.SpotDetailsFragment;
import com.cahue.iweco.spots.query.AreaSpotsQuery;
import com.cahue.iweco.spots.query.ParkingSpotsQuery;
import com.cahue.iweco.spots.query.QueryResult;
import com.cahue.iweco.util.GMapV2Direction;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
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
 * <p/>
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate extends AbstractMarkerDelegate
        implements ParkingSpotsQuery.ParkingSpotsUpdateListener,
        CameraUpdateRequester {

    // distance we are adding to the bounds query on each one of the 4 sides to get also results outside the screen
    public static final int OFFSET_METERS = 2000;
    public static final String FRAGMENT_TAG = "SPOTS_DELEGATE";
    
    /**
     * If zoom is more far than this, we don't display the markers
     */
    public final static float MAX_ZOOM = 5F;
    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    
    private static final String TAG = "SpotsDelegate";
    private static final String QUERY_TAG = "SpotsDelegateQuery";
    
    // Earth’s radius, sphere
    private final static double EARTH_RADIUS = 6378137;
    
    // time after we consider the query is outdated and need to repeat
    private final static long TIMEOUT_MS = 60000;
    
    // number of spots being retrieved on nearby spots query
    private final static int CLOSEST_LOCATIONS = 100;
    
    // max number of spots displayed at once.
    private static final int MARKERS_LIMIT = 100;
    
    private static final float MAX_DIRECTIONS_DISTANCE = 40000; // 40 km
    
    private final Handler handler = new Handler();
    int displayedMarkers;
    private Set<ParkingSpot> spots;
    private Map<ParkingSpot, Marker> spotMarkersMap;
    private Map<Marker, ParkingSpot> markerSpotsMap;

    
    private SpotSelectedListener spotSelectedListener;

    // In the next spots update, clear the previous state
    private boolean shouldBeReset = false;
    private List<LatLngBounds> queriedBounds;
    
    private LatLngBounds viewBounds;
    private LatLngBounds extendedViewBounds;
    
    private Date lastResetTaskRequestTime;
    private ScheduledFuture scheduledResetTask;
    
    private boolean following = false;

    // location used as a center fos nearby spots query
//    private LatLng userQueryLocation;
//    private ParkingSpotsQuery nearbyQuery;

//    private Date lastNearbyQuery;
    /**
     * If markers shouldn't be displayed (like zoom is too far)
     */
    private boolean markersDisplayed = false;
    
    private ParkingSpot selectedSpot;
    
    private float maxZoom;
    
    /**
     * Directions delegate
     */
    private DirectionsDelegate directionsDelegate;

    public static SpotsDelegate newInstance() {
        SpotsDelegate fragment = new SpotsDelegate();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            setRetainInstance(true);

            queriedBounds = new ArrayList<>();
            spots = new HashSet<>();
            lastResetTaskRequestTime = new Date();

            spotMarkersMap = new HashMap<>();
            markerSpotsMap = new HashMap<>();

            maxZoom = BuildConfig.DEBUG ? 0 : MAX_ZOOM;

            directionsDelegate = new DirectionsDelegate();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.spotSelectedListener = (SpotSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement SpotSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setUpResetTask();
        if (getMap() != null)
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
                                shouldBeReset = true;
                                queryCameraView();
                            }
                        });
                    }
                },
                nextTimeOut,
                TIMEOUT_MS,
                TimeUnit.MILLISECONDS);

    }


//    private synchronized boolean queryClosestSpots(LatLng userLocation) {
//
//        this.userQueryLocation = userLocation;
//
//        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING) {
//            return false;
//        }
//
//        nearbyQuery = new NearestSpotsQuery(mContext, userLocation, CLOSEST_LOCATIONS, this);
//
//        Log.v(QUERY_TAG, "Starting query for closest spots to: " + userLocation);
//        nearbyQuery.execute();
//
//        return true;
//    }

    private void reset(boolean clearSpots) {
        Log.d(TAG, "Spots reset");
        for (Marker marker : markerSpotsMap.keySet()) {
            marker.remove();
        }
        queriedBounds.clear();
        if (clearSpots) spots.clear();
        markerSpotsMap.clear();
        spotMarkersMap.clear();
    }

    /**
     * Set the bounds where the camera is currently looking.
     * A query is done
     *
     * @return
     */
    public boolean queryCameraView() {

        // What the user is actually seeing right now
        setUpViewBounds();

//        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING && viewBounds.contains(userQueryLocation)) {
//            Log.d(QUERY_TAG, "Abort camera query because view contains user");
//            return false;
//        }

        // A broader space we want to query so that data is there when we move the camera
        this.extendedViewBounds = LatLngBounds.builder()
                .include(getOffsetLatLng(viewBounds.northeast, OFFSET_METERS, OFFSET_METERS))
                .include(getOffsetLatLng(viewBounds.southwest, -OFFSET_METERS, -OFFSET_METERS))
                .build();

        /**
         * Check if this query is already contained in another one
         */
        if (!shouldBeReset) {
            for (LatLngBounds latLngBounds : queriedBounds) {
                if (latLngBounds.contains(viewBounds.northeast) && latLngBounds.contains(viewBounds.southwest)) {
                    Log.v(QUERY_TAG, "No need to query again camera");
                    return false;
                }
            }
        }

        // we keep a reference of the current query to prevent repeating it
        queriedBounds.add(extendedViewBounds);

        if (BuildConfig.DEBUG) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    Tracker tracker = getTracker();
                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory("IO")
                            .setAction("query")
                            .setLabel("Spots area query")
                            .build());
                    return null;
                }
            }.execute();
        }
        ParkingSpotsQuery areaQuery = new AreaSpotsQuery(getActivity(), extendedViewBounds, this);

        Log.v(QUERY_TAG, "Starting query for queryBounds: " + extendedViewBounds);
        areaQuery.execute();

        return true;

    }

    private void setUpViewBounds() {
        this.viewBounds = getMap().getProjection().getVisibleRegion().latLngBounds;
    }

    /**
     * Called when new parking spots are received
     *
     * @param result
     */
    @Override
    public void onSpotsUpdate(ParkingSpotsQuery query, QueryResult result) {

        if (result.moreResults) {
            Log.d(TAG, "maxZoom set to " + maxZoom);
            if (BuildConfig.DEBUG)
                Toast.makeText(getActivity(), "maxZoom set to " + maxZoom, Toast.LENGTH_SHORT).show();

            maxZoom = getMap().getCameraPosition().zoom;
        }

        Set<ParkingSpot> parkingSpots = result.spots;

//        if (query == nearbyQuery)
//            lastNearbyQuery = new Date();

        if (shouldBeReset) {
            reset(true);
            shouldBeReset = false;
        }

        /**
         * We can consider that after an update, all
         */
        if (!parkingSpots.isEmpty()) {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (ParkingSpot spot : parkingSpots) {
                builder.include(spot.position);
            }
            queriedBounds.add(builder.build());
        }

        spots.addAll(parkingSpots);

        if (selectedSpot != null && !parkingSpots.contains(selectedSpot)) {
            directionsDelegate.hide(true);
            selectedSpot = null;
        }

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
        Toast.makeText(getActivity(), "Error: " + reasonPhrase, Toast.LENGTH_SHORT).show();
        queryCameraView();
    }

    public void doDraw() {

        Log.d(TAG, "Drawing spots");

        setUpViewBounds();

        // hideMarkers first
        hideMarkers();

        if (!markersDisplayed) {
            Log.d(TAG, "Abort drawing spots. Markers are hidden");
            return;
        }

        for (final ParkingSpot parkingSpot : spots) {

            Log.v(TAG, parkingSpot.toString());

            if (displayedMarkers > MARKERS_LIMIT) {
                Log.v(TAG, "Marker display limit reached");
                return;
            }

            LatLng spotPosition = parkingSpot.position;

            Marker marker = spotMarkersMap.get(parkingSpot);

            // if there is no marker we create it
            if (marker == null) {
                marker = getMap().addMarker(MarkerFactory.getMarker(parkingSpot, getActivity(), parkingSpot.equals(selectedSpot)));
                marker.setVisible(false);
                spotMarkersMap.put(parkingSpot, marker);
                markerSpotsMap.put(marker, parkingSpot);
            }

            // else we may need to update it
            else if (true) { // TODO
                updateMarker(parkingSpot, marker, parkingSpot.equals(selectedSpot));
            }


            if (!marker.isVisible() && viewBounds.contains(spotPosition)) {
                makeMarkerVisible(marker, parkingSpot);
                displayedMarkers++;
            }

        }

        if (selectedSpot != null) {
            drawDirections();
        }
    }

    private void drawDirections() {
        if (selectedSpot != null) {

            LatLng userLatLng = getUserLatLng();
            updateTooFar(getSpotLatLng(), userLatLng);

            // don't set if they are too far
            if (isTooFar()) return;

            if (userLatLng != null)
                directionsDelegate.drawDirections(userLatLng, selectedSpot.position, GMapV2Direction.MODE_DRIVING);
        }
    }

    private void updateMarker(ParkingSpot parkingSpot, Marker marker, boolean selected) {
        MarkerOptions markerOptions = MarkerFactory.getMarker(parkingSpot, getActivity(), selected);
        marker.setIcon(markerOptions.getIcon());
    }

    /**
     * Hide non visible markers (outside of viewport)
     */
    private void hideMarkers() {

        displayedMarkers = 0;

        for (final Marker marker : markerSpotsMap.keySet()) {
            if (!markersDisplayed || !viewBounds.contains(marker.getPosition()))
                marker.setVisible(false);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        clearSelectedSpot();

        // apply new style and tell listener
        selectedSpot = markerSpotsMap.get(marker);

        if (selectedSpot != null) {
            Marker selectedMarker = spotMarkersMap.get(selectedSpot);

            updateMarker(selectedSpot, selectedMarker, true);
            drawDirections();

            spotSelectedListener.onSpotClicked(selectedSpot);

            detailsViewManager.setDetailsFragment(SpotDetailsFragment.newInstance(selectedSpot, userLocation));

            if (BuildConfig.DEBUG) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        Tracker tracker = getTracker();
                        tracker.send(new HitBuilders.EventBuilder()
                                .setCategory("UX")
                                .setAction("click")
                                .setLabel("Spot clicked")
                                .build());
                        return null;
                    }
                }.execute();
            }

            return true;
        }
        return false;
    }

    private Tracker getTracker() {
        Tracker tracker = ((IwecoApp) getActivity().getApplication()).getTracker();
        return tracker;
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {
        drawDirections();
    }

    /**
     * Clear previously selected spot
     */
    private void clearSelectedSpot() {

        Log.d(TAG, "Clearing selected spot");

        // clear previous selection
        if (selectedSpot != null) {
            Marker previousMarker = spotMarkersMap.get(selectedSpot);

            // we may have restored the selected spot but it may not have been drawn (like on device rotation)
            if (previousMarker != null) {
                updateMarker(selectedSpot, previousMarker, false);
            }
        }
        directionsDelegate.hide(true);
        selectedSpot = null;
    }

    @Override
    public void onMapReady(GoogleMap map) {
        Log.d(TAG, "setMap");
        directionsDelegate.setMap(map);
        directionsDelegate.setColor(getResources().getColor(R.color.theme_accent));
        reset(false);
        doDraw();
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "scheduledResetTask canceled");
        scheduledResetTask.cancel(true);
    }

    @Override
    public void onDetailsClosed() {
        clearSelectedSpot();
    }

    private void makeMarkerVisible(final Marker marker, ParkingSpot spot) {

        marker.setVisible(true);
        final float dAlpha = 0.03F;
        marker.setAlpha(0);

        handler.post(new Runnable() {
            @Override
            public void run() {

                float alpha = marker.getAlpha() + dAlpha;
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


    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
        float zoom = getMap().getCameraPosition().zoom;
        Log.v(TAG, "zoom: " + zoom);

//        if (requester != this)
//            following = false;

        /**
         * Query for current camera position
         */
        if (zoom >= maxZoom) {
            Log.d(TAG, "Querying because we are close enough");

            queryCameraView();

            markersDisplayed = true;
        }
        /**
         * Too far
         */
        else {
            Log.d(TAG, "Too far to query locations");
            markersDisplayed = false;
        }

        doDraw();

    }

    @Override
    public void onMapResized() {
        updateCameraIfFollowing();
    }

    public boolean updateCameraIfFollowing() {

        if (getMap() == null || !isResumed()) return false;

        if (following) {
            return zoomToSeeBoth();
        }

        return false;
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

    public void setCameraFollowing(boolean following) {
        Log.v(TAG, "Setting camera following mode to " + following);

        this.following = following;

        updateCameraIfFollowing();
    }

    /**
     * This method zooms to see both user and the car.
     */
    protected boolean zoomToSeeBoth() {

        if (cameraManager == null)
            return false;

        if (!isAdded()) return false;

        Log.v(TAG, "zoomToSeeBoth");

        LatLng carPosition = getSpotLatLng();
        LatLng userPosition = getUserLatLng();

        if (carPosition == null || userPosition == null) return false;

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(carPosition)
                .include(userPosition);

        for (LatLng latLng : directionsDelegate.getDirectionPoints())
            builder.include(latLng);

        cameraManager.onCameraUpdateRequest(CameraUpdateFactory.newLatLngBounds(builder.build(), 100), this);

        return true;
    }

    @Override
    public float getDirectionsMaxDistance() {
        return MAX_DIRECTIONS_DISTANCE;
    }

    private LatLng getSpotLatLng() {
        if (selectedSpot == null) return null;
        return selectedSpot.position;
    }

    public boolean isFollowing() {
        return following;
    }

    public interface SpotSelectedListener {
        void onSpotClicked(ParkingSpot spot);
    }


}
