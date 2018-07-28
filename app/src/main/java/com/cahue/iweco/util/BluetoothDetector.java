package com.cahue.iweco.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.R;
import com.cahue.iweco.activityrecognition.ActivityRecognitionService;
import com.cahue.iweco.locationservices.CarMovedReceiver;
import com.cahue.iweco.locationservices.LocationUpdatesService;
import com.cahue.iweco.locationservices.ParkedCarReceiver;
import com.cahue.iweco.locationservices.PossibleParkedCarReceiver;
import com.cahue.iweco.login.LoginActivity;
import com.cahue.iweco.model.Car;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.cahue.iweco.util.NotificationChannelsUtils.DEBUG_CHANNEL_ID;
import static com.cahue.iweco.util.NotificationChannelsUtils.JUST_PARKED_CHANNEL_ID;

/**
 * This receiver is in charge of detecting BT disconnection or connection, as declared on the manifest
 */
public class BluetoothDetector extends BroadcastReceiver {

    private static final String TAG = BluetoothDetector.class.getSimpleName();

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Create an Intent for the activity you want to start
            Intent loginIntent = new Intent(context, LoginActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addNextIntentWithParentStack(loginIntent);
            PendingIntent loginPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            long[] pattern = {0, 1000, 200, 1000};
            NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, JUST_PARKED_CHANNEL_ID) : new Notification.Builder(context))
                            .setContentIntent(loginPendingIntent)
                            .setVibrate(pattern)
                            .setSmallIcon(R.drawable.ic_exit_to_app_24dp_white)
                            .setContentTitle(context.getString(R.string.open_the_app))
                            .setContentText(context.getString(R.string.open_the_app_long));
            mNotifyMgr.notify("57934", 57934, mBuilder.build());
            return;
        }


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

            int state = intent.getExtras().getInt(BluetoothProfile.EXTRA_STATE);
            int previousState = intent.getExtras().getInt(BluetoothProfile.EXTRA_PREVIOUS_STATE);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("cars")
                    .whereEqualTo("owner", currentUser.getUid())
                    .whereEqualTo("bt_address", address)
                    .get()
                    .addOnSuccessListener((snapshot) -> {
                        for (DocumentSnapshot documentSnapshot : snapshot.getDocuments()) {
                            Log.d("Bluetooth", "storedAddress matched: " + address);
                            Car car = Car.fromFirestore(documentSnapshot);
                            if (car.btAddress.equals(address)) {

                                if (state == BluetoothProfile.STATE_DISCONNECTED && previousState == BluetoothProfile.STATE_CONNECTED) {
                                    onBtDisconnectedFromCar(context, car);
                                } else if (state == BluetoothProfile.STATE_CONNECTED && previousState == BluetoothProfile.STATE_DISCONNECTED) {
                                    onBtConnectedToCar(context, car);
                                }
                                break;
                            }
                        }

                    });
        }
    }

    private void onBtTurnedOn(Context context) {
    }

    private void onBtTurnedOff(Context context) {
        ActivityRecognitionService.startCheckingActivityRecognition(context);
    }

    private void onBtConnectedToCar(@NonNull Context context, @NonNull Car car) {

        /* Remove existing notification */
        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);
        mNotifyMgr.cancel(car.id, ParkedCarReceiver.NOTIFICATION_ID);

        /*
         * Stop activity recognition
         */
        context.stopService(new Intent(context, ActivityRecognitionService.class));

        Log.d("Bluetooth", "onBtConnectedToCar");

        /*
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
        LocationUpdatesService.startLocationUpdate(context,
                CarMovedReceiver.ACTION,
                car.id);

        if (BuildConfig.DEBUG) {
            long[] pattern = {0, 1000, 200, 1000};
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
        LocationUpdatesService.startLocationUpdate(context,
                ParkedCarReceiver.ACTION,
                car.id);

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
