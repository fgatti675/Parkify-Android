package com.cahue.iweco.activityrecognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.cahue.iweco.Constants;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationservices.PossibleParkedCarService;
import com.cahue.iweco.model.PossibleSpot;
import com.cahue.iweco.util.Tracking;
import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Broadcast receiver in charge of saving a car, after if is requested by a action taken from a
 * notification
 */
public class SaveCarRequestReceiver extends BroadcastReceiver {

    public static final String TAG = SaveCarRequestReceiver.class.getSimpleName();

    public SaveCarRequestReceiver() {
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {

        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(context);
        mNotifyMgr.cancel(PossibleParkedCarService.NOTIFICATION_ID);

        Log.d(TAG, "onReceive: ");

        Tracking.sendEvent(Tracking.CATEGORY_NOTIFICATION_ACT_RECOG, Tracking.ACTION_CAR_SELECTED);

        String carId = intent.getExtras().getString(Constants.EXTRA_CAR_ID);

        PossibleSpot possibleSpot = intent.getExtras().getParcelable(Constants.EXTRA_SPOT);

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        Bundle bundle = new Bundle();
        bundle.putString("car", carId);
        firebaseAnalytics.logEvent("ar_notification_car_selected", bundle);

        CarDatabase database = CarDatabase.getInstance();

        database.updateCarFromArSpot(context, carId, possibleSpot);


    }
}
