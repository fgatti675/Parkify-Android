package com.whereismycar.util;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by francesco on 06.03.2015.
 */
public class FetchAddressDelegate {

    private static final String TAG = FetchAddressDelegate.class.getSimpleName();
    protected Callbacks callbacks;

    public void fetch(Context context, Location location, Callbacks callbacks) {

        Geocoder geocoder = new Geocoder(context, Locale.getDefault());

        String errorMessage = "";

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            Log.e(TAG, "Service not available", ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e(TAG, "Invalid lat long" + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                Log.e(TAG, "No address found");
            }
            callbacks.onError(errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, "Address found");
            callbacks.onAddressFetched(TextUtils.join(", ", addressFragments));
        }
    }

    public interface Callbacks {
        void onAddressFetched(String address);

        void onError(String error);
    }
}
