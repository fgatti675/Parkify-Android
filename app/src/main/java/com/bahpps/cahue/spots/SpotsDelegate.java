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

    // Earth’s radius, sphere
    private final static double EARTH_RADIUS = 6378137;
    private final static long TIMEOUT_MS = 60000;

    // number of spots being retrieved on nearby spots query
    private final static int CLOSEST_LOCATIONS = 100;

    // max number of spots displayed at once
    private static final int MARKERS_LIMIT = 100;

    private final Handler handler = new Handler();

    /**
     * If zoom is more far than this, we don't display the markers
     */
    public final static float MAX_ZOOM = 0F;

    private Set<ParkingSpot> spots;
    private Map<ParkingSpot, Marker> spotMarkersMap;
    private GoogleMap mMap;

    private Context mContext;

    // in the next fetching of spots, clear the previous state
    private boolean shouldBeReset = false;

    private List<LatLngBounds> queriedBounds = new CopyOnWriteArrayList<LatLngBounds>();

    private LatLngBounds viewBounds;
    private LatLngBounds extendedViewBounds;
    private ParkingSpotsQuery areaQuery;

    private Date lastResetTaskRequestTime;
    private ScheduledFuture scheduledResetTask;

    /**
     * If markers shouldn't be displayed (like zoom is too far)
     */
    private boolean hideMarkers = false;

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


    public void init(Context context, GoogleMap map) {

        this.mContext = context;
        this.mMap = map;

        // we can rebuild this map because markers are removed on init
        spotMarkersMap = new HashMap<ParkingSpot, Marker>();

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
                        repeatLastQuery();
                    }
                },
                TIMEOUT_MS,
                TIMEOUT_MS,
                TimeUnit.MILLISECONDS);

    }

    private void reset() {
        Log.d(TAG, "Spots reset");
        queriedBounds.clear();
        spots.clear();
        spotMarkersMap.clear();
    }

    private boolean repeatLastQuery() {
        return queryCameraView();
    }

    private synchronized boolean queryClosestSpots(LatLng userLocation) {

        this.userQueryLocation = userLocation;

        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING)
            return false;

        nearbyQuery = new CartoDBParkingSpotsQuery(this);

        Log.d(TAG, "Starting query for closest spots to: " + userLocation);
        nearbyQuery.retrieveNearbySpots(userLocation, CLOSEST_LOCATIONS);

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
        this.viewBounds = mMap.getProjection().getVisibleRegion().latLngBounds;

        if (nearbyQuery != null && nearbyQuery.getStatus() == AsyncTask.Status.RUNNING && viewBounds.contains(userQueryLocation)) {
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
                    Log.d(TAG, "NO need to query again");
                    return false;
                }
            }
        }

        // we keep a reference of the current query to prevent repeating it
        queriedBounds.add(extendedViewBounds);

        areaQuery = new CartoDBParkingSpotsQuery(this);

        Log.d(TAG, "Starting query for queryBounds: " + extendedViewBounds);
        areaQuery.retrieveLocationsIn(extendedViewBounds);

        return true;

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
    public void onError(ParkingSpotsQuery query) {
        // do something if there is an error
        Toast.makeText(mContext, "Check internet connection", Toast.LENGTH_SHORT).show();
    }


    public void doDraw() {

        // clear first
        clear();

        if (hideMarkers) return;

        Log.d(TAG, "Drawing spots");
        int i = 0;
        for (ParkingSpot parkingSpot : spots) {
            LatLng spotPosition = parkingSpot.getPosition();

            Marker marker = spotMarkersMap.get(parkingSpot);
            boolean fadeIn = false; // the marker will fade in when appearing for the first time

            if (marker == null) {
                BitmapDescriptor markerBitmap = MarkerFactory.getMarkerBitmap(parkingSpot, mContext);
                marker = mMap.addMarker(new MarkerOptions().icon(markerBitmap).position(spotPosition));
                marker.setVisible(false);
                spotMarkersMap.put(parkingSpot, marker);
                fadeIn = true;
            }

            if (!marker.isVisible() && viewBounds.contains(spotPosition)) {
                makeMarkerVisible(marker, fadeIn);
            }
            i++;
            if(i > MARKERS_LIMIT) return;
        }
    }


    private void clear() {
        for (Marker marker : spotMarkersMap.values()) {
            if (hideMarkers || !viewBounds.contains(marker.getPosition()))
                marker.setVisible(false);
        }
    }

    @Override
    public void onResume() {
        setUpResetTask();
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

            queryCameraView();

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

    private Date lastNearbyQuery;

    @Override
    public void onLocationChanged(Location location) {
        LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());

        if (lastNearbyQuery == null || System.currentTimeMillis() - lastNearbyQuery.getTime() < TIMEOUT_MS)
            queryClosestSpots(userLocation);

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
