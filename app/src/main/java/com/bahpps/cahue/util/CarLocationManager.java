package com.bahpps.cahue.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by francesco on 16.09.2014.
 */
public class CarLocationManager {

    public final static String INTENT = "CAR_MOVED_INTENT";
    public final static String INTENT_POSITION = "CAR_MOVED_INTENT_POSITION";

    private final static String TAG = "CarLocationManager";


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
        editor.putString(Util.PREF_CAR_PROVIDER, loc.getProvider());
        editor.putLong(Util.PREF_CAR_TIME, Calendar.getInstance().getTimeInMillis());

        editor.apply();

        Log.i(TAG, "Stored new location: " + loc);

        Intent intent = new Intent(INTENT);
        intent.putExtra(INTENT_POSITION, loc);
        context.sendBroadcast(intent);
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

        Log.i(TAG, "Stored location was: " + lastLocation);

        return lastLocation;

    }

}
