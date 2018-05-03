package com.cahue.iweco.spots;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonRequest;
import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.ParkifyApp;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.FetchAddressDelegate;
import com.cahue.iweco.util.Requests;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Random;

/**
 * Created by francesco on 27.03.2015.
 */
public class ParkingSpotSender {

    private static final String TAG = ParkingSpotSender.class.getSimpleName();

    @Deprecated
    public static void doPostSpotLocation(@NonNull Context context, Location spotLocation, boolean future, @NonNull String carId) {
        ParkingSpot spot = new ParkingSpot(new Random().nextLong(), spotLocation, null, new Date(), future);

        // Instantiate the RequestQueue.
        RequestQueue queue = ParkifyApp.getParkifyApp().getRequestQueue();

        Log.i(TAG, "Posting parking spot ");

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")
                .authority(BuildConfig.BACKEND_URL)
                .appendPath("spots");

        // Send a JSON spot location.
        JSONObject parkingSpotJSON = spot.toJSON(carId);
        Log.i(TAG, "Posting: " + parkingSpotJSON);
        String url = builder.toString();
        Log.d(TAG, url);
        JsonRequest stringRequest = new Requests.JsonPostRequest(
                context,
                url,
                parkingSpotJSON,
                response -> Log.i(TAG, "Post result: " + response.toString()),
                error -> error.printStackTrace());

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public static void fetchAddressAndSave(Context context, Location spotLocation, Date time, String carId, boolean future) {
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putString("car", carId);
        bundle.putBoolean("future", future);
        firebaseAnalytics.logEvent("bt_freed_spot", bundle);

        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(context, spotLocation, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                saveFreeSpot(address, spotLocation, carId, time, future);
            }

            @Override
            public void onError(String error) {
                saveFreeSpot(null, spotLocation, carId, time, future);
            }
        });
    }

    private static void saveFreeSpot(String address, Location spotLocation, String carId, Date time, boolean future) {
        HashMap<String, Object> parkingMap = new HashMap<>();
        parkingMap.put("location", new GeoPoint(spotLocation.getLatitude(), spotLocation.getLongitude()));
        parkingMap.put("accuracy", spotLocation.getAccuracy());
        parkingMap.put("time", time);
        parkingMap.put("address", address);
        parkingMap.put("car_id", carId);
        parkingMap.put("future", future);
        parkingMap.put("user_id", FirebaseAuth.getInstance().getUid());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("freed_spots").add(parkingMap);
    }

}
