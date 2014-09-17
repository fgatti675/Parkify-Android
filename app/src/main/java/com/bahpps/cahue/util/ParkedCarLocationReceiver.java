package com.bahpps.cahue.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.bahpps.cahue.R;
import com.bahpps.cahue.location.LocationPoller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Set;

/**
 * This class is in charge of receiving location updates, after and store it as the cars position. It is in charge of deciding
 * the most accurate position (where we think the car is), based on wifi and gps position providers.
 *
 * @author Francesco
 */
public class ParkedCarLocationReceiver extends BroadcastReceiver {

    private final static String TAG = "ParkedCarPositionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "Position received at ParkedCarPositionReceiver");


        // We get the location from the intent
        Bundle b = intent.getExtras();
        Location loc = (Location) b.get(context.getString(R.string.intent_extra_car_location));
        if (loc != null) {

            SharedPreferences prefs = Util.getSharedPreferences(context);

            long lastBTRequest = prefs.getLong(BluetoothDetector.PREF_BT_DISCONNECTION_TIME, 0);
            long lastPositionUpdate = prefs.getLong(Util.PREF_CAR_TIME, 0);

            long currentTime = (new GregorianCalendar().getTimeInMillis());

            // Time pased since BT disconnection
            long difference = (currentTime - lastBTRequest);

            // What was the last provider?


            Location lastLocation = CarLocationManager.getStoredLocation(context);

            // If we have received another position fix recently, we evaluate which one is best
            // We make a pounded average taking into account: time passed from bt disconnection,
            // accuracy and provider of each fix.
            if (difference < Util.PREF_POSITIONING_TIME_LIMIT
                    && loc.getProvider().equals("gps")
                    && !lastLocation.getProvider().equals(Util.TAPPED_PROVIDER)
                    && loc.distanceTo(lastLocation) < Util.MAX_ALLOWED_DISTANCE
                    && loc.getTime() > lastBTRequest) {

                Log.i("LocationReceiver", "Doing average: " + loc.toString());

                difference = (lastPositionUpdate - lastBTRequest);

                // Time limit that will be used as a weigh in a pounded average in seconds
                int lastWeight = Util.PREF_POSITIONING_TIME_LIMIT / 1000;

                // We reduce the weight of the last fix, depending on its accuracy
                lastWeight /= lastLocation.getAccuracy();

                int inverseDifferenceSecs = (int) ((Util.PREF_POSITIONING_TIME_LIMIT - difference) / 1000);


                // We make pounded averages of every parameter
                double newLatitude = poundedAverage(lastLocation.getLatitude(), lastWeight, loc.getLatitude(), inverseDifferenceSecs);
                double newLongitude = poundedAverage(lastLocation.getLongitude(), lastWeight, loc.getLongitude(), inverseDifferenceSecs);
                double newAccuracy = poundedAverage(lastLocation.getAccuracy(), lastWeight, loc.getAccuracy(), inverseDifferenceSecs);

                // We set the new location based on the computed average
                Location calculatedPosition = new Location("Average");
                calculatedPosition.setLatitude(newLatitude);
                calculatedPosition.setLongitude(newLongitude);
                calculatedPosition.setAccuracy((float) (newAccuracy));

                CarLocationManager.saveLocation(context, calculatedPosition);

            }


            // If we receive a wifi fix after a gps one on the same location request, we do nothing
            else if (difference < Util.PREF_POSITIONING_TIME_LIMIT
                    && loc.getProvider().equals("network")
                    && lastLocation.getProvider().equals("gps")) {


            } else {

                // We store the result
                CarLocationManager.saveLocation(context, loc);
            }

        }


    }

    /**
     * Method that returns the average of 2 double values, giving a different weight to each one
     *
     * @param a
     * @param aWeight
     * @param b
     * @param bWeight
     * @return
     */
    private double poundedAverage(double a, double aWeight, double b, double bWeight) {
        return (a * aWeight + b * bWeight) / (aWeight + bWeight);
    }
}
