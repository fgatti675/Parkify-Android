package com.bahpps.cahue.parkedCar;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Francesco on 22/01/2015.
 */
public class CarDatabase extends SQLiteOpenHelper {

    /** Schema version. */
    public static final int DATABASE_VERSION = 1;

    /** Filename for SQLite file. */
    public static final String DATABASE_NAME = "cars.db";

    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_REAL = " REAL";
    private static final String COMMA_SEP = ",";

    /** SQL statement to create "CAR" table. */
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + Car.TABLE_NAME + " (" +
                    Car._ID + " INTEGER PRIMARY KEY," +
                    Car.COLUMN_ID + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_BT_ADDRESS + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_NAME + TYPE_TEXT + COMMA_SEP +
                    Car.COLUMN_LATITUDE + TYPE_REAL + COMMA_SEP +
                    Car.COLUMN_LONGITUDE + TYPE_REAL + COMMA_SEP +
                    Car.COLUMN_ACCURACY + TYPE_REAL + COMMA_SEP +
                    Car.COLUMN_CAR_TIME + TYPE_INTEGER + ")";

    /** SQL statement to drop "entry" table. */
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + Car.TABLE_NAME;

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
}