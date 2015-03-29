package com.cahue.iweco.util;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cahue.iweco.locationServices.CarMovedService;
import com.cahue.iweco.locationServices.LocationPollerService;
import com.cahue.iweco.locationServices.ParkedCarService;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;

import java.util.Set;

public class BluetoothDetector extends BroadcastReceiver {

    BluetoothDevice device;

    /**
     * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        String address = device.getAddress();
        String name = device.getName();

        Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
        Log.d("Bluetooth", device.getName() + " " + address);

        // we need to get which BT device the user chose as the one of his car

        CarDatabase carDatabase = CarDatabase.getInstance(context);
        Set<String> storedAddress = carDatabase.getPairedBTAddresses();

        // If the device we just disconnected from is our chosen one
        if (storedAddress.contains(address)) {

            Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

            Car car = carDatabase.findByBTAddress(address);

            if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
                onBtDisconnected(context, car);
            } else if (intent.getAction().equals(BluetoothDevice.ACTION_ACL_CONNECTED)) {
                onBtConnected(context, car);
            }
        }
    }

    public void onBtConnected(Context context, Car car) {

        Log.d("Bluetooth", "onBtConnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, CarMovedService.class);
        intent.putExtra(LocationPollerService.EXTRA_CAR, car.id);
        context.startService(intent);

    }

    public void onBtDisconnected(Context context, Car car) {

        Log.d("Bluetooth", "onBtDisconnected");

        // we create an intent to start the location poller service, as declared in manifest
        Intent intent = new Intent();
        intent.setClass(context, ParkedCarService.class);
        intent.putExtra(LocationPollerService.EXTRA_CAR, car.id);
        context.startService(intent);

    }
}
