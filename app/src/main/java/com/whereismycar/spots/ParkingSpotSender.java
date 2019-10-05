package com.whereismycar.spots;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.whereismycar.util.FetchAddressDelegate;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.Date;
import java.util.HashMap;

/**
 * Created by francesco on 27.03.2015.
 */
public class ParkingSpotSender {

    private static final String TAG = ParkingSpotSender.class.getSimpleName();

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
