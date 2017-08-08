package com.cahue.iweco.locationservices;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.model.ParkingSpot;
import com.cahue.iweco.util.FetchAddressDelegate;
import com.cahue.iweco.util.PreferencesUtil;

import java.util.Date;
import java.util.List;

import static com.cahue.iweco.util.NotificationChannelsUtils.ACT_RECOG_CHANNEL_ID;

/**
 * This service fetches the current location and ask the user if the car was set there.
 * It is started from the activity recognition service, when it detects that the user steps out of
 * a vehicle.
 * <p>
 * When the location is retrieved the user gets a notification asking him whan car he has parked.
 *
 * @author Francesco
 */
public class PossibleParkedCarReceiver extends AbstractLocationUpdatesBroadcastReceiver {

    public static final String ACTION = BuildConfig.APPLICATION_ID + ".POSSIBLE_PARKED_CAR_ACTION";

    public static final int NOTIFICATION_ID = 875644;

    private final static String TAG = PossibleParkedCarReceiver.class.getSimpleName();

    private static final int ACCURACY_THRESHOLD_M = 50;

    private CarDatabase carDatabase;

    private Location location;

    private Date startTime;


    @Override
    protected void onPreciseFixPolled(final Context context, Location location, Bundle extras) {

        if (location.getAccuracy() > ACCURACY_THRESHOLD_M) return;

        this.location = location;
        this.startTime = (Date) extras.getSerializable(Constants.EXTRA_START_TIME);

        Log.i(TAG, "Received : " + location);

        carDatabase = CarDatabase.getInstance();

        Log.d(TAG, "Fetching address");
        FetchAddressDelegate fetchAddressDelegate = new FetchAddressDelegate();
        fetchAddressDelegate.fetch(context, location, new FetchAddressDelegate.Callbacks() {
            @Override
            public void onAddressFetched(String address) {
                PossibleParkedCarReceiver.this.onAddressFetched(context, address);
            }

            @Override
            public void onError(String error) {
            }
        });
    }


    /**
     * When the address is fetched, we display a notification asking the user to save the location,
     * assigning it to a car
     *
     * @param address
     */
    private void onAddressFetched(Context context, String address) {

        ParkingSpot possibleParkingSpot = new ParkingSpot(null, location, address, startTime, false);
        carDatabase.addPossibleParkingSpot(context, possibleParkingSpot);

        if (!PreferencesUtil.isMovementRecognitionNotificationEnabled(context))
            return;

        long[] pattern = {0, 100, 1000};

        // Intent to start the activity and show a just parked dialog
        Intent intent = new Intent(context, MapsActivity.class);
        intent.setAction(Constants.ACTION_POSSIBLE_PARKED_CAR);
        intent.putExtra(Constants.EXTRA_SPOT, possibleParkingSpot);
//        intent.putExtra("test", "not null");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 345345, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context, ACT_RECOG_CHANNEL_ID)
                        .setVibrate(pattern)
                        .setContentIntent(pendingIntent)
                        .setColor(context.getResources().getColor(R.color.theme_primary))
                        .setSmallIcon(R.drawable.ic_car_white_48dp)
                        .setContentTitle(context.getString(R.string.ask_just_parked))
                        .setContentText(address);

        List<Car> cars = carDatabase.retrieveCars(context, true);
        int numberActions = Math.min(cars.size(), 3);
        for (int i = 0; i < numberActions; i++) {
            Car car = cars.get(i);
            NotificationCompat.Action saveAction = createCarSaveAction(context, car, possibleParkingSpot, i);
            mBuilder.addAction(saveAction);
        }

        mNotifyMgr.notify(null, NOTIFICATION_ID, mBuilder.build());
    }


    @NonNull
    private NotificationCompat.Action createCarSaveAction(Context context, @NonNull Car car, ParkingSpot possibleSpot, int index) {

        Intent intent = new Intent(Constants.INTENT_SAVE_CAR_REQUEST + "." + index);
        intent.putExtra(Constants.EXTRA_CAR_ID, car.id);
        intent.putExtra(Constants.EXTRA_SPOT, possibleSpot);

        PendingIntent pIntent = PendingIntent.getBroadcast(context, 782982, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String name = car.isOther() ? context.getResources().getString(R.string.other) : car.name;
        return new NotificationCompat.Action(R.drawable.ic_car_white_24dp, name, pIntent);
    }

}
