package com.bahpps.cahue.debug;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.bahpps.cahue.R;
import com.bahpps.cahue.locationServices.LocationPollerService;

import java.util.Date;


public class DebugActivity extends Activity implements DebugService.ServiceListener {


    DebugService mService;
    boolean mBound = false;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        ImageButton button = (ImageButton) findViewById(R.id.imageButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pollLocation();
            }
        });

        textView = (TextView) findViewById(R.id.text);
    }


    private void pollLocation(){
        Log.d("debug", "Debug poll location ");
        Intent intent = new Intent(this, DebugService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        textView.setText("Polling...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DebugService.LocalBinder binder = (DebugService.LocalBinder) service;
            mService = binder.getService();
            mService.setServiceListener(DebugActivity.this);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    @Override
    public void onNewLocation(Location location) {
        String string = location.toString();
        Date startTime = (Date) location.getExtras().getSerializable(LocationPollerService.EXTRA_START_TIME);
        string += "\n" + (System.currentTimeMillis() - startTime.getTime()) + "ms";
        Log.d("debug", "Debug new location " + string);
        textView.setText(string);
    }
}
