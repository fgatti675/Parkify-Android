package com.bahpps.cahue.auxiliar;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import java.net.ContentHandler;

/**
 * Created by francesco on 16.09.2014.
 */
public class CarLocationManager {


    /**
     * Persist the location of the car in the shared preferences
     * @param loc
     */
    public static void saveLocation(Context context, Location loc){
        SharedPreferences prefs = Util.getSharedPreferences(context);

        SharedPreferences.Editor editor = prefs.edit();

        // We store the result
        editor.putInt(Util.PREF_CAR_LATITUDE, (int) (loc.getLatitude() * 1E6));
        editor.putInt(Util.PREF_CAR_LONGITUDE, (int) (loc.getLongitude() * 1E6));
        editor.putInt(Util.PREF_CAR_ACCURACY, (int) (loc.getAccuracy() * 1E6));
    }

    public static Location getStoredLocation(Context context){

        SharedPreferences prefs = Util.getSharedPreferences(context);

        // Details of the last location fix
        int lastLatitude = prefs.getInt(Util.PREF_CAR_LATITUDE, 0);
        int lastLongitude = prefs.getInt(Util.PREF_CAR_LONGITUDE, 0);
        int lastAccuracy = prefs.getInt(Util.PREF_CAR_ACCURACY, 0);

        if(lastLatitude == 0 || lastLongitude == 0) return null;

        Location lastLocation = new Location(prefs.getString(Util.PREF_CAR_PROVIDER, ""));
        lastLocation.setLatitude(lastLatitude / 1E6);
        lastLocation.setLongitude(lastLongitude / 1E6);
        lastLocation.setAccuracy((float) (lastAccuracy / 1E6));

        return lastLocation;

    }
}
