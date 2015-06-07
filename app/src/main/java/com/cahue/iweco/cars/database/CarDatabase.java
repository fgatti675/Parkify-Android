package com.cahue.iweco.cars.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.Car;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Francesco on 22/01/2015.
 */
public class CarDatabase {

    private static CarDatabase mInstance;
    private Context context;

    public static CarDatabase getInstance(Context ctx) {
        /**
         * use the application context as suggested by CommonsWare.
         * this will ensure that you dont accidentally leak an Activitys
         * context (see this article for more information:
         * http://android-developers.blogspot.nl/2009/01/avoiding-memory-leaks.html)
         */
        if (mInstance == null) {
            mInstance = new CarDatabase(ctx.getApplicationContext());
        }
        return mInstance;
    }

    public static final String[] PROJECTION = new String[]{
            Car.COLUMN_ID,
            Car.COLUMN_NAME,
            Car.COLUMN_BT_ADDRESS,
            Car.COLUMN_COLOR,
            Car.COLUMN_LATITUDE,
            Car.COLUMN_LONGITUDE,
            Car.COLUMN_ACCURACY,
            Car.COLUMN_TIME,
            Car.COLUMN_ADDRESS,
            Car.COLUMN_SPOT_ID
    };

    private static final String TAG = CarDatabase.class.getSimpleName();

    private CarDatabase(Context context) {
        this.context = context;
    }

    /**
     * Persist the location of the car in the shared preferences
     *
     * @param cars
     */
    public void save(Collection<Car> cars) {

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {

            database.delete(Car.TABLE_NAME, null, null);

            for (Car car : cars) {

                if (car.id == null)
                    throw new NullPointerException("Car without an ID");

                ContentValues values = getCarContentValues(car);
                database.insertWithOnConflict(Car.TABLE_NAME, Car.COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);

                broadCastCarUpdate(car);
            }
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    /**
     * Update the spot ID of a parked car
     *
     * @param car
     */
    public void updateSpotId(Car car) {

        Log.i(TAG, "Updating spot: " + car);

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            if (car.id == null)
                throw new NullPointerException("Car without an ID");

            ContentValues values = new ContentValues();
            values.put(Car.COLUMN_SPOT_ID, car.spotId);
            database.update(Car.TABLE_NAME, values, "id = '" + car.id + "'", null);

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

    }

    /**
     * Update the spot ID of a parked car
     *
     * @param car
     */
    public void updateAddress(Car car) {

        Log.i(TAG, "Updating address: " + car);

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            if (car.id == null)
                throw new NullPointerException("Car without an ID");

            ContentValues values = new ContentValues();
            values.put(Car.COLUMN_ADDRESS, car.address);
            database.update(Car.TABLE_NAME, values, "id = '" + car.id + "'", null);

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

    }

    /**
     * Persist date about a car, location included
     *
     * @param car
     */
    public void saveAndBroadcast(Car car) {

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            if (car.id == null)
                throw new NullPointerException("Car without an ID");

            ContentValues values = getCarContentValues(car);
            database.insertWithOnConflict(Car.TABLE_NAME, Car.COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

        broadCastCarUpdate(car);

    }

    /**
     * Tell everyone interested that this car was updated
     */
    private void broadCastCarUpdate(Car car) {

        Log.d(TAG, "Sending car update broadcast");

        Intent intent = new Intent(Constants.INTENT_CAR_UPDATE);
        intent.putExtra(Constants.INTENT_CAR_EXTRA_ID, car.id);
        context.sendBroadcast(intent);
    }

    private ContentValues getCarContentValues(Car car) {
        ContentValues values = new ContentValues();
        values.put(Car.COLUMN_ID, car.id);
        values.put(Car.COLUMN_NAME, car.name);
        values.put(Car.COLUMN_BT_ADDRESS, car.btAddress);
        values.put(Car.COLUMN_COLOR, car.color);

        if (car.location != null) {
            values.put(Car.COLUMN_SPOT_ID, car.spotId);
            values.put(Car.COLUMN_LATITUDE, car.location.getLatitude());
            values.put(Car.COLUMN_LONGITUDE, car.location.getLongitude());
            values.put(Car.COLUMN_ACCURACY, car.location.getAccuracy());
            values.put(Car.COLUMN_TIME, car.time.getTime());
            values.put(Car.COLUMN_ADDRESS, car.address);
        }

        return values;
    }


    public Set<String> getPairedBTAddresses() {
        Set<String> addresses = new HashSet<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(true,
                    Car.TABLE_NAME,
                    new String[]{Car.COLUMN_BT_ADDRESS},
                    Car.COLUMN_BT_ADDRESS + " IS NOT NULL",
                    null, null, null, null, null);

            Log.d(TAG, "Paired BT addresses: ");
            while (cursor.moveToNext()) {
                String address = cursor.getString(0);
                Log.d(TAG, "\t" + address);
                addresses.add(address);
            }
            cursor.close();

        } finally {
            database.close();
        }

        return addresses;
    }

    /**
     * Get a Collection of available car ids
     *
     * @return
     */
    public List<String> getCarIds() {
        List<String> ids = new ArrayList<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {

            Cursor cursor = database.query(Car.TABLE_NAME,
                    new String[]{Car.COLUMN_ID},
                    null, null, null, null,
                    Car.COLUMN_TIME + " DESC");

            while (cursor.moveToNext()) {
                ids.add(cursor.getString(0));
            }
            cursor.close();

        } finally {
            database.close();
        }

        Log.d(TAG, "Retrieved car ids from DB: " + ids);

        return ids;
    }

    /**
     * Get a Collection of available cars, can have the location set to null
     *
     * @param onlyParked
     * @return
     */
    public List<Car> retrieveCars(boolean onlyParked) {
        List<Car> cars = new ArrayList<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {

            Cursor cursor = database.query(Car.TABLE_NAME,
                    PROJECTION,
                    onlyParked ? Car.COLUMN_LATITUDE + " > 0" : null,
                    null, null, null,
                    Car.COLUMN_TIME + " DESC");

            while (cursor.moveToNext()) {
                cars.add(cursorToCar(cursor));
            }
            cursor.close();

        } finally {
            database.close();
        }

        Log.d(TAG, "Retrieved cars from DB: " + cars);

        return cars;
    }

    public Car find(String id) {
        Car car;
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(Car.TABLE_NAME,
                    PROJECTION,
                    Car.COLUMN_ID + " = '" + id + "'",
                    null, null, null, null);

            if (cursor.getCount() == 0) return null;

            cursor.moveToFirst();

            car = cursorToCar(cursor);
            cursor.close();

            Log.d(TAG, "Retrieved car by ID: " + car);

        } finally {
            database.close();
        }

        return car;

    }

    public Car findByBTAddress(String btAddress) {
        Car car;
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(Car.TABLE_NAME,
                    PROJECTION,
                    Car.COLUMN_BT_ADDRESS + " = '" + btAddress + "'",
                    null, null, null, null);

            if (cursor.getCount() == 0) return null;

            cursor.moveToFirst();

            car = cursorToCar(cursor);
            cursor.close();

            Log.d(TAG, "Retrieved car by BT address: " + car);

        } finally {
            database.close();
        }

        return car;

    }


