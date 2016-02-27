package com.cahue.iweco.parkedCar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.FetchAddressIntentService;

/**
 * Receiver in charge of getting car location updates and performing complementary actions,
 * such as retrieving the car's location address.
 */
public class CarPositionUpdatedReceiver extends BroadcastReceiver {

    private static final String TAG = CarPositionUpdatedReceiver.class.getSimpleName();
    @Nullable
    private Car car;
    private Context context;
    private CarDatabase database;

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        this.context = context;

        if (!Geocoder.isPresent()) {
            return;
        }

        database = CarDatabase.getInstance(context);

        String carId = intent.getExtras().getString(Constants.EXTRA_CAR_ID);
        car = database.findCar(carId);

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

    private void fetchAddress(@NonNull Context context, @NonNull Car car) {
        Log.d(TAG, "Fetching address");
        Intent fetchAddressIntent = new Intent(context, FetchAddressIntentService.class);
        fetchAddressIntent.putExtra(FetchAddressIntentService.RECEIVER, new AddressResultReceiver());
        fetchAddressIntent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, car.location);
        context.startService(fetchAddressIntent);
    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int resultCode, @NonNull Bundle resultData) {

            if (resultCode != FetchAddressIntentService.SUCCESS_RESULT)
                return;

            // Display the address string
            // or an error message sent from the intent service.
            car.address = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
            database.updateAddress(car);

            Log.d(TAG, "Sending car update broadcast");

            Intent intent = new Intent(Constants.INTENT_ADDRESS_UPDATE);
            intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
            intent.putExtra(Constants.EXTRA_CAR_ADDRESS, car.address);
            context.sendBroadcast(intent);

        }
    }

}
