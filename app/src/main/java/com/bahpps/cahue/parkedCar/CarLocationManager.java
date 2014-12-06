package com.bahpps.cahue.parkedCar;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import com.bahpps.cahue.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by francesco on 16.09.2014.
 */
public class CarLocationManager {

    public final static String INTENT = "CAR_MOVED_INTENT";
    public final static String INTENT_POSITION = "CAR_MOVED_INTENT_POSITION";

    public static final String PREF_CAR_LATITUDE = "PREF_CAR_LATITUDE";
    public static final String PREF_CAR_LONGITUDE = "PREF_CAR_LONGITUDE";
    public static final String PREF_CAR_ACCURACY = "PREF_CAR_ACCURACY";
    public static final String PREF_CAR_PROVIDER = "PREF_CAR_PROVIDER";
    public static final String PREF_CAR_TIME = "PREF_CAR_TIME";

    private final static String TAG = "CarLocationManager";

    /**
     * Persist the location of the car in the shared preferences
     *
     * @param car
     */
    public static void saveCar(Context context, Car car) {
        SharedPreferences prefs = Util.getSharedPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();

        String id = car.id;
        Location loc = car.location;

        car.time = new Date();

        // We store the result
        editor.putInt(PREF_CAR_LATITUDE + id, (int) (loc.getLatitude() * 1E6));
        editor.putInt(PREF_CAR_LONGITUDE + id, (int) (loc.getLongitude() * 1E6));
        editor.putInt(PREF_CAR_ACCURACY + id, (int) (loc.getAccuracy() * 1E6));
        editor.putString(PREF_CAR_PROVIDER + id, loc.getProvider());
        editor.putLong(PREF_CAR_TIME + id, car.time.getTime());

        editor.apply();

        Log.i(TAG, "Stored new location: " + loc);

        Intent intent = new Intent(INTENT);
        intent.putExtra(INTENT_POSITION, car);
        context.sendBroadcast(intent);
    }

    /**
     * Get a Collection of available cars, can have the location set to null
     *
     * @param context
     * @return
     */
    public static List<Car> getAvailableCars(Context context) {

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // bonded BT devices to the phone
        Set<BluetoothDevice> bondedBTDevices = mBtAdapter.getBondedDevices();

        Set<String> pairedDevices = Util.getPairedDevices(context);
        List<Car> cars = new ArrayList<Car>();
        for (String id : pairedDevices) {
            Car storedCar = getStoredBTCar(context, id, bondedBTDevices);
            if (storedCar != null)
                cars.add(storedCar);
        }

        // add a default car, mainly if the user wants to store the location of a non paired device
        Car defaultCar = getStoredBTCar(context, "", bondedBTDevices);
        cars.add(defaultCar);

        return cars;
    }


    public static Car getStoredCar(Context context, String id) {

        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // bonded BT devices to the phone
        Set<BluetoothDevice> bondedBTDevices = mBtAdapter.getBondedDevices();

        return getStoredBTCar(context, id, bondedBTDevices);
    }


    private static Car getStoredBTCar(Context context, String id, Set<BluetoothDevice> bondedBTDevices) {

        SharedPreferences prefs = Util.getSharedPreferences(context);

        Car car = new Car();
        car.id = id;

        // name
        for (BluetoothDevice device : bondedBTDevices) {
            if (id.equals(device.getAddress())) {
                car.name = device.getName();
            }
        }

        /**
         * If location is set
         */
        if (prefs.contains(PREF_CAR_LATITUDE + id) && prefs.contains(PREF_CAR_LONGITUDE + id)) {

            // Details of the last location fix
            int lastLatitude = prefs.getInt(PREF_CAR_LATITUDE + id, 0);
            int lastLongitude = prefs.getInt(PREF_CAR_LONGITUDE + id, 0);
            int lastAccuracy = prefs.getInt(PREF_CAR_ACCURACY + id, 0);

            Location lastLocation = new Location(prefs.getString(PREF_CAR_PROVIDER + id, ""));
            lastLocation.setLatitude(lastLatitude / 1E6);
            lastLocation.setLongitude(lastLongitude / 1E6);
            lastLocation.setAccuracy((float) (lastAccuracy / 1E6));

            Date date = new Date(prefs.getLong(CarLocationManager.PREF_CAR_TIME + id, 0));
            car.location = lastLocation;
            car.time = date;

        }

        Log.i(TAG, "Stored car was: " + car);

        return car;

    }

    public static void removeStoredLocation(Context context, String id) {
        SharedPreferences prefs = Util.getSharedPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();

        // We store the result
        editor.remove(PREF_CAR_LATITUDE + id);
        editor.remove(PREF_CAR_LONGITUDE + id);
        editor.remove(PREF_CAR_ACCURACY + id);
        editor.remove(PREF_CAR_PROVIDER + id);
        editor.remove(PREF_CAR_TIME + id);

        editor.apply();

        Log.i(TAG, "Removed location");
    }


}