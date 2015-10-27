package com.cahue.iweco.spots.query;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.RequestFuture;
import com.cahue.iweco.IwecoApp;
import com.cahue.iweco.spots.ParkingSpot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Created by francesco on 13.11.2014.
 */
public abstract class ParkingSpotsQuery extends AsyncTask<Void, Void, QueryResult> {


    private static final String TAG = ParkingSpotsQuery.class.getSimpleName();

    protected Context context;
    protected ParkingSpotsUpdateListener listener;


    public ParkingSpotsQuery(Context context, ParkingSpotsUpdateListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected abstract QueryResult doInBackground(Void... voids);

    @Override
    protected void onPostExecute(final QueryResult result) {

        Log.d("ParkingSpotsQuery", result.toString());
        super.onPostExecute(result);
        if(context instanceof Activity){
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onSpotsUpdate(ParkingSpotsQuery.this, result);
                }
            });
        }
    }

    protected JSONObject query(String url) {

        // Instantiate the RequestQueue.
        RequestQueue queue = IwecoApp.getIwecoApp().getRequestQueue();

        RequestFuture<JSONObject> future = RequestFuture.newFuture();

        Log.d(TAG, url);
        JsonRequest stringRequest = new JsonObjectRequest(
                url,
                future,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        listener.onServerError(ParkingSpotsQuery.this, error.networkResponse.statusCode, new String(error.networkResponse.data));
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
        }

        return null;
    }

    protected QueryResult parseResult(JSONObject jsonObject) {

        QueryResult result = new QueryResult();

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

    /**
     * Components that use this service must implement a listener using this interface to get the
     * parking locations
     */
    public interface ParkingSpotsUpdateListener {

        void onSpotsUpdate(ParkingSpotsQuery query, QueryResult result);

        void onServerError(ParkingSpotsQuery query, int statusCode, String reasonPhrase);

    }
}
