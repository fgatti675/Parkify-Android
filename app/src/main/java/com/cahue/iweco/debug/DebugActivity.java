package com.cahue.iweco.debug;

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
import android.widget.TextView;

import com.cahue.iweco.R;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.locationServices.LocationPollerService;
import com.cahue.iweco.cars.Car;

import java.util.Date;


public class DebugActivity extends Activity implements ServiceListener {


    DebugCarMovedService mMovedService;
    DebugParkedCarService mParkedService;
    boolean mCarMovedBound = false;
    boolean mCarParkedBound = false;
    TextView locationTextView;
    TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        findViewById(R.id.park).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carParked();
            }
        });

        findViewById(R.id.driveOff).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                carMoved();
            }
        });

        locationTextView = (TextView) findViewById(R.id.locationText);
        resultTextView = (TextView) findViewById(R.id.resultText);

    }


    private void carMoved() {
        Log.d("debug", "Car moved");
        Intent intent = new Intent(this, DebugCarMovedService.class);

        Car car = CarDatabase.getInstance(this).retrieveCars(false).iterator().next(); // TODO

        intent.putExtra(LocationPollerService.EXTRA_CAR, car);
        bindService(intent, mCarMovedConnection, Context.BIND_AUTO_CREATE);
        locationTextView.setText("Polling...");
    }

    private void carParked() {
        Log.d("debug", "Car parked");
        Intent intent = new Intent(this, DebugParkedCarService.class);

        Car car = CarDatabase.getInstance(this).retrieveCars(false).iterator().next(); // TODO

        intent.putExtra(LocationPollerService.EXTRA_CAR, car);
        bindService(intent, mCarParkedConnection, Context.BIND_AUTO_CREATE);
        locationTextView.setText("Polling...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mCarMovedBound) {
            unbindService(mCarMovedConnection);
            mCarMovedBound = false;
        }

        if (mCarParkedBound) {
            unbindService(mCarParkedConnection);
            mCarParkedBound = false;
        }
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mCarMovedConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DebugCarMovedService.LocalBinder binder = (DebugCarMovedService.LocalBinder) service;
            mMovedService = binder.getService();
            mMovedService.setServiceListener(DebugActivity.this);
            mCarMovedBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCarMovedBound = false;
        }
    };

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mCarParkedConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            DebugParkedCarService.LocalBinder binder = (DebugParkedCarService.LocalBinder) service;
            mParkedService = binder.getService();
            mParkedService.setServiceListener(DebugActivity.this);
            mCarParkedBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mCarParkedBound = false;
        }
    };


    @Override
    public void onNewLocation(Location location) {
        String string = location.toString();
        Date startTime = (Date) location.getExtras().getSerializable(LocationPollerService.EXTRA_START_TIME);
        string += "\n" + (System.currentTimeMillis() - startTime.getTime()) + "ms";
        Log.d("debug", "Debug new location " + string);
        locationTextView.setText(string);
    }

    @Override
    public void onLocationPost() {
        Log.d("debug", "Debug new post");
        resultTextView.setText("Posted");
    }
}
