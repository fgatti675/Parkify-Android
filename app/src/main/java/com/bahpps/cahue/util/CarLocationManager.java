package com.bahpps.cahue.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by francesco on 16.09.2014.
 */
public class CarLocationManager {

    public final static String INTENT = "CAR_MOVED_INTENT";
    public final static String INTENT_POSITION = "CAR_MOVED_INTENT_POSITION";

    public static final String PREF_CAR_LATITUDE = "PREF_CAR_LATITUDE";
    public static final String PREF_CAR_LONGITUDE = "PREF_CAR_LONGITUDE";
    public static final String PREF_CAR_ACCURACY = "PREF_CAR_ACCURACY";
    public static final String PREF_CAR_PROVIDER = "PREF_CAR_PROVIDER";
    public static final String PREF_CAR_TIME = "PREF_CAR_TIME";

    private final static String TAG = "CarLocationManager";


    /**
     * Persist the location of the car in the shared preferences
     * @param loc
     */
    public static void saveLocation(Context context, Location loc){
        SharedPreferences prefs = Util.getSharedPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();

        // We store the result
        editor.putInt(PREF_CAR_LATITUDE, (int) (loc.getLatitude() * 1E6));
        editor.putInt(PREF_CAR_LONGITUDE, (int) (loc.getLongitude() * 1E6));
        editor.putInt(PREF_CAR_ACCURACY, (int) (loc.getAccuracy() * 1E6));
        editor.putString(PREF_CAR_PROVIDER, loc.getProvider());
        editor.putLong(PREF_CAR_TIME, Calendar.getInstance().getTimeInMillis());

        editor.apply();

        Log.i(TAG, "Stored new location: " + loc);

        Intent intent = new Intent(INTENT);
        intent.putExtra(INTENT_POSITION, loc);
        context.sendBroadcast(intent);
    }

    public static Location getStoredLocation(Context context){

        SharedPreferences prefs = Util.getSharedPreferences(context);

        // Details of the last location fix
        int lastLatitude = prefs.getInt(PREF_CAR_LATITUDE, 0);
        int lastLongitude = prefs.getInt(PREF_CAR_LONGITUDE, 0);
        int lastAccuracy = prefs.getInt(PREF_CAR_ACCURACY, 0);

        if(lastLatitude == 0 || lastLongitude == 0) return null;

        Location lastLocation = new Location(prefs.getString(PREF_CAR_PROVIDER, ""));
        lastLocation.setLatitude(lastLatitude / 1E6);
        lastLocation.setLongitude(lastLongitude / 1E6);
        lastLocation.setAccuracy((float) (lastAccuracy / 1E6));

        Log.i(TAG, "Stored location was: " + lastLocation);

        return lastLocation;

    }

    public static void removeStoredLocation(Context context){
        SharedPreferences prefs = Util.getSharedPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();

        // We store the result
        editor.remove(PREF_CAR_LATITUDE);
        editor.remove(PREF_CAR_LONGITUDE);
        editor.remove(PREF_CAR_ACCURACY);
        editor.remove(PREF_CAR_PROVIDER);
        editor.remove(PREF_CAR_TIME);

        editor.apply();

        Log.i(TAG, "Removed location");
    }

}
