package com.bahpps.cahue.locationServices;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.bahpps.cahue.parkedCar.Car;
import com.bahpps.cahue.parkedCar.CarLocationManager;

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
    protected boolean checkPreconditions(String id) {
        return true;
    }

    @Override
    public void onLocationPolled(Context context, Location location, String id) {
        Log.i(TAG, "Received : " + location);
        Car car = new Car();
        car.location = location;
        car.id = id;
        car.time = new Date();
        CarLocationManager.saveCar(context, car);
    }


}
