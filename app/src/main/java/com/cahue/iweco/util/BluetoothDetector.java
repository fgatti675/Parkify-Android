package com.cahue.iweco.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.activityrecognition.ActivityRecognitionService;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationservices.CarMovedReceiver;
import com.cahue.iweco.locationservices.LocationUpdatesHelper;
import com.cahue.iweco.locationservices.ParkedCarReceiver;
import com.cahue.iweco.model.Car;

import java.util.Calendar;
import java.util.Set;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.cahue.iweco.util.NotificationChannelsUtils.DEBUG_CHANNEL_ID;

/**
 * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
 */
public class BluetoothDetector extends BroadcastReceiver {

    private static final String TAG = BluetoothDetector.class.getSimpleName();

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            if (state == BluetoothAdapter.STATE_ON) {
                onBtTurnedOn(context);
            } else if (state == BluetoothAdapter.STATE_OFF) {
                onBtTurnedOff(context);
            }
        } else if (intent.getAction().equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                || intent.getAction().equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device == null) return;

            String address = device.getAddress();
            String name = device.getName();

            Log.d("Bluetooth", "Bluetooth: " + intent.getAction());
            Log.d("Bluetooth", device.getName() + " " + address);

            // we need to get which BT device the user chose as the one of his car

            CarDatabase carDatabase = CarDatabase.getInstance();
            Set<String> storedAddress = carDatabase.getPairedBTAddresses(context);

            // If the device we just disconnected from is our chosen one
            if (storedAddress.contains(address)) {

                Log.d("Bluetooth", "storedAddress matched: " + storedAddress);

                Car car = carDatabase.findCarByBTAddress(context, address);
                int state = intent.getExtras().getInt(BluetoothProfile.EXTRA_STATE);
                if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    onBtDisconnectedFromCar(context, car);
                } else if (state == BluetoothProfile.STATE_CONNECTED) {
                    onBtConnectedToCar(context, car);
                }
            }
        }
    }

    private void onBtTurnedOn(Context context) {
    }

    private void onBtTurnedOff(Context context) {
        ActivityRecognitionService.startCheckingActivityRecognition(context);
    }

    private void onBtConnectedToCar(@NonNull Context context, @NonNull Car car) {

        /**
         * Stop activity recognition
         */
        context.stopService(new Intent(context, ActivityRecognitionService.class));

        Log.d("Bluetooth", "onBtConnectedToCar");

        /**
         * Check if the car was parked long enough
         */
        if (!BuildConfig.DEBUG) {
            long now = Calendar.getInstance().getTimeInMillis();
            if (car.time != null) {
                long parkingTime = car.time.getTime();
                boolean result = now - parkingTime > Constants.MINIMUM_STAY_MS;
                if (!result) {
                    Log.w(TAG, "Preconditions failed");
                    return;
                }
            }
        }

        // start the CarMovedReceiver
        LocationUpdatesHelper helper = new LocationUpdatesHelper(context, CarMovedReceiver.ACTION);
        Bundle extras = new Bundle();
        extras.putString(Constants.EXTRA_CAR_ID, car.id);
        helper.startLocationUpdates(extras);


        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 1000, 200, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, DEBUG_CHANNEL_ID) : new Notification.Builder(context))
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.com_facebook_tooltip_blue_topnub)
                            .setContentTitle("BT connected to car: " + car.name);
            int id = (int) (Math.random() * 10000);
            mNotifyMgr.notify("" + id, id, mBuilder.build());
        }

    }

    private void onBtDisconnectedFromCar(@NonNull Context context, @NonNull Car car) {

        Log.d("Bluetooth", "onBtDisconnectedFromCar");

        // we create an intent to start the location poller service, as declared in manifest
        LocationUpdatesHelper helper = new LocationUpdatesHelper(context, ParkedCarReceiver.ACTION);
        Bundle extras = new Bundle();
        extras.putString(Constants.EXTRA_CAR_ID, car.id);
        helper.startLocationUpdates(extras);

        /**
         * Start activity recognition if required
         */
        ActivityRecognitionService.startCheckingActivityRecognition(context);

        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 1000, 200, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, DEBUG_CHANNEL_ID) : new Notification.Builder(context))
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.com_facebook_tooltip_blue_bottomnub)
                            .setContentTitle("BT disconnected from car: " + car.name);
            int id = (int) (Math.random() * 10000);
            mNotifyMgr.notify("" + id, id, mBuilder.build());
        }
    }
}
