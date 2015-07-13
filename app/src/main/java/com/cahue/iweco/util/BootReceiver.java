package com.cahue.iweco.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cahue.iweco.activityRecognition.ActivityRecognitionService;

/**
 * Created by f.gatti.gomez on 13/07/15.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ActivityRecognitionService.startIfNecessary(context);
    }

}
