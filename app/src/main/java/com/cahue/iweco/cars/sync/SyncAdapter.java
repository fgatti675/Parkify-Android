package com.cahue.iweco.cars.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.Requests;

import org.json.JSONArray;

import java.util.Set;

/**
 * Sync cars with the server state
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();
    private final CarDatabase database;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        database = CarDatabase.getInstance();
    }

    @Override
    public void onPerformSync(@Nullable Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.i(TAG, "Sync called");

        if (account == null)
            return;

        Log.i(TAG, "Sync in progress");

        // Instantiate the RequestQueue.
        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("cars");

        /**
         * Retrieve an array of cars
         */
        Request carSyncRequest = new Requests.JsonArrayGetRequest(
                getContext(),
                builder.toString(),
                new Response.Listener<JSONArray>() {
                    /**
                     * Here we are receiving cars that were modified by other clients and
                     * their state is outdated here
                     *
                     * @param response
                     */
                    @Override
                    public void onResponse(JSONArray response) {
                        Set<Car> cars = Car.fromJSONArray(response);
                        database.clearSaveAndBroadcast(getContext(), cars);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(@NonNull VolleyError error) {
                        error.printStackTrace();
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(carSyncRequest);

    }


}
