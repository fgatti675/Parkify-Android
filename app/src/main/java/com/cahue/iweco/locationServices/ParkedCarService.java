package com.cahue.iweco.locationServices;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarsSync;

import java.util.Date;

/**
 * This class is in charge of receiving location updates, after and store it as the cars position.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarService extends LocationPollerService {

    private final static String TAG = "ParkedCarService";

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onLocationPolled(Context context, Location location, Car car) {

        Log.i(TAG, "Received : " + location);

        car.location = location;
        car.address = null;
        car.time = new Date();

        CarDatabase carDatabase = CarDatabase.getInstance(context);
        CarsSync.storeCar(carDatabase, context, car);

    }


}
