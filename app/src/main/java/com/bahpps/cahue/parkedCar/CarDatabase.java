package com.bahpps.cahue.parkedCar;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
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
                    Car._ID + " INTEGER PRIMARY KEY," +
                    Car.COLUMN_ID + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_NAME + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_BT_ADDRESS + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_BT_NAME + TYPE_TEXT + COMMA_SEP +
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
            Car.COLUMN_BT_NAME,
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
    }

    /**
     * Get a Collection of available cars, can have the location set to null
     *
     * @param onlyParked
     * @return
     */
    public List<Car> retrieveCars(boolean onlyParked) {
        List<Car> cars = new ArrayList<>();

        return cars;
    }


    public Car findByBTAddress(String btAddress) {

        Cursor cursor = getReadableDatabase().query(Car.TABLE_NAME,
                PROJECTION,
                Car.COLUMN_BT_ADDRESS + " = ?s",
                new String[]{btAddress},
                null, null, null);

        if(cursor.getCount() == 0) return null;

        if(cursor.getCount() > 1)
            throw new IllegalStateException("Can't have more than 1 car with the same BT address");


        cursor.moveToFirst();
        Car car = cursorToCar(cursor);

        return car;

    }

    private Car cursorToCar(Cursor cursor) {
        Car car = new Car();
        car.id = cursor.getString(0);

        return car;
    }


    private Car setBTName(String btAddress, Set<BluetoothDevice> bondedBTDevices) {
        Car car = new Car();

        return car;
    }

    public void removeStoredLocation(Context context, Car car) {
    }
}