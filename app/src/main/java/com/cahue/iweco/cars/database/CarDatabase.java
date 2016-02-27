package com.cahue.iweco.cars.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;

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

    public static final String TABLE_CARS = "cars";
    public static final String TABLE_POSSIBLE_SPOTS = "possible_spots";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_BT_ADDRESS = "bt_address";
    public static final String COLUMN_SPOT_ID = "spot_id";
    public static final String COLUMN_LATITUDE = "latitude";
    public static final String COLUMN_LONGITUDE = "longitude";
    public static final String COLUMN_ACCURACY = "accuracy";
    public static final String COLUMN_ADDRESS = "address";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_COLOR = "color";
    public static final String[] CAR_PROJECTION = new String[]{
            COLUMN_ID,
            COLUMN_NAME,
            COLUMN_BT_ADDRESS,
            COLUMN_COLOR,
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_ACCURACY,
            COLUMN_TIME,
            COLUMN_ADDRESS,
            COLUMN_SPOT_ID
    };

    private static final String[] SPOT_PROJECTION = new String[]{
            COLUMN_LATITUDE,
            COLUMN_LONGITUDE,
            COLUMN_ACCURACY,
            COLUMN_TIME,
            COLUMN_ADDRESS
    };
    private static final int MAX_POSSIBLE_SPOTS = 5;
    private static final String TAG = CarDatabase.class.getSimpleName();
    private static CarDatabase mInstance;
    private final Context context;

    private CarDatabase(Context context) {
        this.context = context;
    }

    public static CarDatabase getInstance(@NonNull Context ctx) {
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

    /**
     * Create the 'Other' car
     */
    @NonNull
    public Car generateOtherCar() {
        Car car = new Car();
        car.id = Car.OTHER_ID;
        return car;
    }

    /**
     * Persist the location of the car in the shared preferences
     *
     * @param cars
     */
    public void clearSaveAndBroadcast(@NonNull Collection<Car> cars) {

        cars.add(generateOtherCar());

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {

            database.delete(TABLE_CARS, null, null);

            for (Car car : cars) {

                if (car.id == null)
                    throw new NullPointerException("Car without an ID");

                ContentValues values = createCarContentValues(car);
                database.insertWithOnConflict(TABLE_CARS, COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);

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
    public void updateSpotId(@NonNull Car car) {

        Log.i(TAG, "Updating spot: " + car);

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            if (car.id == null)
                throw new NullPointerException("Car without an ID");

            ContentValues values = new ContentValues();
            values.put(COLUMN_SPOT_ID, car.spotId);
            database.update(TABLE_CARS, values, "id = '" + car.id + "'", null);

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
    public void updateAddress(@NonNull Car car) {

        Log.i(TAG, "Updating address: " + car);

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            if (car.id == null)
                throw new NullPointerException("Car without an ID");

            ContentValues values = new ContentValues();
            values.put(COLUMN_ADDRESS, car.address);
            database.update(TABLE_CARS, values, "id = '" + car.id + "'", null);

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
    public void saveCarAndBroadcast(@NonNull Car car) {

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            if (car.id == null)
                throw new NullPointerException("Car without an ID");

            ContentValues values = createCarContentValues(car);
            database.insertWithOnConflict(TABLE_CARS, COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

        broadCastCarUpdate(car);

    }

    /**
     * Persist date about a car, location included
     *
     * @param car
     */
    public void updateCarRemoveSpotAndBroadcast(@NonNull Car car, @NonNull ParkingSpot spot) {

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            if (car.id == null)
                throw new NullPointerException("Car without an ID");

            ContentValues values = createCarContentValues(car);
            database.insertWithOnConflict(TABLE_CARS, COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);
            database.delete(TABLE_POSSIBLE_SPOTS, COLUMN_TIME + " = '" + spot.time.getTime() + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }

        broadCastCarUpdate(car);

    }

    /**
     * Tell everyone interested that this car was updated
     */
    private void broadCastCarUpdate(@NonNull Car car) {

        Log.d(TAG, "Sending car update broadcast");

        Intent intent = new Intent(Constants.INTENT_CAR_UPDATED);
        intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
        context.sendBroadcast(intent);
    }

    /**
     * Create ContentValues from Car
     *
     * @param car
     * @return
     */
    @NonNull
    private ContentValues createCarContentValues(@NonNull Car car) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, car.id);
        values.put(COLUMN_NAME, car.name);
        values.put(COLUMN_BT_ADDRESS, car.btAddress);
        values.put(COLUMN_COLOR, car.color);

        if (car.location != null) {
            values.put(COLUMN_SPOT_ID, car.spotId);
            values.put(COLUMN_LATITUDE, car.location.getLatitude());
            values.put(COLUMN_LONGITUDE, car.location.getLongitude());
            values.put(COLUMN_ACCURACY, car.location.getAccuracy());
            values.put(COLUMN_TIME, car.time != null ? car.time.getTime() : null);
            values.put(COLUMN_ADDRESS, car.address);
        }

        return values;
    }


    @NonNull
    public Set<String> getPairedBTAddresses() {
        Set<String> addresses = new HashSet<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(true,
                    TABLE_CARS,
                    new String[]{COLUMN_BT_ADDRESS},
                    COLUMN_BT_ADDRESS + " IS NOT NULL",
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
     * @param includeOther Should the "Other" car be included
     * @return
     */
    @NonNull
    public List<String> getCarIds(boolean includeOther) {
        List<String> ids = new ArrayList<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();

        // was the 'Other' car already in the database
        try {

            Cursor cursor = database.query(TABLE_CARS,
                    new String[]{COLUMN_ID},
                    includeOther ? null : COLUMN_ID + " != '" + Car.OTHER_ID + "'",
                    null, null, null,
                    COLUMN_TIME + " DESC");

            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                ids.add(id);
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
     * @param includeOther Should the "Other" car be included
     * @return
     */
    @NonNull
    public List<Car> retrieveCars(boolean includeOther) {
        List<Car> cars = new ArrayList<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();

        try {
            Cursor cursor = database.query(TABLE_CARS,
                    CAR_PROJECTION,
                    includeOther ? null : COLUMN_ID + " != '" + Car.OTHER_ID + "'",
                    null, null, null,
                    COLUMN_TIME + " DESC");

            while (cursor.moveToNext()) {
                Car car = cursorToCar(cursor);
                cars.add(car);
            }
            cursor.close();

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

        Log.d(TAG, "Retrieved cars from DB: " + cars);
        return cars;
    }

    /**
     * Get a Collection of available cars, location will be not null
     *
     * @return
     */
    @NonNull
    public List<Car> retrieveParkedCars() {
        List<Car> cars = new ArrayList<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {

            Cursor cursor = database.query(TABLE_CARS,
                    CAR_PROJECTION,
                    COLUMN_LATITUDE + " > 0",
                    null, null, null,
                    COLUMN_TIME + " DESC");

            while (cursor.moveToNext()) {
                cars.add(cursorToCar(cursor));
            }
            cursor.close();

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

        Log.d(TAG, "Retrieved cars from DB: " + cars);

        return cars;
    }


    @Nullable
    public Car findCar(String id) {

        Log.d(TAG, "Querying car by ID: " + id);
        Car car;
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(TABLE_CARS,
                    CAR_PROJECTION,
                    COLUMN_ID + " = '" + id + "'",
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

    @Nullable
    public Car findCarByBTAddress(String btAddress) {
        Car car;
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(TABLE_CARS,
                    CAR_PROJECTION,
                    COLUMN_BT_ADDRESS + " = '" + btAddress + "'",
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


    @NonNull
    private Car cursorToCar(@NonNull Cursor cursor) {

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
            database.execSQL("delete from " + TABLE_CARS);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    public void deleteCar(@NonNull Car car) {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {
            database.delete(TABLE_CARS, "id = '" + car.id + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    public void deleteCar(String carId) {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();
        try {
            database.delete(TABLE_CARS, "id = '" + carId + "'", null);
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
    public boolean isEmptyOfCars() {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {
            Cursor cursor = database.query(TABLE_CARS,
                    new String[]{COLUMN_ID},
                    COLUMN_ID + " != '" + Car.OTHER_ID + "'",
                    null, null, null, null);

            boolean res = cursor.getCount() == 0;
            cursor.close();
            return res;
        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    public void addPossibleParkingSpot(@NonNull ParkingSpot spot) {
        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            ContentValues values = createSpotContentValues(spot);

            database.insertWithOnConflict(TABLE_POSSIBLE_SPOTS, COLUMN_TIME, values, SQLiteDatabase.CONFLICT_REPLACE);

            Cursor cursor = database.query(TABLE_POSSIBLE_SPOTS,
                    new String[]{COLUMN_TIME},
                    null,
                    null, null, null,
                    COLUMN_TIME + " DESC");

            List<Long> parkingTimes = new ArrayList<>();
            while (cursor.moveToNext()) {
                parkingTimes.add(cursor.getLong(0));
            }
            cursor.close();

            if (parkingTimes.size() > MAX_POSSIBLE_SPOTS) {
                for (Long time : parkingTimes.subList(MAX_POSSIBLE_SPOTS, parkingTimes.size())) {
                    database.delete(TABLE_POSSIBLE_SPOTS, COLUMN_TIME + " = '" + time.toString() + "'", null);
                }
            }

        } finally {
            database.close();
            carDatabaseHelper.close();
        }
    }

    /**
     * Create ContentValues from Car
     *
     * @param spot
     * @return
     */
    @NonNull
    private ContentValues createSpotContentValues(@NonNull ParkingSpot spot) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_LATITUDE, spot.location.getLatitude());
        values.put(COLUMN_LONGITUDE, spot.location.getLongitude());
        values.put(COLUMN_ACCURACY, spot.location.getAccuracy());
        values.put(COLUMN_TIME, spot.time != null ? spot.time.getTime() : null);
        values.put(COLUMN_ADDRESS, spot.address);

        return values;
    }

    /**
     * Get a Collection of possible parking spots
     *
     * @return
     */
    @NonNull
    public List<ParkingSpot> retrievePossibleParkingSpots() {
        List<ParkingSpot> spots = new ArrayList<>();

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getReadableDatabase();
        try {

            Cursor cursor = database.query(TABLE_POSSIBLE_SPOTS,
                    SPOT_PROJECTION,
                    null,
                    null, null, null,
                    COLUMN_TIME + " DESC");

            while (cursor.moveToNext()) {
                spots.add(cursorToSpot(cursor));
            }
            cursor.close();

        } finally {
            database.close();
            carDatabaseHelper.close();
        }

        Log.d(TAG, "Retrieved cars from DB: " + spots);

        return spots;
    }

    /**
     * Persist date about a car, location included
     *
     * @param spot
     */
    public void removeParkingSpot(@NonNull ParkingSpot spot) {

        CarDatabaseHelper carDatabaseHelper = new CarDatabaseHelper(context);
        SQLiteDatabase database = carDatabaseHelper.getWritableDatabase();

        try {
            database.delete(TABLE_POSSIBLE_SPOTS, COLUMN_TIME + " = '" + spot.time.getTime() + "'", null);
        } finally {
            database.close();
            carDatabaseHelper.close();
        }

    }

    @NonNull
    private ParkingSpot cursorToSpot(@NonNull Cursor cursor) {

        double latitude = cursor.getDouble(0);
        double longitude = cursor.getDouble(1);
        float accuracy = cursor.getFloat(2);
        Location location = new Location("db");
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setAccuracy(accuracy);
        Date time = new Date(cursor.getLong(3));
        String address = cursor.isNull(4) ? null : cursor.getString(4);

        return new ParkingSpot(null, location, address, time, false);
    }
}