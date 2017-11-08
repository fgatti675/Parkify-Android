package com.cahue.iweco.cars.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Created by Francesco on 25/02/2015.
 */
public class CarsProvider extends ContentProvider {

    public static final long SYNC_FREQUENCY = 5 * 60;  // 3 hour (in seconds)

    /*
        * Always return true, indicating that the
        * provider loaded correctly.
        */
    @Override
    public boolean onCreate() {
        return true;
    }

    /*
     * Return an empty String for MIME type
     */
    @Override
    public String getType(@NonNull Uri uri) {
        return "";
    }

    /*
     * query() always returns no results
     *
     */
    @Override
    public Cursor query(
            @NonNull Uri uri,
            @NonNull String[] projection,
            @NonNull String selection,
            @NonNull String[] selectionArgs,
            @NonNull String sortOrder) {
        return null;
    }

    /*
     * insert() always returns null (no URI)
     */
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    /*
     * deleteCar() always returns "no rows affected" (0)
     */
    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    /*
     * update() always returns "no rows affected" (0)
     */
    public int update(
            @NonNull Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        return 0;
    }
}
