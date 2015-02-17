package com.bahpps.cahue.cars.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import com.bahpps.cahue.cars.Car;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Francesco on 22/01/2015.
 */
public class CarDatabase  {

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
            Car.COLUMN_TIME
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
    public void saveCars(Collection<Car> cars) {

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {
            for (Car car : cars) {

                if (car.id == null)
                    throw new NullPointerException("Car without an ID");

                ContentValues values = getCarContentValues(car);
                database.insertWithOnConflict(Car.TABLE_NAME, Car.COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
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
    public void saveCar(Car car) {
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
    }

    private ContentValues getCarContentValues(Car car) {
        ContentValues values = new ContentValues();
        values.put(Car.COLUMN_ID, car.id);
        values.put(Car.COLUMN_NAME, car.name);
        values.put(Car.COLUMN_BT_ADDRESS, car.btAddress);
        values.put(Car.COLUMN_COLOR, car.color);

        if (car.location != null) {
            values.put(Car.COLUMN_LATITUDE, car.location.getLatitude());
            values.put(Car.COLUMN_LONGITUDE, car.location.getLongitude());
            values.put(Car.COLUMN_ACCURACY, car.location.getAccuracy());
        }
        values.put(Car.COLUMN_TIME, car.time.getTime());
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
                    null, null, null, null);

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

            if (cursor.getCount() > 1)
                throw new IllegalStateException("Can't have more than 1 car with the same BT address");

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
        }

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

}