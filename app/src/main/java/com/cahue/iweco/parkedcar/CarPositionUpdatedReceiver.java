package com.cahue.iweco.parkedcar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.FetchAddressDelegate;

/**
 * Receiver in charge of getting car location updates and performing complementary actions,
 * such as retrieving the car's location address.
 */
public class CarPositionUpdatedReceiver extends BroadcastReceiver {

    private static final String TAG = CarPositionUpdatedReceiver.class.getSimpleName();
    @Nullable
    private Car car;
    private CarDatabase database;

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        if (!Geocoder.isPresent()) {
            return;
        }

        database = CarDatabase.getInstance();

        String carId = intent.getExtras().getString(Constants.EXTRA_CAR_ID);
        car = database.findCar(context, carId);

        // this should happen only if the user was logged out at that particular moment...
        if (car == null)
            return;

        /**
         * Location is set but the address isn't, so let's try to fetch it
         */
        if (car.address == null && car.location != null) {
            fetchAddress(context, car);
        }

    }

    private void fetchAddress(@NonNull final Context context, @NonNull final Car car) {

        /**
         * Fetch address
         */
        Log.d(TAG, "Fetching address");
        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(context, car.location, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                // Display the address string
                // or an error message sent from the intent service.
                car.address = address;
                database.updateAddress(context, car);

                Log.d(TAG, "Sending car update broadcast");

                Intent intent = new Intent(Constants.INTENT_ADDRESS_UPDATE);
                intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
                intent.putExtra(Constants.EXTRA_CAR_ADDRESS, car.address);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }

            @Override
            public void onError(String error) {
            }
        });
    }

}
