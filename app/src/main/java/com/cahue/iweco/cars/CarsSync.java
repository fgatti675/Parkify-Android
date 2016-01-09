package com.cahue.iweco.cars;

import android.accounts.Account;
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
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.IwecoApp;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.login.AuthUtils;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.Requests;
import com.cahue.iweco.util.Util;

import org.json.JSONObject;

/**
 * Created by Francesco on 04/02/2015.
 */
public class CarsSync {

    private static final String TAG = CarsSync.class.getSimpleName();

    public static void updateCarFromPossibleSpot(CarDatabase carDatabase, Context context, Car car, ParkingSpot spot) {

        Log.i(TAG, "Updating car " + car + " " + spot);

        car.location = spot.location;
        car.address = spot.address;
        car.spotId = null;
        car.time = spot.time;

        if (BuildConfig.DEBUG)
            Toast.makeText(context, "Storing car", Toast.LENGTH_LONG);

        carDatabase.updateCarRemoveSpotAndBroadcast(car, spot);

        if (!AuthUtils.isSkippedLogin(context))
            postCar(car, context, carDatabase);
    }

    public static void storeCar(CarDatabase carDatabase, Context context, Car car) {
        Log.i(TAG, "Storing car " + car);

        if (BuildConfig.DEBUG)
            Toast.makeText(context, "Storing car", Toast.LENGTH_LONG);

        carDatabase.saveCarAndBroadcast(car);

        if (!AuthUtils.isSkippedLogin(context))
            postCar(car, context, carDatabase);
    }

    /**
     * Remove the stored location of a car
     *
     * @param car
     */
    public static void clearLocation(CarDatabase carDatabase, Context context, Car car) {

        car.spotId = null;
        car.time = null;
        car.location = null;
        car.address = null;

        storeCar(carDatabase, context, car);
    }


    public static void remove(final Context context, final Car car, final CarDatabase database) {
        // Instantiate the RequestQueue.
        RequestQueue queue = IwecoApp.getIwecoApp().getRequestQueue();

        Log.i(TAG, "Removing car " + car);

        if (!AuthUtils.isSkippedLogin(context)) {

            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https")
                    .authority(context.getResources().getString(R.string.baseURL))
                    .appendPath(context.getResources().getString(R.string.carsPath))
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
                            Util.createUpperToast(context, R.string.delete_error, Toast.LENGTH_SHORT);
                            database.saveCarAndBroadcast(car);
                            error.printStackTrace();
                        }
                    });

            // Add the request to the RequestQueue.
            queue.add(removeRequest);
        }

        database.deleteCar(car);

    }

    /**
     * Post the current state of the car to the server
     *
     * @param context
     */
    public static void postCar(Car car, final Context context, final CarDatabase carDatabase) {

        if (car.isOther()) return;

        // Instantiate the RequestQueue.
        RequestQueue queue = IwecoApp.getIwecoApp().getRequestQueue();

        Log.i(TAG, "Posting car");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(context.getResources().getString(R.string.baseURL))
                .appendPath(context.getResources().getString(R.string.carsPath));

        /**
         * Send a Json with the cars contained in this phone
         */
        JSONObject jsonRequest = car.toJSON();
        Log.d(TAG, jsonRequest.toString());
        JsonRequest carSyncRequest = new Requests.JsonPostRequest(
                context,
                builder.toString(),
                jsonRequest,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, response.toString());
                        Car car = Car.fromJSON(response);
                        carDatabase.updateSpotId(car);
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
     * <p/>
     * <p>This should only be used when we need to preempt the normal sync schedule. Typically, this
     * means the user has pressed the "refresh" button.
     * <p/>
     * Note that SYNC_EXTRAS_MANUAL will cause an immediate sync, without any optimization to
     * preserve battery life. If you know new data is available (perhaps via a GCM notification),
     * but the user is not actively waiting for that data, you should omit this flag; this will give
     * the OS additional freedom in scheduling your sync request.
     */
    public static void TriggerRefresh(Context context, Account account) {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                account,                                        // Sync account
                context.getString(R.string.content_authority),                              // Content authority
                b);                                             // Extras
    }

}
