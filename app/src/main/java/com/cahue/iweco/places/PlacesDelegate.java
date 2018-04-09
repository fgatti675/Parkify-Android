package com.cahue.iweco.places;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.cahue.iweco.AbstractMarkerDelegate;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.DirectionsDelegate;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.R;
import com.cahue.iweco.spots.MarkerFactory;
import com.cahue.iweco.util.GMapV2Direction;
import com.cahue.iweco.util.Tracking;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by f.gatti.gomez on 31/05/16.
 */
public class PlacesDelegate extends AbstractMarkerDelegate {

    public static final String FRAGMENT_TAG = "PLACES_DELEGATE";

    // higher means closer zoom
    private final static float MAX_ZOOM = 13F;
    // how many markers
    private final static int MAX_MARKERS_PER_REQUEST = 20;
    private final static int MAX_DISPLAYED_MARKERS = 30;

    private static final String TAG = PlacesDelegate.class.getSimpleName();

    // location used as a center fos nearby query
    private LatLng lastUserLatLngQueried;
    private List<LatLngBounds> queriedBounds = new ArrayList<>();
    private JsonRequest currentUserLocationRequest;

    private Set<Place> places = new HashSet<>();
    private Set<Marker> displayedMarker = new HashSet<>();

    private Map<Place, Marker> placeMarkerMap = new HashMap<>();

    @Nullable
    private Marker selectedMarker;

    /**
     * Directions delegate
     */
    private DirectionsDelegate directionsDelegate;

    @Nullable
    private Place selectedPlace;


    private Handler handler = new Handler();
    private Random random = new Random();

    @NonNull
    public static PlacesDelegate newInstance() {
        PlacesDelegate fragment = new PlacesDelegate();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        directionsDelegate = new DirectionsDelegate();
        directionsDelegate.setColor(getResources().getColor(R.color.theme_primary));
    }

    @Override
    protected void onMapReady(GoogleMap map) {
        super.onMapReady(map);
        directionsDelegate.setMap(map);
        doDraw();
    }

    public void doDraw() {

        if (!isMapReady() || !isResumed()) return;

        displayedMarker.clear();

        LatLngBounds viewPortBounds = getViewPortBounds();
        for (Place place : places) {
            if (viewPortBounds.contains(place.getLatLng())) {
                Marker marker = placeMarkerMap.get(place);
                if (marker == null) {
                    final Marker newMarker = getMap().addMarker(MarkerFactory.getParkingMarker(place, getActivity()));
                    placeMarkerMap.put(place, newMarker);
                    newMarker.setTag(place);
                    newMarker.setAlpha(0);
                    handler.postDelayed(new Runnable() {
                        float alpha = 0;

                        @Override
                        public void run() {
                            alpha += 0.04;
                            newMarker.setAlpha(alpha);
                            if (alpha < 1) {
                                handler.postDelayed(this, 16);
                            }
                        }
                    }, random.nextInt(300));
                    marker = newMarker;
                } else {
                    marker.setVisible(true);
                    marker.setAlpha(1);
                }

                displayedMarker.add(marker);

                if (displayedMarker.size() == MAX_DISPLAYED_MARKERS) break;
            }
        }

        if (selectedPlace != null) {
            drawDirections();
            drawSelectedMarker();
        }

    }

    public void fadeOutMarkers(CameraPosition cameraPosition) {
        float zoom = cameraPosition.zoom;
        if (zoom < MAX_ZOOM) {
            for (final Marker marker : placeMarkerMap.values()) {
                handler.postDelayed(new Runnable() {
                    float alpha = 1;

                    @Override
                    public void run() {
                        alpha -= 0.05;
                        marker.setAlpha(alpha);
                        if (alpha <= 0) {
                            marker.setVisible(false);
                        } else {
                            handler.postDelayed(this, 16);
                        }
                    }
                }, random.nextInt(200));
            }

            clearSelectedMarker();

            placeMarkerMap.clear();

        } else {
            handler.removeCallbacks(null);
        }
    }

    private void drawDirections() {
        if (selectedPlace != null && isActive) {
            LatLng userLatLng = getUserLatLng();
            if (userLatLng != null)
                directionsDelegate.drawDirections(userLatLng, selectedPlace.getLatLng(), GMapV2Direction.MODE_DRIVING);
        }
    }


    private void drawSelectedMarker() {

        if (!isMapReady() || !isResumed()) return;

        for (Marker otherMarkers : placeMarkerMap.values()) {
            otherMarkers.setZIndex(0);
        }

        if (selectedMarker != null) {
            selectedMarker.remove();
        }

        if (selectedPlace != null) {
            Marker spotMarker = placeMarkerMap.get(selectedPlace);
            if (spotMarker == null) {
                spotMarker = getMap().addMarker(MarkerFactory.getParkingMarker(selectedPlace, getActivity()));
                placeMarkerMap.put(selectedPlace, spotMarker);
            }
            spotMarker.setZIndex(2);

            selectedMarker = getMap().addMarker(MarkerFactory.getSelectedMarker(getActivity(), selectedPlace.getLatLng()));
            selectedMarker.setZIndex(1);

        }
    }


