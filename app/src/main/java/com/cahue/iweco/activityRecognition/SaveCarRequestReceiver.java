package com.cahue.iweco.activityrecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.widget.Toast;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;

/**
 * Broadcast receiver in charge of saving a car, after if is requested by a action taken from a
 * notification
 */
public class SaveCarRequestReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        CarDatabase database = CarDatabase.getInstance(context);

        String carId = intent.getExtras().getString(Constants.EXTRA_CAR_ID);
        Car car = database.findCar(carId);

        // this should happen only if the user was logged out at that particular moment...
        if (car == null){
            if(BuildConfig.DEBUG)
                Toast.makeText(context, "Trying to save null car", Toast.LENGTH_LONG);
            return;
        }

        ParkingSpot possibleSpot = intent.getExtras().getParcelable(Constants.EXTRA_SPOT);

        CarsSync.updateCarFromPossibleSpot(database, context, car, possibleSpot);

        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);
        mNotifyMgr.cancel(PossibleParkedCarService.NOTIFICATION_ID);
    }
}
