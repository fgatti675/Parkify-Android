package com.bahpps.cahue.auxiliar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.bahpps.cahue.location.LocationPoller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is in charge of receiving location updates, and store it as the cars position. It is in charge of deciding
 * the most accurate position (where we think the car is), based on wifi and gps position providers.
 *
 * @author Francesco
 */
public class LocationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        // Log used as debug
        File log = new File(Environment.getExternalStorageDirectory(), "LocationLog.txt");

        // create the log file if it didn't exist
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(log.getAbsolutePath(), log.exists()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We log the date on a new position
        try {
            out.write(new Date().toString());
            out.write(" : ");
            Log.i("LocationReceiver", "Location received");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // We get the location from the intent
        Bundle b = intent.getExtras();
        Location loc = (Location) b.get(LocationPoller.EXTRA_LOCATION);
        if (loc != null) {

            SharedPreferences prefs = context.getSharedPreferences(BluetoothDetector.BT_PREFS, Context.MODE_WORLD_READABLE);

            long lastBTRequest = prefs.getLong(BluetoothDetector.PREF_BT_DISCONNECTION_TIME, 0);
            long lastPositionUpdate = prefs.getLong(Util.PREF_CAR_TIME, 0);

            long currentTime = (new GregorianCalendar().getTimeInMillis());

            // Time pased since BT disconnection
            long difference = (currentTime - lastBTRequest);

            // What was the last provider?
            String lastProvider = prefs.getString(Util.PREF_CAR_PROVIDER, "");

            Editor editor = prefs.edit();

            // Details of the last location fix
            int lastLatitude = prefs.getInt(Util.PREF_CAR_LATITUDE, 0);
            int lastLongitude = prefs.getInt(Util.PREF_CAR_LONGITUDE, 0);
            int lastAccuracy = prefs.getInt(Util.PREF_CAR_ACCURACY, 0);

            Location lastLocation = new Location("Last");
            lastLocation.setLatitude(lastLatitude / 1E6);
            lastLocation.setLongitude(lastLongitude / 1E6);

            // If we have received another position fix recently, we evaluate which one is best
            // We make a punded average taking into account: time passed from bt disconnection,
            // accuracy and provider of each fix.
            if (difference < Util.PREF_POSITIONING_TIME_LIMIT && loc.getProvider().equals("gps")
                    && !lastProvider.equals("Tapped") && (loc.distanceTo(lastLocation) < Util.MAX_ALLOWED_DISTANCE)
                    && loc.getTime() > lastBTRequest) {

                Log.i("LocationReceiver", "Doing average: " + loc.toString());

                difference = (lastPositionUpdate - lastBTRequest);

                // Time limit that will be used as a weigh in a pounded average in seconds
                int lastWeight = Util.PREF_POSITIONING_TIME_LIMIT / 1000;

                // We reduce the weight of the last fix, depending on its accuracy
                lastWeight /= lastAccuracy;

                int inverseDifferenceSecs = (int) ((Util.PREF_POSITIONING_TIME_LIMIT - difference) / 1000);

                try {
                    out.write(loc.toString());
                    out.write("\n");
                } catch (Exception e) {
                    e.printStackTrace();
                }

                double currentLatitude = (loc.getLatitude() * 1E6);
                double currentLongitude = (loc.getLongitude() * 1E6);
                double currentAccuracy = (loc.getAccuracy() * 1E6);

                // We make pounded averages of every parameter
                int newLatitude = (int) Util.poundedAverage(lastLatitude, lastWeight, currentLatitude,
                        inverseDifferenceSecs);
                int newLongitude = (int) Util.poundedAverage(lastLongitude, lastWeight, currentLongitude,
                        inverseDifferenceSecs);
                int newAccuracy = (int) Util.poundedAverage(lastAccuracy, lastWeight, currentAccuracy,
                        inverseDifferenceSecs);

                // We store the result
                editor.putInt(Util.PREF_CAR_LATITUDE, newLatitude);
                editor.putInt(Util.PREF_CAR_LONGITUDE, newLongitude);
                editor.putInt(Util.PREF_CAR_ACCURACY, newAccuracy);

                // We set the new location based on the computated average
                loc = new Location("Average");
                loc.setLatitude(newLatitude / 1E6);
                loc.setLongitude(newLongitude / 1E6);
                loc.setAccuracy((float) (newAccuracy / 1E6));

            } else if (difference < Util.PREF_POSITIONING_TIME_LIMIT && loc.getProvider().equals("network")
                    && lastProvider.equals("gps")) {

                try {
                    out.write("Network fix after GPS: ");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // If we receive a wifi fix after a gps one on the same location request, we do nothing

            } else {

                Log.i("LocationReceiver", "Saving " + loc.toString());

                // We store the result
                editor.putInt(Util.PREF_CAR_LATITUDE, (int) (loc.getLatitude() * 1E6));
                editor.putInt(Util.PREF_CAR_LONGITUDE, (int) (loc.getLongitude() * 1E6));
                editor.putInt(Util.PREF_CAR_ACCURACY, (int) (loc.getAccuracy() * 1E6));
            }

            // We also store provider and time of the fix
            editor.putString(Util.PREF_CAR_PROVIDER, loc.getProvider());
            editor.putLong(Util.PREF_CAR_TIME, Calendar.getInstance().getTimeInMillis());
            editor.commit();
        }

        // Loggin, can be removed
        try {
            String msg;

            if (loc == null) {
                loc = (Location) b.get(LocationPoller.EXTRA_LASTKNOWN);

                if (loc == null) {
                    msg = intent.getStringExtra(LocationPoller.EXTRA_ERROR);
                } else {
                    msg = "TIMEOUT, lastKnown=" + loc.toString();
                }
            } else {
                msg = loc.toString();

                Bundle extras = loc.getExtras();
                if (extras != null) {
                    Set<String> keys = extras.keySet();
                    Iterator<String> iterate = keys.iterator();
                    while (iterate.hasNext()) {
                        String key = iterate.next();
                        msg += "[" + extras.get(key) + "]\n";
                    }
                }
            }

            if (msg == null) {
                msg = "Invalid broadcast received!";
            }

            out.write(msg);
            out.write("\n");
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
