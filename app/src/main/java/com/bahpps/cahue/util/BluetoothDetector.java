package com.bahpps.cahue.util;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.bahpps.cahue.locationServices.CarMovedService;
import com.bahpps.cahue.locationServices.ParkedCarService;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

public class BluetoothDetector extends BroadcastReceiver {


    BluetoothDevice device;

    /**
     * This receiver is in charge of detecting BT disconnection, as declared on the manifest
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
        Log.d("Bluetooth", device.getName() + " " + device.getAddress());

        // we need to get which BT device the user chose as the one of his car

        Set<String> storedAddress = Util.getPairedDevices(context);

        // If the device we just disconnected from is our chosen one
        if (storedAddress.contains(device.getAddress())) {

            Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                onBtDisconnected(context);
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                onBtConnected(context);
            }
        }
    }

    public void onBtConnected(Context context) {

        Log.d("Bluetooth", "onBtConnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent i = new Intent();
        i.setClass(context,CarMovedService.class);
        context.startService(i);


    }

    public void onBtDisconnected(Context context) {

        Log.d("Bluetooth", "onBtDisconnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent i = new Intent();
        i.setClass(context,ParkedCarService.class);
        context.startService(i);

    }
}
