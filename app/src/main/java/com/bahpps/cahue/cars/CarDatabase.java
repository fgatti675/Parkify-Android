package com.bahpps.cahue.cars;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Francesco on 22/01/2015.
 */
public class CarDatabase extends SQLiteOpenHelper {

    /**
     * Schema version.
     */
    public static final int DATABASE_VERSION = 2;

    /**
     * Filename for SQLite file.
     */
    public static final String DATABASE_NAME = "cars.db";

    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_REAL = " REAL";
    private static final String COMMA_SEP = ", ";

    /**
     * SQL statement to create "CAR" table.
     */
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Car.TABLE_NAME + " (" +
                    Car.COLUMN_ID + TYPE_TEXT + " PRIMARY KEY" + COMMA_SEP +
                    Car.COLUMN_NAME + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_BT_ADDRESS + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_COLOR + TYPE_INTEGER + COMMA_SEP +
                    Car.COLUMN_LATITUDE + TYPE_REAL + COMMA_SEP +
                    Car.COLUMN_LONGITUDE + TYPE_REAL + COMMA_SEP +
                    Car.COLUMN_ACCURACY + TYPE_REAL + COMMA_SEP +
                    Car.COLUMN_TIME + TYPE_INTEGER + ")";

    /**
     * SQL statement to drop "entry" table.
     */
    private static final String SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + Car.TABLE_NAME;

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

    public CarDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    /**
     * Persist the location of the car in the shared preferences
     *
     * @param cars
     */
    public void saveCars(Collection<Car> cars) {
        SQLiteDatabase database = getWritableDatabase();

        for (Car car : cars) {
            ContentValues values = getCarContentValues(car);
            database.insertWithOnConflict(Car.TABLE_NAME, Car.COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);
        }

        database.close();
    }

    /**
     * Persist date about a car, location included
     *
     * @param car
     */
    public void saveCar(Car car) {
        SQLiteDatabase database = getWritableDatabase();

        ContentValues values = getCarContentValues(car);
        database.insertWithOnConflict(Car.TABLE_NAME, Car.COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);

        database.close();
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
            values.put(Car.COLUMN_TIME, car.time.getTime());
        }
        return values;
    }


    public Set<String> getPairedBTAddresses() {
        Set<String> addresses = new HashSet<>();

        Cursor cursor = getReadableDatabase().query(true,
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

        Cursor cursor = getReadableDatabase().query(Car.TABLE_NAME,
                PROJECTION,
                onlyParked ? Car.COLUMN_LATITUDE + " > 0" : null,
                null, null, null, null);

        while (cursor.moveToNext()) {
            cars.add(cursorToCar(cursor));
        }

        Log.d(TAG, "Retrieved cars from DB: " + cars);

        return cars;
    }


    public Car findByBTAddress(String btAddress) {

        Cursor cursor = getReadableDatabase().query(Car.TABLE_NAME,
                PROJECTION,
                Car.COLUMN_BT_ADDRESS + " = '" + btAddress + "'",
                null, null, null, null);

        if (cursor.getCount() == 0) return null;

        if (cursor.getCount() > 1)
            throw new IllegalStateException("Can't have more than 1 car with the same BT address");

        cursor.moveToFirst();
        Car car = cursorToCar(cursor);

        Log.d(TAG, "Retrieved car by BT address: " + car);
        return car;

    }

    public void clearLocation(Car car) {
        SQLiteDatabase database = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(Car.COLUMN_LATITUDE, (Double) null);
        values.put(Car.COLUMN_LONGITUDE, (Double) null);
        values.put(Car.COLUMN_ACCURACY, (Double) null);
        values.put(Car.COLUMN_TIME, (Long) null);

        database.update(Car.TABLE_NAME, values, Car.COLUMN_ID + " = " + car.id, null);

        database.close();
    }


    private Car cursorToCar(Cursor cursor) {
        Car car = new Car();
        car.id = cursor.getString(0);
        car.name = cursor.getString(1);
        car.btAddress = cursor.getString(2);
        car.color = cursor.getInt(3);

        double latitude = cursor.getDouble(4);
        double longitude = cursor.getDouble(5);
        float accuracy = cursor.getFloat(6);

        if (latitude > 0 && longitude > 0) {
            Location location = new Location("db");
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAccuracy(accuracy);
            car.location = location;
            car.time = new Date(cursor.getLong(7));
        }

        return car;
    }
}