package com.cahue.iweco.parkedCar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.util.FetchAddressIntentService;

/**
 * Receiver in charge of getting car location updates and performing complementary actions,
 * such as retrieving the car's location address.
 */
public class CarPositionUpdatedReceiver extends BroadcastReceiver {

    private Car car;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (!Geocoder.isPresent()) {
            return;
        }

        car = (Car) intent.getExtras().get(CarDatabase.INTENT_CAR_EXTRA);

        /**
         * Location is set but the address isn't, so let's try to fetch it
         */
        if (car.address == null && car.location != null) {
            Intent fetchAddressIntent = new Intent(context, FetchAddressIntentService.class);
            fetchAddressIntent.putExtra(FetchAddressIntentService.RECEIVER, new AddressResultReceiver());
            fetchAddressIntent.putExtra(FetchAddressIntentService.LOCATION_DATA_EXTRA, car.location);
            context.startService(fetchAddressIntent);
        }

    }

    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver() {
            super(new Handler());
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            if (resultCode != FetchAddressIntentService.SUCCESS_RESULT)
                return;

            // Display the address string
            // or an error message sent from the intent service.
            car.address = resultData.getString(FetchAddressIntentService.RESULT_DATA_KEY);
            CarDatabase.getInstance(context).save(car);

        }
    }

}