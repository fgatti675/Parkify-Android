package com.cahue.iweco.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.cahue.iweco.activityrecognition.ActivityRecognitionService;

/**
 * Created by f.gatti.gomez on 13/07/15.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = BootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        ActivityRecognitionService.startIfNoBT(context);
    }

}
