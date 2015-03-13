package com.cahue.iweco.locationServices;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.cahue.iweco.BuildConfig;
import com.cahue.iweco.Constants;
import com.cahue.iweco.activityRecognition.ActivityRecognitionIntentService;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.cars.CarsSync;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.Date;

/**
 * This class is in charge of receiving location updates, after and store it as the cars position.
 * Triggered when BT is disconnected
 *
 * @author Francesco
 */
public class ParkedCarService extends LocationPollerService {

    private final static String TAG = "ParkedCarService";

    private PendingIntent pIntent;

    @Override
    protected boolean checkPreconditions(Car car) {
        return true;
    }

    @Override
    public void onLocationPolled(Context context, Location location, Car car) {

        Log.i(TAG, "Received : " + location);

        car.location = location;
        car.address = null;
        car.time = new Date();

        CarDatabase carDatabase = CarDatabase.getInstance(context);
        CarsSync.storeCar(carDatabase, context, car);

        /**
         * If the car was parked accurately we check if the user gets far enough walking after it
         */
        if (location.getAccuracy() < Constants.ACCURACY_THRESHOLD_M) {
            Intent intent = new Intent(this, ActivityRecognitionIntentService.class);
            pIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(getGoogleApiClient(), 5000, pIntent);
        }

    }

    public class ActivityRecognitionIntentService extends IntentService {

        private String TAG = this.getClass().getSimpleName();

        public static final String RECEIVER = BuildConfig.APPLICATION_ID + ".GET_ACTIVITY";
        public static final String DETECTED_ACTIVITY_DATA_KEY = BuildConfig.APPLICATION_ID + RECEIVER + ".DETECTED_ACTIVITY_DATA_KEY";

        public ActivityRecognitionIntentService() {
            super("My Activity Recognition Service");
        }

        protected ResultReceiver mReceiver;

        @Override
        protected void onHandleIntent(Intent intent) {

            ResultReceiver resultReceiver;

            mReceiver = intent.getParcelableExtra(RECEIVER);

            if (ActivityRecognitionResult.hasResult(intent)) {

            }

        }

        private void deliverResultToReceiver(int resultCode, DetectedActivity detectedActivity) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(DETECTED_ACTIVITY_DATA_KEY, detectedActivity);
            mReceiver.send(resultCode, bundle);
        }

    }


}
