package com.cahue.iweco.cars;


import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.cahue.iweco.R;

/**
 * This Activity appears as a dialog. It lists any paired devices and devices detected in the area after discovery. When
 * a device is chosen by the user, the MAC address of the device is sent back to the parent Activity in the result
 * Intent.
 */
public class CarManagerActivity extends ActionBarActivity
        implements CarManagerFragment.DeviceSelectionLoadingListener,
        EditCarDialog.CarEditedListener {

    private static final String TAG = CarManagerActivity.class.getSimpleName();

    private ProgressBar progressBar;
    private CarManagerFragment carFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_car_manager);

        /**
         * Get an instance of the fragment
         */
        carFragment = (CarManagerFragment) getFragmentManager().findFragmentById(R.id.car_manager_fragment);
//
//        /**
//         * There is no saved instance so we create a few things
//         */
//        if (savedInstanceState == null) {
//
//            // First incarnation of this activity.
//            carFragment.setRetainInstance(true);
//
//        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        ViewCompat.setElevation(toolbar, 5);
        toolbar.setTitle(R.string.cars);
        toolbar.setNavigationIcon(R.drawable.ic_action_navigation_arrow_back);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) toolbar.findViewById(R.id.progress_bar);

        // animation
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);

        boolean loading = carFragment.areDevicesBeingLoaded();
        Log.d(TAG, "Devices being loaded on create: " + loading);
        devicesBeingLoaded(loading);
    }


    @Override
    protected void onPause() {
        super.onPause();
        //closing transition animations
        overridePendingTransition(R.anim.activity_open_scale, R.anim.activity_close_translate);
    }


    @Override
    public void devicesBeingLoaded(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onCarEdited(Car car, boolean newCar) {
        carFragment.onCarEdited(car, newCar);
    }
}
