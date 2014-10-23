package com.bahpps.cahue.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
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
	public static void createUpperToast(Context context, String string, int length) {
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.custom_toast, null);
		TextView text = (TextView) layout.findViewById(R.id.error_text);
		text.setText(string);

		Toast toast = new Toast(context.getApplicationContext());

        toast.setGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL, 0, getActionBarSize(context));
		toast.setDuration(length);
		toast.setView(layout);
		toast.show();
	}

    public static int getActionBarSize(Context context){
        final TypedArray styledAttributes = context.getTheme().obtainStyledAttributes(
                new int[] { android.R.attr.actionBarSize });
        int actionBarSize = (int) styledAttributes.getDimension(0, 0);
        styledAttributes.recycle();
        return actionBarSize;
    }

	/**
	 * Shows a toast in case no BT is detected
	 */
	public static void noBluetooth(Context context) {
		Util.createUpperToast(context, context.getString(R.string.bt_not_available), Toast.LENGTH_LONG);
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
