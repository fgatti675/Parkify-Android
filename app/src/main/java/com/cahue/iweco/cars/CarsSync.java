package com.cahue.iweco.cars;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.Requests;

import org.json.JSONObject;

/**
 * Created by Francesco on 04/02/2015.
 */
public class CarsSync {

    private static final String TAG = CarsSync.class.getSimpleName();


    /**
     * Post the current state of the car to the server
     *
     */
    public static void postCar(@NonNull Car car) {

        if (car.isOther()) return;

        // Instantiate the RequestQueue.
        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        Log.i(TAG, "Posting car");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("cars");

        /**
         * Send a Json with the cars contained in this phone
         */
        JSONObject jsonRequest = car.toJSON();
        Log.d(TAG, jsonRequest.toString());
        JsonRequest carSyncRequest = new Requests.JsonPostRequest(
                ParkifyApp.getParkifyApp(),
                builder.toString(),
                jsonRequest,
                response -> Log.d(TAG, response.toString()),
                error -> error.printStackTrace());

        // Add the request to the RequestQueue.
        queue.add(carSyncRequest);
    }

}
