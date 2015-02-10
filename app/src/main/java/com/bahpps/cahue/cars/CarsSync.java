package com.bahpps.cahue.cars;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;
import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.util.CommUtil;
import com.bahpps.cahue.util.Requests;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by Francesco on 04/02/2015.
 */
public class CarsSync {

    private static final String TAG = CarsSync.class.getSimpleName();

    public static final int RETRIES = 3;
    public static final String NEEDS_SYNC_PREF = "NEEDS_SYNC";

    public static final String INTENT = "CAR_MOVED_INTENT";
    public static final String INTENT_POSITION = "CAR_MOVED_INTENT_POSITION";

    public static void storeCar(CarDatabase carDatabase, final Context context, final Car car) {

        setNeedsSyncPref(context, true);
        carDatabase.saveCar(car);
        postCars(context, carDatabase.retrieveCars(false), RETRIES);

        /**
         * Tell everyone else
         */
        Intent intent = new Intent(INTENT);
        intent.putExtra(INTENT_POSITION, car);
        context.sendBroadcast(intent);

    }

//    public static void saveCars(CarDatabase carDatabase, final Context context, final List<Car> cars) {
//        postCars(context, cars, RETRIES);
//        setNeedsSyncPref(context, true);
//        carDatabase.saveCars(cars);
//    }

    private static boolean isSyncNeeded(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(NEEDS_SYNC_PREF, false);
    }

    private static void setNeedsSyncPref(Context context, boolean isNeeded) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean(NEEDS_SYNC_PREF, isNeeded).apply();
    }

    public static void postCars2(final Context context, final List<Car> cars, final int retries) {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void[] objects) {
                try {

                    Log.i(TAG, "Posting users location");

                    Uri.Builder builder = new Uri.Builder();
                    builder.scheme("https")
                            .authority(Endpoints.BASE_URL)
                            .appendPath(Endpoints.CARS_PATH);

                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httpPost = CommUtil.createHttpPost(context, builder.build().toString());

                    String json = getCarsJSON(cars).toString();
                    Log.i(TAG, "Posting\n" + json);
                    httpPost.setEntity(new StringEntity(json));

                    Log.d(TAG, httpPost.toString());

                    HttpResponse response = httpclient.execute(httpPost);
                    StatusLine statusLine = response.getStatusLine();

                    Log.i(TAG, "Post result: " + statusLine.getStatusCode());

                    /**
                     * Everything cool
                     */
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                        setNeedsSyncPref(context, false);
                        Log.i(TAG, "Post result: " + EntityUtils.toString(response.getEntity()));
                    }
                    /**
                     * Not so cool
                     */
                    else {
                        //Closes the connection.
                        if (response != null && response.getEntity() != null) {
                            response.getEntity().getContent().close();
                            Log.e(TAG, statusLine.getReasonPhrase());
                        }
                        if (retries > 0)
                            postCars2(context, cars, retries - 1);
                    }

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
    }

    public static void postCars(final Context context, final List<Car> cars, final int retries) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(context);

        Log.i(TAG, "Posting sars");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(Endpoints.BASE_URL)
                .appendPath(Endpoints.CARS_PATH);

        // Send a JSON spot location.
        JsonRequest stringRequest = new Requests.JsonArrayPostRequest(
                context,
                builder.toString(),
                getCarsJSON(cars),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "Post result: " + response.toString());
                        setNeedsSyncPref(context, false);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        if (retries > 0)
                            postCars(context, cars, retries - 1);
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private static JSONArray getCarsJSON(List<Car> cars) {
        JSONArray carsArray = new JSONArray();
        for (Car car : cars)
            carsArray.put(car.toJSON());

        Log.d(TAG, carsArray.toString());
        return carsArray;
    }
}
