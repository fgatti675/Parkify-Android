package com.bahpps.cahue.cars;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.util.Requests;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

/**
 * Created by Francesco on 04/02/2015.
 */
public class CarsSync {

    private static final String TAG = CarsSync.class.getSimpleName();

    public static final int RETRIES = 3;
    public static final String NEEDS_SYNC_PREF = "NEEDS_SYNC";

    public static final String INTENT_CAR_UPDATE = "CAR_UPDATED_INTENT";
    public static final String INTENT_CAR_EXTRA = "CAR_EXTRA";

    public static void storeCar(CarDatabase carDatabase, final Context context, final Car car) {

        setNeedsSyncPref(context, true);
        carDatabase.saveCar(car);
        postCars(context, carDatabase);

        /**
         * Tell everyone else
         */
        Intent intent = new Intent(INTENT_CAR_UPDATE);
        intent.putExtra(INTENT_CAR_EXTRA, car);
        context.sendBroadcast(intent);

    }

    public static void deleteCar(CarDatabase carDatabase, final Context context, final Car car) {

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
     * Post the current state of the cars database to the server
     *
     * @param context
     * @param carDatabase
     */
    public static void postCars(final Context context, final CarDatabase carDatabase) {

        final List<Car> cars = carDatabase.retrieveCars(false);

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);

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
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                Car car = Car.fromJSON(response.getJSONObject(i));
                                carDatabase.saveCar(car);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        setNeedsSyncPref(context, false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                    }
                });

        carSyncRequest.setRetryPolicy(new DefaultRetryPolicy(
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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
