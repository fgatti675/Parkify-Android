package com.bahpps.cahue.util;

import android.content.Context;
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

    private final static String TAG = "CarLocationManager";
    private final static String URL = "http://glossy-radio.appspot.com/spots";


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

    public static void postLocation(Context context){
        try {

            Log.i(TAG, "Posting users location");
            Location lastCarLocation = getStoredLocation(context);

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(URL);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            String json = getJSON(lastCarLocation);
            Log.i(TAG, "Posting\n" + json);
            httpPost.setEntity(new StringEntity(json));

            HttpResponse response = httpclient.execute(httpPost);
            StatusLine statusLine = response.getStatusLine();

            Log.i(TAG, "Post result: " + statusLine.getStatusCode());
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                Log.i(TAG, "Post result: " + EntityUtils.toString(response.getEntity()));
            } else {
                //Closes the connection.
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getJSON(Location location) {
        return String.format(Locale.ENGLISH,
                "{\n" +
                        "    \"latitude\" : \"%f\",\n" +
                        "    \"longitude\": \"%f\"\n" +
                        "}",
                location.getLatitude(),
                location.getLongitude());
    }
}