    private Car cursorToCar(Cursor cursor) {

        Car car = new Car();
        car.id = cursor.getString(0);
        car.name = cursor.getString(1);
        car.btAddress = cursor.getString(2);
        car.color = cursor.isNull(3) ? null : cursor.getInt(3);

        if (!cursor.isNull(4) && !cursor.isNull(5)) {
            double latitude = cursor.getDouble(4);
            double longitude = cursor.getDouble(5);
            float accuracy = cursor.getFloat(6);
            Location location = new Location("db");
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAccuracy(accuracy);
            car.location = location;
            car.time = new Date(cursor.getLong(7));
            car.address = cursor.isNull(8) ? null : cursor.getString(8);
        }

        if (!cursor.isNull(9))
            car.spotId = cursor.getLong(9);

        return car;
    }

    /**
     * Remove all the cars in the database
     */
    public void clearCars() {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {
            database.execSQL("delete from " + Car.TABLE_NAME);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    public void delete(Car car) {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {
            database.delete(Car.TABLE_NAME, "id = '" + car.id + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    public void delete(String carId) {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {
            database.delete(Car.TABLE_NAME, "id = '" + carId + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    /**
     * Check if there is at least one car
     *
     * @return
     */
    public boolean isEmpty() {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(Car.TABLE_NAME,
                    new String[]{Car.COLUMN_ID},
                    null, null, null, null, null);

            return cursor.getCount() == 0;
        } finally {
            database.close();
        }
    }
}