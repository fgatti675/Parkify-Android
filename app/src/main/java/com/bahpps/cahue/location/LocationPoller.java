package com.bahpps.cahue.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * BroadcastReceiver to be launched by AlarmManager. Simply passes the work over to LocationPollerService, who arranges
 * to make sure the WakeLock stuff is done properly.
 */
public class LocationPoller extends BroadcastReceiver {
	public static final String EXTRA_ERROR = "com.bahpps.cahue.EXTRA_ERROR";
	public static final String EXTRA_INTENT = "com.bahpps.cahue.EXTRA_INTENT";
	public static final String EXTRA_LOCATION = "com.bahpps.cahue.EXTRA_LOCATION";
	public static final String EXTRA_PROVIDER = "com.bahpps.cahue.EXTRA_PROVIDER";
	public static final String EXTRA_LASTKNOWN = "com.bahpps.cahue.EXTRA_LASTKNOWN";

	/**
	 * Standard entry point for a BroadcastReceiver. Delegates the event to LocationPollerService for processing.
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("LocationPoller", "Location request received");
		LocationPollerService.requestLocation(context, intent);
	}
}
