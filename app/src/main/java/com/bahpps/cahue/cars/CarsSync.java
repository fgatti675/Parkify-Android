package com.bahpps.cahue.cars;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.R;
import com.bahpps.cahue.cars.database.CarDatabase;
import com.bahpps.cahue.util.CommUtil;
import com.bahpps.cahue.util.Requests;

import org.json.JSONArray;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by Francesco on 04/02/2015.
 */
public class CarsSync {

    private static final String TAG = CarsSync.class.getSimpleName();

    public static final String NEEDS_SYNC_PREF = "NEEDS_SYNC";

    public static final String INTENT_CAR_UPDATE = "CAR_UPDATED_INTENT";
    public static final String INTENT_CAR_EXTRA = "CAR_EXTRA";

    public static void storeCar(CarDatabase carDatabase, final Context context, final Car car) {

        saveAndBroadcast(carDatabase, context, car);
        postCars(context, carDatabase);

    }

    private static void saveAndBroadcast(CarDatabase carDatabase, Context context, Car car) {
        carDatabase.saveCar(car);

        /**
         * Tell everyone else
         */
        Intent intent = new Intent(INTENT_CAR_UPDATE);
        intent.putExtra(INTENT_CAR_EXTRA, car);
        context.sendBroadcast(intent);
    }

    /**
     * Remove the stored location of a car
     *
     * @param car
     */
    public static void clearLocation(CarDatabase carDatabase, Car car) {

        car.time = new Date();
        car.location = null;

        carDatabase.saveCar(car);
    }

    private static boolean isSyncNeeded(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(NEEDS_SYNC_PREF, false);
    }

    private static void setNeedsSyncPref(Context context, boolean isNeeded) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(NEEDS_SYNC_PREF, isNeeded).apply();
    }

    /**
     * Retrieve the state of the cars from the server
     * @param context
     */
    public static void update(Context context){

    }

    public static void remove(final Context context, final Car car, final CarDatabase database) {
        // Instantiate the RequestQueue.
        RequestQueue queue = CommUtil.getInstance(context).getRequestQueue();

        Log.i(TAG, "Posting sars");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(Endpoints.BASE_URL)
                .appendPath(Endpoints.CARS_PATH)
                .appendPath(car.id);

        /**
         * Send a Json with the cars contained in this phone
         */
        Request carSyncRequest = new Requests.DeleteRequest(
                context,
                builder.toString(),
                new Response.Listener<String>() {
                    /**
                     * Here we are receiving cars that were modified by other clients and
                     * their state is outdated here
                     *
                     * @param response
                     */
                    @Override
                    public void onResponse(String response) {
                        Log.i(TAG, "Car deleted : " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context, R.string.delete_error, Toast.LENGTH_SHORT).show();
                        saveAndBroadcast(database, context, car);
                        error.printStackTrace();
                    }
                });

        database.delete(car);

        // Add the request to the RequestQueue.
        queue.add(carSyncRequest);
    }

    /**
     * Post the current state of the cars database to the server
     *
     * @param context
     * @param carDatabase
     */
    public static void postCars(final Context context, final CarDatabase carDatabase) {

        final List<Car> cars = carDatabase.retrieveCars(false);

        // Instantiate the RequestQueue.
        RequestQueue queue = CommUtil.getInstance(context).getRequestQueue();

        Log.i(TAG, "Posting sars");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(Endpoints.BASE_URL)
                .appendPath(Endpoints.CARS_PATH);

        /**
         * Send a Json with the cars contained in this phone
         */
        JsonRequest carSyncRequest = new Requests.JsonArrayPostRequest(
                context,
                builder.toString(),
                getCarsJSON(cars),
                new Response.Listener<JSONArray>() {
                    /**
                     * Here we are receiving cars that were modified by other clients and
                     * their state is outdated here
                     *
                     * @param response
                     */
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.i(TAG, "Post result: " + response.toString());
                        Set<Car> cars = Car.fromJSONArray(response);
                        carDatabase.saveCars(cars);
                        setNeedsSyncPref(context, false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });


        // Add the request to the RequestQueue.
        queue.add(carSyncRequest);
    }

    private static JSONArray getCarsJSON(List<Car> cars) {
        JSONArray carsArray = new JSONArray();
        for (Car car : cars)
            carsArray.put(car.toJSON());

        Log.d(TAG, carsArray.toString());
        return carsArray;
    }
}
