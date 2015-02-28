package com.bahpps.cahue.cars;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.R;
import com.bahpps.cahue.auth.Authenticator;
import com.bahpps.cahue.auth.IwecoAccountService;
import com.bahpps.cahue.cars.database.CarDatabase;
import com.bahpps.cahue.login.LoginActivity;
import com.bahpps.cahue.util.Singleton;
import com.bahpps.cahue.util.Requests;

import org.json.JSONObject;

import java.util.Date;

/**
 * Created by Francesco on 04/02/2015.
 */
public class CarsSync {

    private static final String TAG = CarsSync.class.getSimpleName();

    public static void storeCar(CarDatabase carDatabase, Context context, Car car) {
        carDatabase.save(car);
        postCar(car, context);
    }

    /**
     * Remove the stored location of a car
     *
     * @param car
     */
    public static void clearLocation(CarDatabase carDatabase, Context context, Car car) {

        car.time = new Date();
        car.location = null;

        storeCar(carDatabase, context, car);
    }


    public static void remove(final Context context, final Car car, final CarDatabase database) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Singleton.getInstance(context).getRequestQueue();

        Log.i(TAG, "Posting cars");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(Endpoints.BASE_URL)
                .appendPath(Endpoints.CARS_PATH)
                .appendPath(car.id);

        /**
         * Send a Json with the cars contained in this phone
         */
        Request removeRequest = new Requests.DeleteRequest(
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
                        database.save(car);
                        error.printStackTrace();
                    }
                });

        database.delete(car);

        // Add the request to the RequestQueue.
        queue.add(removeRequest);
    }

    /**
     * Post the current state of the car to the server
     *
     * @param context
     */
    public static void postCar(Car car, final Context context) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Singleton.getInstance(context).getRequestQueue();

        Log.i(TAG, "Posting car");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(Endpoints.BASE_URL)
                .appendPath(Endpoints.CARS_PATH);

        /**
         * Send a Json with the cars contained in this phone
         */
        JsonRequest carSyncRequest = new Requests.JsonPostRequest(
                context,
                builder.toString(),
                car.toJSON(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "Post result: " + response.toString());
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

    /**
     * Helper method to trigger an immediate sync ("refresh").
     *
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     *
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh() {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                IwecoAccountService.GetAccount(Authenticator.ACCOUNT_TYPE), // Sync account
                LoginActivity.CONTENT_AUTHORITY,                                          // Content authority
                b);                                                         // Extras
    }

}
