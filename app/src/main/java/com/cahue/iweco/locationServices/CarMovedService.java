package com.cahue.iweco.locationServices;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.cahue.iweco.Constants;
import com.cahue.iweco.Endpoints;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.util.Singleton;
import com.cahue.iweco.util.Requests;
import com.google.android.gms.location.DetectedActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

/**
 * This class is in charge of uploading the location of the car to the server when BT connects
 * and the car starts moving.
 *
 * @author Francesco
 */
public class CarMovedService extends LocationPollerService {

    private final static String TAG = CarMovedService.class.getSimpleName();

    @Override
    protected boolean checkPreconditions(Car car) {
        long now = Calendar.getInstance().getTimeInMillis();
        if (car.time == null) return true;
        long parkingTime = car.time.getTime();
        return now - parkingTime > Constants.MINIMUM_STAY_MS;
    }

    @Override
    public void onFirstPreciseFixPolled(Context context, Location spotLocation, Car car) {

        CarDatabase carDatabase = CarDatabase.getInstance(context);

        /**
         * If the accuracy is not good enough, we can check the previous location of the car
         * and if it's close and more accurate, we use it.
         */
        if (spotLocation.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            if (car.location != null && car.location.distanceTo(spotLocation) < Constants.ACCURACY_THRESHOLD_M)
                spotLocation = car.location;
        }

        CarsSync.clearLocation(carDatabase, this, car);

        /**
         * If it's still not accurate, we don't use it
         */
        if (spotLocation.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            doPostSpotLocation(spotLocation, car);
        }

    }

    @Override
    public void onActivitiesDetected(Context context, List<DetectedActivity> detectedActivities, Location lastLocation, Car car) {

    }

    protected void onLocationPost() {

    }

    private void doPostSpotLocation(Location spotLocation, Car car) {
        postSpotLocation(spotLocation, car);
    }


    private void postSpotLocation(final Location location, final Car car) {

        // Instantiate the RequestQueue.
        RequestQueue queue = Singleton.getInstance(this).getRequestQueue();

        Log.i(TAG, "Posting parking spot location");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(getResources().getString(R.string.baseURL))
                .appendPath(getResources().getString(R.string.spotsPath));

        // Send a JSON spot location.
        JSONObject parkingSpotJSON = getParkingSpotJSON(location, car);
        Log.i(TAG, "Posting: " + parkingSpotJSON);
        JsonRequest stringRequest = new Requests.JsonPostRequest(
                this,
                builder.toString(),
                parkingSpotJSON,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        onLocationPost();
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
