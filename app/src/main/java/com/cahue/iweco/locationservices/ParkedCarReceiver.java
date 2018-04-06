package com.cahue.iweco.locationservices;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.FetchAddressDelegate;
import com.cahue.iweco.util.NotificationChannelsUtils;
import com.cahue.iweco.util.PreferencesUtil;
import com.cahue.iweco.util.Tracking;
import com.cahue.iweco.util.Util;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Date;

import static android.app.Notification.PRIORITY_MIN;
import static android.content.Intent.ACTION_VIEW;

/**
 * This class is in charge of receiving a location fix when the user parks his car.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarReceiver extends AbstractLocationUpdatesBroadcastReceiver {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".PARKED_CAR_ACTION";

    public static final int NOTIFICATION_ID = 4833;

    private final static String TAG = ParkedCarReceiver.class.getSimpleName();

    private Car car;
    private CarDatabase carDatabase;


    private void notifyUser(Context context) {
        if (PreferencesUtil.isDisplayParkedNotificationEnabled(context)) {

            NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);

            // Intent to start the activity and show a just parked dialog
            Intent intent = new Intent(context, MapsActivity.class);
            intent.setAction(ACTION_VIEW);
            intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(context, 79243, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            CharSequence title;
            if (car.name != null) {
                Spannable sb = new SpannableString(car.name + " - " + context.getString(R.string.just_parked));
                sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, car.name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                title = sb;
            } else {
                title = context.getString(R.string.just_parked);
            }

            Notification.Builder mBuilder =
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(context, NotificationChannelsUtils.JUST_PARKED_CHANNEL_ID) : new Notification.Builder(context))
                            .setContentIntent(pendingIntent)
                            .setColor(context.getResources().getColor(R.color.theme_primary))
                            .setSmallIcon(R.drawable.ic_car_white_48dp)
                            .setContentTitle(title)
                            .setContentText(car.address);

            if (PreferencesUtil.isDisplayParkedSoundEnabled(context)) {

                long[] pattern = {0, 100, 200, 200};
                mBuilder
                        .setVibrate(pattern)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

            } else {

                mBuilder.setPriority(PRIORITY_MIN);
            }

            mNotifyMgr.notify(car.id, NOTIFICATION_ID, mBuilder.build());

        } else {
            Util.showBlueToast(context, context.getString(R.string.car_location_registered, car.name), Toast.LENGTH_SHORT);
        }
    }

    private void fetchAddress(@NonNull final Context context, @NonNull final Car car) {
        Log.d(TAG, "Fetching address");
        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(context, car.location, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                // Display the address string
                // or an error message sent from the intent service.
                car.address = address;
                carDatabase.updateAddress(context, car);

                Log.d(TAG, "Sending car update broadcast");

                Intent intent = new Intent(Constants.INTENT_ADDRESS_UPDATE);
                intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
                intent.putExtra(Constants.EXTRA_CAR_ADDRESS, car.address);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                notifyUser(context);
            }

            @Override
            public void onError(String error) {
            }
        });
    }

    @Override
    protected void onPreciseFixPolled(Context context, Location location, Bundle extras) {
        carDatabase = CarDatabase.getInstance();
        String carId = extras.getString(Constants.EXTRA_CAR_ID, null);
        this.car = carDatabase.findCar(context, carId);

        Log.i(TAG, "Received : " + location);

        car.spotId = null;
        car.location = location;
        car.address = null;
        car.time = new Date();

        CarsSync.storeCar(carDatabase, context, car);

        /**
         * If the location of the car is good enough we can set a geofence afterwards.
         */
        if (car.location.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            GeofenceCarReceiver.startDelayedGeofenceService(context, car.id);
        }

        fetchAddress(context, car);

        Tracking.sendEvent(Tracking.CATEGORY_PARKING, Tracking.ACTION_BLUETOOTH_PARKING);

        Tracking.sendEvent(Tracking.CATEGORY_PARKING, Tracking.ACTION_BLUETOOTH_FREED_SPOT);
        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putString("car", carId);
        firebaseAnalytics.logEvent("bt_car_parked", bundle);
    }

}
