package com.cahue.iweco.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.MapsActivity;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


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
    public static final int NOTIFICATION_ID = 1;
    private static final String TAG = GcmIntentService.class.getSimpleName();
    NotificationCompat.Builder builder;
    private NotificationManager mNotificationManager;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(@NonNull Intent intent) {
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

                try {

                    String updatedCar = extras.getString(UPDATED_CAR);
                    if (updatedCar != null)
                        saveCar(URLDecoder.decode(updatedCar, "UTF-8"));

                    String deletedCarId = extras.getString(DELETED_CAR);
                    if (deletedCarId != null)
                        deleteCar(URLDecoder.decode(deletedCarId, "UTF-8"));

                } catch (JSONException | UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void saveCar(String carJson) throws JSONException {
        Car car = Car.fromJSON(new JSONObject(carJson));
        if (BuildConfig.DEBUG)
            sendNotification(car.toString());
        CarDatabase.getInstance().saveCarAndBroadcast(this, car);
    }

    private void deleteCar(String carId) {
        CarDatabase database = CarDatabase.getInstance();
        database.deleteCar(this, carId);
    }

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

