package com.bahpps.cahue.auxiliar;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bahpps.cahue.R;

public class Util {

	/*
	 * Shared preferences constants
	 */
	public static final String PREF_DIALOG_SHOWN = "PREF_DIALOG_SHOWN";
	public static final String PREF_CAR_LATITUDE = "PREF_CAR_LATITUDE";
	public static final String PREF_CAR_LONGITUDE = "PREF_CAR_LONGITUDE";
	public static final String PREF_CAR_ACCURACY = "PREF_CAR_ACCURACY";
	public static final String PREF_CAR_PROVIDER = "PREF_CAR_PROVIDER";
	public static final String PREF_CAR_TIME = "PREF_CAR_TIME";

	public static final float WALKING_SPEED = 1.4F; // walking speed in m/s

	/*
	 * Maximum distance we consider normal between 2 positions received on the same parking
	 */
	public static final int MAX_ALLOWED_DISTANCE = 100; // meters

	public static final int PREF_POSITIONING_TIME_LIMIT = 30000; // 30 secs
	public static final float DEFAULT_ACCURACY = 7; // in meters, used when the user taps the map
    public static final String TAPPED_PROVIDER = "Tapped";

    /**
	 * Method for printing our fancy custom Toast
	 * 
	 * @param context
	 * @param string
	 *            Content of the toast
	 * @param length
	 *            One from Toast.LENGTH_LONG or Toast.LENGTH_SHORT
	 */
	public static void createToast(Context context, String string, int length) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.custom_toast, null);
		TextView text = (TextView) layout.findViewById(R.id.text);
		text.setText(string);

		Toast toast = new Toast(context.getApplicationContext());
		toast.setGravity(Gravity.FILL_HORIZONTAL, 0, 0);
		toast.setDuration(length);
		toast.setView(layout);
		toast.show();
	}

	/**
	 * Shows a toast in case no BT is detected
	 */
	public static void noBluetooth(Context context) {
		Util.createToast(context, context.getString(R.string.bt_not_available), Toast.LENGTH_SHORT);
	}


	/**
	 * It returns the only shared preferences file we will be using in the app.
	 *
	 * @param context
	 * @return
	 */
	public static SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences("WIMC", Context.MODE_WORLD_READABLE);
	}



}
