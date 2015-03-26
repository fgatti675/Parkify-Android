package com.cahue.iweco.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

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
    public static final String UPDATED_CAR = "UPDATED_CAR";
    public static final String DELETED_CAR = "DELETED_CAR";

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
//                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
//                sendNotification("Deleted messages on server: " +
//                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());

                try {

                    String updatedCar = extras.getString(UPDATED_CAR);
                    if (updatedCar != null)
                        saveCar(updatedCar);

                    String deletedCarId = extras.getString(DELETED_CAR);
                    if (deletedCarId != null)
                        deleteCar(deletedCarId);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Post notification of received message.
//                sendNotification("Received: " + extras.toString());
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void saveCar(String carJson) throws JSONException {
        Car car = Car.fromJSON(new JSONObject(carJson));
        CarDatabase.getInstance(this).saveAndBroadcast(car);
    }

    private void deleteCar(String carId) throws JSONException {
        CarDatabase database = CarDatabase.getInstance(this);
        database.delete(carId);
    }

    public static final int NOTIFICATION_ID = 1;

//    // Put the message into a notification and post it.
//    // This is just one simple example of what you might choose to do with
//    // a GCM message.
//    private void sendNotification(String msg) {
//        mNotificationManager = (NotificationManager)
//                this.getSystemService(Context.NOTIFICATION_SERVICE);
//
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MapsActivity.class), 0);
//
//        NotificationCompat.Builder mBuilder =
//                new NotificationCompat.Builder(this)
//                        .setSmallIcon(R.drawable.ic_stat_gcm)
//                        .setContentTitle("GCM Notification")
//                        .setStyle(new NotificationCompat.BigTextStyle()
//                                .bigText(msg))
//                        .setContentText(msg);
//
//        mBuilder.setContentIntent(contentIntent);
//        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
//    }
}

