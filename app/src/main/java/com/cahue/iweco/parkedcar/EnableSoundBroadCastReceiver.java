package com.cahue.iweco.parkedcar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.cahue.iweco.util.PreferencesUtil;

/**
 * Created by f.gatti.gomez on 01/12/2016.
 */
public class EnableSoundBroadCastReceiver extends BroadcastReceiver {

    public EnableSoundBroadCastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferencesUtil.setDisplayParkedSoundEnabled(context, true);
        Toast.makeText(context, "Test", Toast.LENGTH_LONG).show();
    }

}
