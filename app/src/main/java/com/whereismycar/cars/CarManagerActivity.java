package com.whereismycar.cars;


import android.os.Bundle;
import androidx.core.view.ViewCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.whereismycar.R;
import com.whereismycar.model.Car;
import com.whereismycar.util.Tracking;

/**
 * This activity is in charge of wrapping a {@link com.whereismycar.cars.CarManagerFragment}
 */
public class CarManagerActivity
        extends AppCompatActivity
        implements CarManagerFragment.Callbacks,
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        toolbar.setTitle(R.string.cars);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        ViewCompat.setElevation(toolbar, getResources().getDimension(R.dimen.elevation));
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) toolbar.findViewById(R.id.progress_bar);

        boolean loading = carFragment.areDevicesBeingLoaded();
        Log.d(TAG, "Devices being loaded on create: " + loading);
        devicesBeingLoaded(loading);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Tracking.sendView(Tracking.CATEGORY_CAR_MANAGER);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //closing transition animations
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
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
