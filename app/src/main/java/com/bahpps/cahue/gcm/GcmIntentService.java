package com.bahpps.cahue.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.bahpps.cahue.MapsActivity;
import com.bahpps.cahue.R;
import com.bahpps.cahue.cars.Car;
import com.bahpps.cahue.cars.CarDatabase;
import com.bahpps.cahue.cars.CarsSync;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;
import java.util.Set;

/**
 * Created by Francesco on 15/01/2015.
 */
public class GcmIntentService extends IntentService {

    /**
     * Tell a Device that
     */
    public static final String CARS_UPDATE = "CARS_UPDATE";

    /**
     * Message parameters
     */
    public static final String CARS = "CARS";

    private static final String TAG = GcmIntentService.class.getSimpleName();

    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());

                try {

                    String carsJson = extras.getString(CARS);
                    saveCars(carsJson);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Post notification of received message.
                sendNotification("Received: " + extras.toString());
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void saveCars(String carsJson) throws JSONException {
        Set<Car> cars = Car.fromJSONArray(new JSONArray(carsJson));
        CarDatabase.getInstance(this).saveCars(cars);

        for (Car car : cars) {
            /**
             * Tell everyone else
             */
            Intent carUpdateIntent = new Intent(CarsSync.INTENT_CAR_UPDATE);
            carUpdateIntent.putExtra(CarsSync.INTENT_CAR_EXTRA, car);
            sendBroadcast(carUpdateIntent);
        }
    }

    public static final int NOTIFICATION_ID = 1;

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MapsActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_gcm)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}

