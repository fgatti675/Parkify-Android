package com.cahue.iweco.places;

import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
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
import com.cahue.iweco.CameraUpdateRequester;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.spots.MarkerFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by f.gatti.gomez on 31/05/16.
 */
public class PlacesDelegate extends AbstractMarkerDelegate {

    public static final String FRAGMENT_TAG = "PLACES_DELEGATE";

    // higher means closer zoom
    private final static float MAX_ZOOM = 14.5F;
    private static final String TAG = PlacesDelegate.class.getSimpleName();
    // location used as a center fos nearby spots query
    private LatLng lastUserLatLngQueried;
    private List<LatLngBounds> queriedBounds = new ArrayList<>();
    private JsonRequest currentUserLocationRequest;

    private Set<Place> places = new HashSet<>();
    private Set<Marker> markers = new HashSet<>();


    @NonNull
    public static PlacesDelegate newInstance() {
        PlacesDelegate fragment = new PlacesDelegate();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        doDraw();
    }

    @Override
    protected void onMapReady(GoogleMap mMap) {
        super.onMapReady(mMap);
        doDraw();
    }

    @Override
    public void doDraw() {

        if (!isMapReady() || !isResumed()) return;

        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();

        if (getMap().getCameraPosition().zoom < MAX_ZOOM)
            return;

        LatLngBounds viewPortBounds = getViewPortBounds();
        for (Place place : places) {
            if (viewPortBounds.contains(place.getLatLng())) {
                Marker marker = getMap().addMarker(MarkerFactory.getParkingMarker(place, getActivity()));
                markers.add(marker);
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
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


    private void makePlacesViewPortRequest() {

        final LatLngBounds viewPortBounds = getViewPortBounds();

        for (LatLngBounds latLngBounds : queriedBounds) {
            if (latLngBounds.contains(viewPortBounds.northeast) && latLngBounds.contains(viewPortBounds.southwest)) {
                return;
            }
        }

        queriedBounds.add(viewPortBounds);
        float distances[] = new float[3];
        Location.distanceBetween(
                viewPortBounds.getCenter().latitude,
                viewPortBounds.getCenter().longitude,
                viewPortBounds.northeast.latitude,
                viewPortBounds.northeast.longitude,
                distances);

        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        String url = new Uri.Builder().scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("places")
                .appendQueryParameter("lat", Double.toString(viewPortBounds.getCenter().latitude))
                .appendQueryParameter("long", Double.toString(viewPortBounds.getCenter().longitude))
                .appendQueryParameter("radius", String.valueOf((int) (distances[0] / 1.42)))
                .build().toString();

        Log.d(TAG, "Places query: " + url);

        Request request = new JsonObjectRequest(
                url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        PlacesQueryResult parkingQueryResult = parseResult(response);
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

    private void onPlacesUpdate(PlacesQueryResult parkingQueryResult) {
        places.addAll(parkingQueryResult.places);
        doDraw();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition, CameraUpdateRequester requester) {
        if (cameraPosition.zoom > MAX_ZOOM) {
            makePlacesViewPortRequest();
        }

        doDraw();

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
