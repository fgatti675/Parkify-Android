package com.bahpps.cahue.cars;

import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.util.ArrayList;
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
    public static final int DATABASE_VERSION = 1;

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
            Car.COLUMN_LATITUDE,
            Car.COLUMN_LONGITUDE,
            Car.COLUMN_ACCURACY,
            Car.COLUMN_TIME,
    };

    public CarDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        db.execSQL(SQL_DELETE_ENTRIES);
//        onCreate(db);
    }

    /**
     * Persist the location of the car in the shared preferences
     *
     * @param car
     */
    public void saveCar(Car car) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        ContentValues values = new ContentValues();
        values.put(Car.COLUMN_ID, car.id);
        values.put(Car.COLUMN_NAME, car.name);
        values.put(Car.COLUMN_BT_ADDRESS, car.btAddress);

        if (car.location != null) {
            values.put(Car.COLUMN_LATITUDE, car.location.getLatitude());
            values.put(Car.COLUMN_LONGITUDE, car.location.getLongitude());
            values.put(Car.COLUMN_ACCURACY, (double) car.location.getAccuracy());
            values.put(Car.COLUMN_TIME, car.time.getTime());
        }

        database.insertWithOnConflict(Car.TABLE_NAME, Car.COLUMN_ID, values, SQLiteDatabase.CONFLICT_REPLACE);
        database.endTransaction();

    }


    public Set<String> getPairedBTAddresses() {
        Set<String> addresses = new HashSet<>();

        Cursor cursor = getReadableDatabase().query(true,
                Car.TABLE_NAME,
                new String[]{Car.COLUMN_BT_ADDRESS},
                Car.COLUMN_BT_ADDRESS + " IS NOT NULL",
                null, null, null, null, null);

        while (cursor.moveToNext()) {
            addresses.add(cursor.getString(0));
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

        return cars;
    }


    public Car findByBTAddress(String btAddress) {

        Cursor cursor = getReadableDatabase().query(Car.TABLE_NAME,
                PROJECTION,
                Car.COLUMN_BT_ADDRESS + " = ?s",
                new String[]{btAddress},
                null, null, null);

        if (cursor.getCount() == 0) return null;

        if (cursor.getCount() > 1)
            throw new IllegalStateException("Can't have more than 1 car with the same BT address");


        cursor.moveToFirst();
        Car car = cursorToCar(cursor);

        return car;

    }


    private Car setBTName(String btAddress, Set<BluetoothDevice> bondedBTDevices) {
        Car car = new Car();

        return car;
    }

    public void removeStoredLocation(Car car) {
        SQLiteDatabase database = getWritableDatabase();
        database.beginTransaction();

        ContentValues values = new ContentValues();
        values.put(Car.COLUMN_LATITUDE, -1);
        values.put(Car.COLUMN_LONGITUDE, -1);
        values.put(Car.COLUMN_ACCURACY, -1);
        values.put(Car.COLUMN_TIME, -1);

        database.update(Car.TABLE_NAME, values, Car.COLUMN_ID + " = ?s", new String[]{car.id});

        database.endTransaction();
    }


    private Car cursorToCar(Cursor cursor) {
        Car car = new Car();
        car.id = cursor.getString(0);
        car.name = cursor.getString(1);
        car.btAddress = cursor.getString(2);

        double latitude = cursor.getDouble(3);
        double longitude = cursor.getDouble(4);
        float accuracy = (float) cursor.getDouble(5);
        if (latitude > 0 && longitude > 0) {
            Location location = new Location("db");
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setAccuracy(accuracy);
            car.location = location;
            car.time = new Date(cursor.getLong(6));
        }

        return car;
    }
}