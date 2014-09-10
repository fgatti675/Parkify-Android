package com.bahpps.cahue.auxiliar;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.util.Log;

import com.bahpps.cahue.location.LocationPoller;

import java.util.GregorianCalendar;

public class BluetoothDetector extends BroadcastReceiver {


    public static final String PREF_BT_DEVICE_ADDRESS = "PREF_BT_DEVICE_ADDRESS";
    public static final String PREF_BT_DISCONNECTION_TIME = "PREF_BT_DISCONNECTION_TIME";
    public static final String BT_PREFS = "BT_PREFS";

    /**
	 * This receiver is in charge of detecting BT disconnection, as declared on the manifest
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		BluetoothDevice device;
		SharedPreferences prefs;
		String storedAddress;

		if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {

			device = (BluetoothDevice) intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

			Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
			Log.d("Bluetooth", device.getName() + " " + device.getAddress());

			// we need to get which BT device the user chose as the one of his car
			prefs = context.getSharedPreferences(BT_PREFS, Context.MODE_WORLD_READABLE);
			storedAddress = prefs.getString(PREF_BT_DEVICE_ADDRESS, "");

			// If the device we just disconnected from is our chosen one
			if (device.getAddress().equals(storedAddress)) {

				// we store the time the bt device was disconnected
				prefs.edit().putLong(PREF_BT_DISCONNECTION_TIME, (new GregorianCalendar().getTimeInMillis()))
						.commit();

				Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

				// we create an intent to start the location poller service, as declared in manifest
				Intent i = new Intent(context, LocationPoller.class);
				i.putExtra(LocationPoller.EXTRA_INTENT, new Intent(Util.INTENT_NEW_CAR_POS));
				i.putExtra(LocationPoller.EXTRA_PROVIDER,
						LocationManager.GPS_PROVIDER);
				context.sendBroadcast(i);

				// we send it twice, for gps and network provider
				i.putExtra(LocationPoller.EXTRA_PROVIDER,
						LocationManager.NETWORK_PROVIDER);
				context.sendBroadcast(i);
			}
		}
	}
}
