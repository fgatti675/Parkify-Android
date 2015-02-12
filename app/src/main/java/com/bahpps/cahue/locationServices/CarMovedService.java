package com.bahpps.cahue.locationServices;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.bahpps.cahue.Endpoints;
import com.bahpps.cahue.cars.Car;
import com.bahpps.cahue.cars.CarDatabase;
import com.bahpps.cahue.cars.CarsSync;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;

/**
 * This class is in charge of uploading the location of the car to the server when BT connects
 * and the car starts moving.
 *
 * @author Francesco
 */
public class CarMovedService extends LocationPollerService {

    private final static String TAG = "CarMovedPositionReceiver";

    /**
     * Minimum desired accuracy
     */
    private final static int ACCURACY_THRESHOLD_M = 25;

    /**
     * Only post locations if the car has stopped for at least a few minutes
     */
    private static final long MINIMUM_STAY_MS = 180000;

    /**
     * Times we will be retrying to post a spot location
     */
    private static final int POST_RETRIES = 3;

    @Override
    protected boolean checkPreconditions(Car car) {
        long now = Calendar.getInstance().getTimeInMillis();
        if (car.time == null) return true;
        long parkingTime = car.time.getTime();
        return now - parkingTime > MINIMUM_STAY_MS;
    }

    @Override
    public void onLocationPolled(Context context, Location spotLocation, Car car) {

        CarDatabase carDatabase = CarDatabase.getInstance(context);

        /**
         * If the accuracy is not good enough, we can check the previous location of the car
         * and if it's close and more accurate, we use it.
         */
        if (spotLocation.getAccuracy() < ACCURACY_THRESHOLD_M) {
            if (car.location.distanceTo(spotLocation) < ACCURACY_THRESHOLD_M)
                spotLocation = car.location;
        }

        CarsSync.clearLocation(carDatabase, car);

        /**
         * If it's still not accurate, we don't use it
         */
        if (spotLocation.getAccuracy() < ACCURACY_THRESHOLD_M) {
            doPostSpotLocation(spotLocation, car);
        }

    }

    protected void onLocationPost() {

    }

    private void doPostSpotLocation(Location spotLocation, Car car) {
        postSpotLocation(spotLocation, car, POST_RETRIES);
    }

    private void postSpotLocation2(final Location location, final Car car, final int retries) {

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void[] objects) {
                try {

                    Log.i(TAG, "Posting parking spot location");

                    Uri.Builder builder = new Uri.Builder();
                    builder.scheme("https")
                            .authority(Endpoints.BASE_URL)
                            .appendPath(Endpoints.SPOTS_PATH);

                    HttpClient httpclient = new DefaultHttpClient();
                    HttpPost httpPost = CommUtil.createHttpPost(CarMovedService.this, builder.build().toString());

                    String json = getParkingSpotJSON(location, car).toString();

                    Log.i(TAG, "Posting\n" + json);
                    httpPost.setEntity(new StringEntity(json));

                    Log.d(TAG, httpPost.toString());

                    HttpResponse response = httpclient.execute(httpPost);
                    StatusLine statusLine = response.getStatusLine();

                    Log.i(TAG, "Post result: " + statusLine.getStatusCode());
                    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {

                        Log.i(TAG, "Post result: " + EntityUtils.toString(response.getEntity()));
                    } else {
                        //Closes the connection.
                        if (response != null && response.getEntity() != null) {
                            response.getEntity().getContent().close();
                            Log.e(TAG, statusLine.getReasonPhrase());
                        }
                        if (retries > 0)
                            postSpotLocation(location, car, retries - 1);
                    }
                    return null;

                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                onLocationPost();
            }
        }.execute();
    }


    private void postSpotLocation(final Location location, final Car car, final int retries) {

        // Instantiate the RequestQueue.
        RequestQueue queue = CommUtil.getInstance(this).getRequestQueue();

        Log.i(TAG, "Posting parking spot location");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(Endpoints.BASE_URL)
                .appendPath(Endpoints.SPOTS_PATH);

        // Send a JSON spot location.
        JsonRequest stringRequest = new Requests.JsonPostRequest(
                this,
                builder.toString(),
                getParkingSpotJSON(location, car),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(TAG, "Post result: " + response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Network error : " + error.networkResponse.statusCode);
                        if (retries > 0)
                            postSpotLocation(location, car, retries - 1);
                    }
                });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private static JSONObject getParkingSpotJSON(Location location, Car car) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("car", car.toJSON());
            obj.put("latitude", location.getLatitude());
            obj.put("longitude", location.getLongitude());
            obj.put("accuracy", location.getAccuracy());
            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

}
