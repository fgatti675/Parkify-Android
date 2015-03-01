package com.bahpps.cahue.cars.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Created by Francesco on 25/02/2015.
 */
public class CarsProvider extends ContentProvider {

    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = "com.bahpps.cahue.cars";

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
    public String getType(Uri uri) {
        return new String();
    }

    /*
     * query() always returns no results
     *
     */
    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        return null;
    }
    /*
     * insert() always returns null (no URI)
     */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }
    /*
     * delete() always returns "no rows affected" (0)
     */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }
    /*
     * update() always returns "no rows affected" (0)
     */
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
        return 0;
    }
}
