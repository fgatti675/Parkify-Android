package com.cahue.iweco.activityRecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;

import java.util.Date;

/**
 * Broadcast receiver in charge of saving a car, after if is requested by a action taken from a
 * notification
 */
public class SaveCarRequestReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        CarDatabase database = CarDatabase.getInstance(context);

        String carId = intent.getExtras().getString(Constants.INTENT_CAR_EXTRA_ID);
        Car car = database.findCar(carId);

        // this should happen only if the user was logged out at that particular moment...
        if (car == null){
            if(BuildConfig.DEBUG)
                Toast.makeText(context, "Trying to save null car", Toast.LENGTH_LONG);
            return;
        }

        car.spotId = null;
        car.location = intent.getParcelableExtra(Constants.INTENT_CAR_EXTRA_LOCATION);
        car.address = intent.getStringExtra(Constants.INTENT_CAR_EXTRA_ADDRESS);
        car.time = new Date(intent.getLongExtra(Constants.INTENT_CAR_EXTRA_TIME, 0));

        CarsSync.storeCar(database, context, car);

        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);
        mNotifyMgr.cancel(ParkedCarRequestedService.NOTIFICATION_ID);
    }
}
