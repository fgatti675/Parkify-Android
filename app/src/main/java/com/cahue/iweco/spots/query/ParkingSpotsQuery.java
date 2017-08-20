package com.cahue.iweco.spots.query;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.model.ParkingSpot;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by francesco on 13.11.2014.
 */
public class ParkingSpotsQuery {

    private static final String TAG = ParkingSpotsQuery.class.getSimpleName();

    private final LatLngBounds latLngBounds;
    private final ParkingSpotsUpdateListener listener;

    public ParkingSpotsQuery(LatLngBounds latLngBounds, ParkingSpotsUpdateListener listener) {
        this.listener = listener;
        this.latLngBounds = latLngBounds;
    }

    public void execute() {

        // Instantiate the RequestQueue.
        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        String url = getRequestUri().toString();

        Log.d(TAG, "Parking spots query: " + url);

        JsonRequest request = new JsonObjectRequest(
                url,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ParkingQueryResult parkingQueryResult = parseResult(response);
                        listener.onSpotsUpdate(ParkingSpotsQuery.this, parkingQueryResult);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        error.printStackTrace();
                        if (error.networkResponse == null)
                            listener.onServerError(ParkingSpotsQuery.this, -1, null);
                        else
                            listener.onServerError(ParkingSpotsQuery.this, error.networkResponse.statusCode, new String(error.networkResponse.data));
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(request);
    }

    @NonNull
    protected ParkingQueryResult parseResult(@Nullable JSONObject jsonObject) {

        ParkingQueryResult result = new ParkingQueryResult();

        Set<ParkingSpot> spots = new HashSet<>();

        result.spots = spots;

        if (jsonObject != null) {
            try {

                JSONArray spotsArray = jsonObject.getJSONArray("spots");
                for (int i = 0; i < spotsArray.length(); i++) {
                    JSONObject entry = spotsArray.getJSONObject(i);
                    ParkingSpot spot = ParkingSpot.fromJSON(entry);
                    spots.add(spot);
                }

                result.moreResults = jsonObject.getBoolean("moreResults");
                result.error = jsonObject.getBoolean("error");

            } catch (JSONException e) {
                e.printStackTrace();
                throw new RuntimeException("Error parsing spots jsonObject", e);
            }
        }

        return result;
    }

    protected Uri getRequestUri() {
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("spots")
                .appendQueryParameter("swLat", Double.toString(latLngBounds.southwest.latitude))
                .appendQueryParameter("swLong", Double.toString(latLngBounds.southwest.longitude))
                .appendQueryParameter("neLat", Double.toString(latLngBounds.northeast.latitude))
                .appendQueryParameter("neLong", Double.toString(latLngBounds.northeast.longitude));
        return builder.build();
    }

    /**
     * Components that use this service must implement a listener using this interface to get the
     * parking locations
     */
    public interface ParkingSpotsUpdateListener {

        void onSpotsUpdate(ParkingSpotsQuery query, ParkingQueryResult result);

        void onServerError(ParkingSpotsQuery query, int statusCode, String reasonPhrase);

    }
}