    @Override
    public boolean onMarkerClick(Marker marker) {

        selectedPlace = null;
        clearSelectedMarker();

        // apply new style and tell listener
        Object tag = marker.getTag();
        if (tag instanceof Place) {

            isActive = true;

            selectedPlace = (Place) tag;

            drawSelectedMarker();
            drawDirections();

            detailsViewManager.setDetailsFragment(this, PlaceDetailsFragment.newInstance(selectedPlace, userLocation));

            Tracking.sendEvent(Tracking.CATEGORY_MAP, Tracking.ACTION_PARKING_SELECTED);

            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
            Bundle bundle = new Bundle();
            firebaseAnalytics.logEvent("public_parking_click", bundle);

            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActiveStatusChanged(boolean active) {
        if (active) {
            drawDirections();
        } else {
            selectedPlace = null;
            clearSelectedMarker();
        }
    }

    @Override
    protected void onUserLocationChanged(Location userLocation) {

        if (lastUserLatLngQueried != null) {
            Location lastUserLocation = new Location("");
            lastUserLocation.setLatitude(lastUserLatLngQueried.latitude);
            lastUserLocation.setLongitude(lastUserLatLngQueried.longitude);
            if (lastUserLocation.distanceTo(userLocation) < 300)
                return;
        }

        makeUserLocationPlacesRequest(getUserLatLng());

    }

    private void makeUserLocationPlacesRequest(final LatLng latLng) {

        if (currentUserLocationRequest != null) return;

        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        String url = new Uri.Builder().scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("places")
                .appendQueryParameter("lat", Double.toString(latLng.latitude))
                .appendQueryParameter("long", Double.toString(latLng.longitude))
                .appendQueryParameter("radius", String.valueOf(1800))
                .build().toString();

        Log.d(TAG, "Places query: " + url);

        currentUserLocationRequest = new JsonObjectRequest(
                url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        lastUserLatLngQueried = latLng;
                        currentUserLocationRequest = null;
                        PlacesQueryResult parkingQueryResult = parseResult(response);
                        onPlacesUpdate(parkingQueryResult);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        currentUserLocationRequest = null;
                        error.printStackTrace();
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(currentUserLocationRequest);
    }

    /**
     * Clear previously selected spot
     */
    private void clearSelectedMarker() {

        Log.d(TAG, "Clearing selected spot");

        // clear previous selection
        if (selectedMarker != null) {
            selectedMarker.remove();
            selectedMarker = null;
        }

        directionsDelegate.hide(true);
    }

    private void makePlacesViewPortRequest() {

        final LatLngBounds viewPortBounds = getViewPortBounds();

        for (LatLngBounds latLngBounds : queriedBounds) {
            if (latLngBounds.contains(viewPortBounds.northeast) && latLngBounds.contains(viewPortBounds.southwest)) {
                Log.d(TAG, "Viewport contained in previous request");
                return;
            }
        }

        LatLng viewPortCenter = viewPortBounds.getCenter();
        int displayedMarkers = 0;

        if (displayedMarker.size() == MAX_DISPLAYED_MARKERS) {
            Log.d(TAG, "Too many places in viewport");
            return;
        }

        float distances[] = new float[3];
        for (Place place : places) {
            Location.distanceBetween(
                    viewPortCenter.latitude,
                    viewPortCenter.longitude,
                    place.getLatLng().latitude,
                    place.getLatLng().longitude,
                    distances);

            if (distances[0] < getRadiusFromCenter()) {
                displayedMarkers++;
                if (displayedMarkers > MAX_MARKERS_PER_REQUEST) {
                    Log.d(TAG, "Too many places around center");
                    return;
                }
            }
        }


        int distance = getRadiusFromCenter();

        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        String url = new Uri.Builder().scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("places")
                .appendQueryParameter("lat", Double.toString(viewPortBounds.getCenter().latitude))
                .appendQueryParameter("long", Double.toString(viewPortBounds.getCenter().longitude))
                .appendQueryParameter("radius", String.valueOf(distance))
                .build().toString();

        Log.d(TAG, "Places query: " + url);

        Request request = new JsonObjectRequest(
                url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        PlacesQueryResult parkingQueryResult = parseResult(response);
                        if (!parkingQueryResult.moreResults) {
                            LatLngBounds.Builder builder = LatLngBounds.builder();
                            builder.include(viewPortBounds.northeast);
                            builder.include(viewPortBounds.southwest);
                            for (Place place : parkingQueryResult.places)
                                builder.include(place.getLatLng());
                            queriedBounds.add(builder.build());
                        }
                        onPlacesUpdate(parkingQueryResult);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        queriedBounds.remove(viewPortBounds);
                        error.printStackTrace();
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(request);
    }

    /**
     * Get radius from center of viewport to one of the sides
     *
     * @return
     */
    private int getRadiusFromCenter() {
        LatLngBounds viewPortBounds = getViewPortBounds();
        float distances[] = new float[3];
        Location.distanceBetween(
                viewPortBounds.getCenter().latitude,
                viewPortBounds.getCenter().longitude,
                viewPortBounds.northeast.latitude,
                viewPortBounds.northeast.longitude,
                distances);
        return (int) (distances[0] / 1.42);
    }

    private void onPlacesUpdate(PlacesQueryResult parkingQueryResult) {
        places.addAll(parkingQueryResult.places);
        doDraw();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (cameraPosition.zoom > MAX_ZOOM) {
            makePlacesViewPortRequest();
            doDraw();
        } else {
            fadeOutMarkers(cameraPosition);
        }

    }

    @Override
    public void setCameraFollowing(boolean following) {

    }

    @Override
    public void onMapResized() {

    }

    @NonNull
    protected PlacesQueryResult parseResult(@Nullable JSONObject jsonObject) {

        PlacesQueryResult result = new PlacesQueryResult();

        Set<Place> places = new HashSet<>();

        result.places = places;

        if (jsonObject != null) {
            try {

                JSONArray spotsArray = jsonObject.getJSONArray("places");
                for (int i = 0; i < spotsArray.length(); i++) {
                    JSONObject entry = spotsArray.getJSONObject(i);
                    Place place = Place.fromJSON(entry);
                    places.add(place);
                }

                result.moreResults = jsonObject.getBoolean("moreResults");

            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("Error parsing places jsonObject", e);
            }
        }

        return result;
    }
}
