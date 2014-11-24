package com.bahpps.cahue.spots;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import com.bahpps.cahue.AbstractMarkerDelegate;
import com.bahpps.cahue.spots.query.CartoDBParkingSpotsQuery;
import com.bahpps.cahue.spots.query.ParkingSpotsQuery;
import com.bahpps.cahue.util.MarkerFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Delegate in charge of querying and drawing parking spots in the map.
 * <p/>
 * Created by Francesco on 21/10/2014.
 */
public class SpotsDelegate extends AbstractMarkerDelegate implements Parcelable, ParkingSpotsQuery.ParkingSpotsUpdateListener {

    private final static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    private static final String TAG = "SpotsDelegate";
    private static final String QUERY_TAG = "SpotsDelegateQuery";

    // Earth’s radius, sphere
    private final static double EARTH_RADIUS = 6378137;
    private final static long TIMEOUT_MS = 60000;

    // number of spots being retrieved on nearby spots query
    private final static int CLOSEST_LOCATIONS = 200;

    // max number of spots displayed at once
    private static final int MARKERS_LIMIT = 100;

    private final Handler handler = new Handler();

    /**
     * If zoom is more far than this, we don't display the markers
     */
    public final static float MAX_ZOOM = 0F;

    private Set<ParkingSpot> spots;
    private Map<ParkingSpot, Marker> spotMarkersMap;
    private Map<Marker, ParkingSpot> markerSpotsMap;

    private GoogleMap mMap;

    private Context mContext;
    private SpotSelectedListener spotSelectedListener;

    // In the next spots update, clear the previous state
    private boolean shouldBeReset = false;

    private List<LatLngBounds> queriedBounds = new CopyOnWriteArrayList<LatLngBounds>();

    private LatLngBounds viewBounds;
    private LatLngBounds extendedViewBounds;

    private Date lastResetTaskRequestTime;
    private ScheduledFuture scheduledResetTask;

    /**
     * If markers shouldn't be displayed (like zoom is too far)
     */
    private boolean markersDisplayed = false;

