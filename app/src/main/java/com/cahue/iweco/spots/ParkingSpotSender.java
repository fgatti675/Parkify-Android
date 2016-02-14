package com.cahue.iweco.spots;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.R;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.Requests;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by francesco on 27.03.2015.
 */
public class ParkingSpotSender {

    private static final String TAG = ParkingSpotSender.class.getSimpleName();

    public static void doPostSpotLocation(@NonNull Context context, Location spotLocation, boolean future, @NonNull Car car) {
        ParkingSpot spot = new ParkingSpot(car.spotId, spotLocation, null, new Date(), future);
        postSpot(context, spot, car);
    }


    private static void postSpot(@NonNull Context context, @NonNull ParkingSpot spot, @NonNull final Car car) {

        // Instantiate the RequestQueue.
        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        Log.i(TAG, "Posting parking spot ");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(context.getResources().getString(R.string.baseURL))
                .appendPath(context.getResources().getString(R.string.spotsPath));


        // Send a JSON spot location.
        JSONObject parkingSpotJSON = spot.toJSON(car);
        Log.i(TAG, "Posting: " + parkingSpotJSON);
        String url = builder.toString();
        Log.d(TAG, url);
        JsonRequest stringRequest = new Requests.JsonPostRequest(
                context,
                url,
                parkingSpotJSON,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(@NonNull JSONObject response) {
                        Log.i(TAG, "Post result: " + response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        error.printStackTrace();
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}
