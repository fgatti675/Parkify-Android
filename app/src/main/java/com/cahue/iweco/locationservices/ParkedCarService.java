package com.cahue.iweco.locationservices;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.Toast;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
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
public class ParkedCarService extends AbstractLocationUpdatesService {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".PARKED_CAR_ACTION";

    public static final int NOTIFICATION_ID = 4833;

    private final static String TAG = ParkedCarService.class.getSimpleName();

    private CarDatabase carDatabase;


    @Override
    protected void onPreciseFixPolled(Location location, String carId, Date startTime) {

        carDatabase = CarDatabase.getInstance();

        Date now = new Date();

        Log.d(TAG, "Fetching address");
        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(this, location, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                // Display the address string
                // or an error message sent from the intent service.
                carDatabase.updateCarLocation(carId, location, address, now, "bt_event", new CarDatabase.CarUpdateListener() {
                    @Override
                    public void onCarUpdated(Car car) {
                        Log.d(TAG, "Sending car update broadcast");
                        notifyUser(car);
                    }

                    @Override
                    public void onCarUpdateError() {

                    }
                });

            }

            @Override
            public void onError(String error) {
                carDatabase.updateCarLocation(carId, location, null, now, "bt_event", new CarDatabase.CarUpdateListener() {
                    @Override
                    public void onCarUpdated(Car car) {
                        notifyUser(car);
                    }

                    @Override
                    public void onCarUpdateError() {

                    }
                });
            }
        });

        /**
         * If the location of the car is good enough we can set a geofence afterwards.
         */
        if (location.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            GeofenceCarService.startDelayedGeofenceService(this, carId);
        }


        Tracking.sendEvent(Tracking.CATEGORY_PARKING, Tracking.ACTION_BLUETOOTH_PARKING);

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        Bundle bundle = new Bundle();
        bundle.putString("car", carId);
        firebaseAnalytics.logEvent("bt_car_parked", bundle);

        Log.i(TAG, "Location polled update");

    }


    private void notifyUser(Car car) {

        if (PreferencesUtil.isDisplayParkedNotificationEnabled(this)) {

            NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);

            // Intent to start the activity and show a just parked dialog
            Intent intent = new Intent(this, MapsActivity.class);
            intent.setAction(ACTION_VIEW);
            intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 79243, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            CharSequence title;
            if (car.name != null) {
                Spannable sb = new SpannableString(car.name + " - " + this.getString(R.string.location_stored));
                sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, car.name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                title = sb;
            } else {
                title = this.getString(R.string.location_stored);
            }

            Notification.Builder mBuilder = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? new Notification.Builder(this, NotificationChannelsUtils.JUST_PARKED_CHANNEL_ID) : new Notification.Builder(this))
                    .setContentIntent(pendingIntent)
                    .setColor(this.getResources().getColor(R.color.theme_primary))
                    .setSmallIcon(R.drawable.ic_car_white_48dp)
                    .setContentTitle(title);

            if (car.address != null)
                mBuilder = mBuilder.setContentText(car.address);

            if (PreferencesUtil.isDisplayParkedSoundEnabled(this)) {

                long[] pattern = {0, 100, 200, 200};
                mBuilder
                        .setVibrate(pattern)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

            } else {

                mBuilder.setPriority(PRIORITY_MIN);
            }

            mNotifyMgr.notify(car.id, NOTIFICATION_ID, mBuilder.build());

        } else {
            Util.showBlueToast(this, this.getString(R.string.car_location_registered, car.name), Toast.LENGTH_SHORT);
        }

    }
}