    // location used as a center fos nearby spots query
    private LatLng userQueryLocation;
    private ParkingSpotsQuery nearbyQuery;


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
        ParkingSpot[] spotsArray = parcel.createTypedArray(ParkingSpot.CREATOR);
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
        parcel.writeTypedArray(spots.toArray(spotsArray), 0);
        LatLngBounds[] boundsArray = new LatLngBounds[queriedBounds.size()];
        parcel.writeParcelableArray(queriedBounds.toArray(boundsArray), 0);
        parcel.writeParcelable(viewBounds, 0);
        parcel.writeByte((byte) (shouldBeReset ? 1 : 0));
        parcel.writeSerializable(lastResetTaskRequestTime);
    }


    public void init(Context context, GoogleMap map, SpotSelectedListener spotSelectedListener) {

        this.mContext = context;
        this.mMap = map;
        this.spotSelectedListener = spotSelectedListener;

        // we can rebuild this map because markers are removed on init
        spotMarkersMap = new HashMap<ParkingSpot, Marker>();
        markerSpotsMap = new HashMap<Marker, ParkingSpot>();

    }

    public void setUpResetTask() {

        Log.d(TAG, "Setting up repetitive reset task");

        long timeFromLastTimeout = System.currentTimeMillis() - lastResetTaskRequestTime.getTime();
        long nextTimeOut = TIMEOUT_MS - timeFromLastTimeout;
        if (nextTimeOut < 0) nextTimeOut = 0;

        Log.d(TAG, "Next time out (ms): " + nextTimeOut);

        scheduledResetTask = scheduledExecutorService.scheduleAtFixedRate(
                new Runnable() {
                    public void run() {
                        Log.d(TAG, "scheduledResetTask run");
                        lastResetTaskRequestTime = new Date();
                        shouldBeReset = true;
                        Toast.makeText(mContext, "Reset task fired", Toast.LENGTH_SHORT).show();
                        repeatLastQuery();
                    }
                },
                nextTimeOut,
                TIMEOUT_MS,
                TimeUnit.MILLISECONDS);

    }

    private void reset() {
        Log.d(TAG, "Spots reset");
        for (Marker marker : markerSpotsMap.keySet()) {
            marker.remove();
        }
        queriedBounds.clear();
        spots.clear();
        spotMarkersMap.clear();
        markerSpotsMap.clear();
    }

    private boolean repeatLastQuery() {
        return queryCameraView();
    }

    private synchronized boolean queryClosestSpots(LatLng userLocation) {

        this.userQueryLocation = userLocation;

        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING) {
            return false;
        }

        nearbyQuery = new CartoDBParkingSpotsQuery(this);

        Log.d(QUERY_TAG, "Starting query for closest spots to: " + userLocation);
        nearbyQuery.retrieveNearbySpots(userLocation, CLOSEST_LOCATIONS);

        Toast.makeText(mContext, "queryClosestSpots", Toast.LENGTH_SHORT).show();

        return true;
    }

    /**
     * Set the bounds where the camera is currently looking.
     * A query is done
     *
     * @return
     */
    private synchronized boolean queryCameraView() {

        // What the user is actually seeing right now
        setUpViewBounds();

        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING && viewBounds.contains(userQueryLocation)) {
            Log.d(QUERY_TAG, "Abort camera query because view contains user");
            return false;
        }

        // A broader space we want to query so that data is there when we move the camera
        this.extendedViewBounds = LatLngBounds.builder()
                .include(getOffsetLatLng(viewBounds.northeast, 500, 500))
                .include(getOffsetLatLng(viewBounds.southwest, -500, -500))
                .build();

        /**
         * Check if this query is already contained in another one
         */
        if (!shouldBeReset) {
            for (LatLngBounds latLngBounds : queriedBounds) {
                if (latLngBounds.contains(viewBounds.northeast) && latLngBounds.contains(viewBounds.southwest)) {
                    Log.d(QUERY_TAG, "No need to query again camera");
                    return false;
                }
            }
        }

        // we keep a reference of the current query to prevent repeating it
        queriedBounds.add(extendedViewBounds);

        ParkingSpotsQuery areaQuery = new CartoDBParkingSpotsQuery(this);

        Log.d(QUERY_TAG, "Starting query for queryBounds: " + extendedViewBounds);
        areaQuery.retrieveLocationsIn(extendedViewBounds);

        Toast.makeText(mContext, "queryCameraView", Toast.LENGTH_SHORT).show();

        return true;

    }

    private void setUpViewBounds() {
        this.viewBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
    }

    /**
     * Called when new parking spots are received
     *
     * @param parkingSpots
     */
    @Override
    public synchronized void onSpotsUpdate(ParkingSpotsQuery query, Set<ParkingSpot> parkingSpots) {

        if (query == nearbyQuery)
            lastNearbyQuery = new Date();

        if (shouldBeReset) {
            reset();
            shouldBeReset = false;
        }

        /**
         * We can consider that after an update, all
         */
        if (!parkingSpots.isEmpty()) {
            LatLngBounds.Builder builder = LatLngBounds.builder();
            for (ParkingSpot spot : parkingSpots) {
                builder.include(spot.getPosition());
            }
            queriedBounds.add(builder.build());
        }

        spots.addAll(parkingSpots);

        doDraw();
    }

    /**
     * On getting an error when retrieving spots
     */
    @Override
    public void onSpotsUpdateError(ParkingSpotsQuery query) {
        // do something if there is an error
        Toast.makeText(mContext, "Check internet connection", Toast.LENGTH_SHORT).show();
    }


    public void doDraw() {

        // hideMarkers first
        hideMarkers();

        if (!markersDisplayed) {
            Log.d(TAG, "Abort drawing spots. Markers are hidden");
            return;
        }

        Log.d(TAG, "Drawing spots");

        setUpViewBounds();

        int displayedMarkers = 0;

        for (ParkingSpot parkingSpot : spots) {
            LatLng spotPosition = parkingSpot.getPosition();

            Marker marker = spotMarkersMap.get(parkingSpot);

            if (marker == null) {
                BitmapDescriptor markerBitmap = MarkerFactory.getMarkerBitmap(parkingSpot, mContext);
                marker = mMap.addMarker(new MarkerOptions().icon(markerBitmap).position(spotPosition));
                marker.setVisible(false);
                spotMarkersMap.put(parkingSpot, marker);
                markerSpotsMap.put(marker, parkingSpot);
            }

            if (!marker.isVisible() && viewBounds.contains(spotPosition)) {
                makeMarkerVisible(marker, parkingSpot);
                displayedMarkers++;
            }

            if (displayedMarkers > MARKERS_LIMIT) {
                Log.d(TAG, "Marker display limit reached");
                return;
            }
        }
    }


    /**
     * Hide non visible markers (outside of viewport)
     */
    private void hideMarkers() {
        for (Marker marker : markerSpotsMap.keySet()) {
            if (!markersDisplayed || !viewBounds.contains(marker.getPosition()))
                marker.setVisible(false);
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        ParkingSpot spot = markerSpotsMap.get(marker);
        if (spot != null)
            spotSelectedListener.onSpotSelected(spot);
        return true;
    }

    @Override
    public void onResume() {
        setUpResetTask();
        doDraw();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "scheduledResetTask canceled");
        scheduledResetTask.cancel(true);
    }

    private void makeMarkerVisible(final Marker marker, ParkingSpot spot) {

        marker.setVisible(true);
        final float dAlpha = spot.getMarkerType().dAlpha;
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
    public void onCameraChange(CameraPosition cameraPosition) {
        float zoom = mMap.getCameraPosition().zoom;
        Log.v(TAG, "zoom: " + zoom);

        /**
         * Query for current camera position
         */
        if (zoom >= MAX_ZOOM) {
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

        markAsDirty();

    }

    private Date lastNearbyQuery;

    @Override
    public void onLocationChanged(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

        if (lastNearbyQuery == null || System.currentTimeMillis() - lastNearbyQuery.getTime() > TIMEOUT_MS)
            queryClosestSpots(userLocation);
        else
            Log.d(TAG, "No need to query for closest points again");

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

    public interface SpotSelectedListener {
        void onSpotSelected(ParkingSpot spot);
    }

}
