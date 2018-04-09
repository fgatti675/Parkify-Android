package com.cahue.iweco.cars.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cahue.iweco.model.Car;

/**
 * Created by Francesco on 22/01/2015.
 */
public class CarDatabaseHelper extends SQLiteOpenHelper {

    /**
     * Schema version.
     */
    private static final int DATABASE_VERSION = 6;

    /**
     * Filename for SQLite file.
     */
    private static final String DATABASE_NAME = "cars.db";

    private static final String TYPE_TEXT = " TEXT";
    private static final String TYPE_INTEGER = " INTEGER";
    private static final String TYPE_REAL = " REAL";
    private static final String COMMA_SEP = ", ";

    /**
     * SQL statement to create "CAR" table.
     */
    private static final String SQL_CREATE_CAR_ENTRIES =
            "CREATE TABLE " + CarDatabase.TABLE_CARS + " (" +
                    CarDatabase.COLUMN_ID + TYPE_TEXT + " PRIMARY KEY" + COMMA_SEP +
                    CarDatabase.COLUMN_FIRESTORE_ID + TYPE_TEXT + " UNIQUE " + COMMA_SEP +
                    CarDatabase.COLUMN_NAME + TYPE_TEXT + COMMA_SEP +
                    CarDatabase.COLUMN_BT_ADDRESS + TYPE_TEXT + COMMA_SEP +
                    CarDatabase.COLUMN_COLOR + TYPE_INTEGER + COMMA_SEP +
                    CarDatabase.COLUMN_LATITUDE + TYPE_REAL + COMMA_SEP +
                    CarDatabase.COLUMN_LONGITUDE + TYPE_REAL + COMMA_SEP +
                    CarDatabase.COLUMN_ACCURACY + TYPE_REAL + COMMA_SEP +
                    CarDatabase.COLUMN_TIME + TYPE_INTEGER + COMMA_SEP +
                    CarDatabase.COLUMN_ADDRESS + TYPE_TEXT + ")";

    private static final String SQL_CREATE_POSSIBLE_SPOTS_ENTRIES =
            "CREATE TABLE " + CarDatabase.TABLE_POSSIBLE_SPOTS + " (" +
                    CarDatabase.COLUMN_LATITUDE + TYPE_REAL + COMMA_SEP +
                    CarDatabase.COLUMN_LONGITUDE + TYPE_REAL + COMMA_SEP +
                    CarDatabase.COLUMN_ACCURACY + TYPE_REAL + COMMA_SEP +
                    CarDatabase.COLUMN_TIME + TYPE_INTEGER + COMMA_SEP +
                    CarDatabase.COLUMN_ADDRESS + TYPE_TEXT + ")";

    private static final String SQL_ADD_OTHER_CAR =
            "INSERT or replace INTO " + CarDatabase.TABLE_CARS +
                    " (" + CarDatabase.COLUMN_ID + ") VALUES ('" + Car.OTHER_ID + "')";

    private static final String TAG = CarDatabaseHelper.class.getSimpleName();

    CarDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_CAR_ENTRIES);
        db.execSQL(SQL_ADD_OTHER_CAR);
        db.execSQL(SQL_CREATE_POSSIBLE_SPOTS_ENTRIES);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {

        // add address column
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + CarDatabase.TABLE_CARS + " ADD COLUMN " + CarDatabase.COLUMN_ADDRESS + " " + TYPE_TEXT);
        }

        if (oldVersion < 4) {
//            db.execSQL("ALTER TABLE " + CarDatabase.TABLE_CARS + " ADD COLUMN " + CarDatabase.COLUMN_SPOT_ID + " " + TYPE_INTEGER);
        }

        if (oldVersion < 5) {
            db.execSQL(SQL_ADD_OTHER_CAR);
            db.execSQL(SQL_CREATE_POSSIBLE_SPOTS_ENTRIES);
        }

        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + CarDatabase.TABLE_CARS + " ADD COLUMN " + CarDatabase.COLUMN_FIRESTORE_ID + " " + TYPE_TEXT);
        }

        Log.d(TAG, "Database updated from " + oldVersion + " to " + newVersion);
    }


}