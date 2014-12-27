package com.bahpps.cahue.util;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.bahpps.cahue.locationServices.CarMovedService;
import com.bahpps.cahue.locationServices.LocationPollerService;
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

        String address = device.getAddress();
        String name = device.getName();

        Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
        Log.d("Bluetooth", device.getName() + " " + address);

        // we need to get which BT device the user chose as the one of his car

        Set<String> storedAddress = Util.getPairedDevices(context);

        // If the device we just disconnected from is our chosen one
        if (storedAddress.contains(address)) {

            Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                onBtDisconnected(context, address, name);
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                onBtConnected(context, address, name);
            }
        }
    }

    public void onBtConnected(Context context, String address, String name) {

        Log.d("Bluetooth", "onBtConnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, CarMovedService.class);
        intent.putExtra(LocationPollerService.EXTRA_BT_ID, address);
        intent.putExtra(LocationPollerService.EXTRA_BT_NAME, name);
        context.startService(intent);

    }

    public void onBtDisconnected(Context context, String address, String name) {

        Log.d("Bluetooth", "onBtDisconnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, ParkedCarService.class);
        intent.putExtra(LocationPollerService.EXTRA_BT_ID, address);
        intent.putExtra(LocationPollerService.EXTRA_BT_NAME, name);
        context.startService(intent);

    }
}
