package com.cahue.iweco.parkedCar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.util.FetchAddressIntentService;

/**
 * Receiver in charge of getting car location updates and performing complementary actions,
 * such as retrieving the car's location address.
 */
public class CarPositionUpdatedReceiver extends BroadcastReceiver {

    private static final String TAG = CarPositionUpdatedReceiver.class.getSimpleName();
    private Car car;
    private Context context;
    private CarDatabase database;


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (!Geocoder.isPresent()) {
            return;
        }

        database = CarDatabase.getInstance(context);

        String carId = intent.getExtras().getString(Constants.INTENT_CAR_EXTRA_ID);
        car = database.find(carId);

        /**
         * Location is set but the address isn't, so let's try to fetch it
         */
        if (car.address == null && car.location != null) {
            fetchAddress(context, car);
        }

    }

    private void fetchAddress(Context context, Car car) {
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
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode != FetchAddressIntentService.SUCCESS_RESULT)
                return;

            // check the address hasn't changed
//            Car currentCar = database.find(car.id);
//            if (currentCar == null) return;
//
//            if (currentCar.location.getLatitude() != car.location.getLatitude()
//                    || currentCar.location.getLongitude() != car.location.getLongitude()) {
//                fetchAddress(context, currentCar);
//                return;
//            }

            // Display the address string
            // or an error message sent from the intent service.
            car.address = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
            database.updateAddress(car);

            Log.d(TAG, "Sending car update broadcast");

            Intent intent = new Intent(Constants.INTENT_ADDRESS_UPDATE);
            intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, car.id);
            intent.putExtra(Constants.INTENT_EXTRA_ADDRESS, car.address);
            context.sendBroadcast(intent);

        }
    }

}
