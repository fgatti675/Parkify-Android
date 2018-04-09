package com.cahue.iweco.locationservices;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.spots.ParkingSpotSender;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;

/**
 * This class is in charge of uploading the location of the car to the server when BT connects
 * and the car starts moving.
 *
 * @author Francesco
 */
public class CarMovedReceiver extends AbstractLocationUpdatesBroadcastReceiver {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".CAR_MOVED_ACTION";

    private final static String TAG = CarMovedReceiver.class.getSimpleName();

    private GoogleApiClient mGeofenceApiClient;

    private void clearGeofence(Context context, @NonNull final Car car) {
        mGeofenceApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {

                        LocationServices.GeofencingApi.removeGeofences(
                                mGeofenceApiClient,
                                Arrays.asList(car.id)
                        );
                        Log.d(TAG, "Geofence removed");
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "Geofence, connection suspended");
                        mGeofenceApiClient.disconnect();
                    }
                })
                .build();

        mGeofenceApiClient.connect();
    }

    @Override
    protected void onPreciseFixPolled(Context context, final Location spotLocation, Bundle extras) {

        CarDatabase carDatabase = CarDatabase.getInstance();
        String carId = extras.getString(Constants.EXTRA_CAR_ID, null);

        DocumentReference documentReference = FirebaseFirestore.getInstance().collection("cars").document(carId);
        documentReference.get().addOnSuccessListener(documentSnapshot -> {

            Car car = Car.fromFirestore(documentSnapshot);

            /**
             * If the accuracy is not good enough, we can check the previous location of the car
             * and if it's close and more accurate, we use it.
             */
            Location location = spotLocation;
            if (location.getAccuracy() > Constants.ACCURACY_THRESHOLD_M) {
                if (car.location != null && car.location.distanceTo(location) < Constants.ACCURACY_THRESHOLD_M)
                    location = car.location;
            }

            /**
             * If it's accurate enough we notify
             */
            if (location.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
                ParkingSpotSender.doPostSpotLocation(context, location, false, car);
                Util.showBlueToast(context, R.string.thanks_free_spot, Toast.LENGTH_SHORT);
            }

            carDatabase.removeCarLocation(car.id);
            clearGeofence(context, car);

            Tracking.sendEvent(Tracking.CATEGORY_PARKING, Tracking.ACTION_BLUETOOTH_FREED_SPOT);

            FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
            Bundle bundle = new Bundle();
            bundle.putString("car", carId);
            firebaseAnalytics.logEvent("bt_freed_spot", bundle);
        });

    }
}
