package com.cahue.iweco.parkedCar;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cahue.iweco.Constants;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.ParkedCarDelegate;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;


/**
 * Use the {@link CarDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CarDetailsFragment extends DetailsFragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = CarDetailsFragment.class.getSimpleName();

    // the fragment initialization parameter
    private static final String ARG_CAR_ID = "car_ID";
    CarViewHolder carViewHolder;
    ParkedCarDelegate parkedCarDelegate;
    private Location userLocation;
    private String carId;
    private Car car;
    private OnCarPositionDeletedListener mListener;
    private CarDatabase carDatabase;

    private BroadcastReceiver carUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String carId = intent.getExtras().getString(Constants.INTENT_CAR_EXTRA_ID);
            if (carId.equals(CarDetailsFragment.this.carId)) {
                Log.d(TAG, "Received car update request" + carId);
                car = carDatabase.find(carId);
                updateLayout();
            }
        }

    };

    public CarDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static CarDetailsFragment newInstance(String carId) {
        CarDetailsFragment fragment = new CarDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CAR_ID, carId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        carDatabase = CarDatabase.getInstance(getActivity());

        if (getArguments() != null) {
            carId = getArguments().getString(ARG_CAR_ID);
        }
        parkedCarDelegate = (ParkedCarDelegate) getFragmentManager().findFragmentByTag(carId);

        car = carDatabase.find(carId);
    }

    @Override
    public void onResume() {

        super.onResume();

        Log.d(TAG, "Register receiver");
        getActivity().registerReceiver(carUpdatedReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATED));
        getActivity().registerReceiver(carUpdatedReceiver, new IntentFilter(Constants.INTENT_ADDRESS_UPDATE));

        updateLayout();

    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(carUpdatedReceiver);
    }

    private void updateLayout() {

        if (car == null) {
            mListener.onCarRemoved(carId);
            return;
        }

        carViewHolder.bind(getActivity(), car, userLocation, BluetoothAdapter.getDefaultAdapter());

        updateFollowButtonState();
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.layout_car_details, container, false);

        carViewHolder = new CarViewHolder(view);

        carViewHolder.toolbar.inflateMenu(R.menu.parked_car_menu);
        carViewHolder.toolbar.setOnMenuItemClickListener(this);

        return view;
    }

    private void removeCarLocation() {
        if (mListener != null) {
            mListener.onCarRemoved(carId);
        }
        CarsSync.clearLocation(carDatabase, getActivity(), car);
        parkedCarDelegate.removeCar();
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            carViewHolder.updateDistance(getActivity(), userLocation, car.location);
        }
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCarPositionDeletedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCameraUpdate() {
        updateFollowButtonState();
    }

    private void updateFollowButtonState() {

        boolean selected = parkedCarDelegate.isFollowing();

        MenuItem item = carViewHolder.toolbar.getMenu().findItem(R.id.action_follow);
        item.setIcon(getResources().getDrawable(
                selected
                        ? R.drawable.ic_action_maps_navigation_accent
                        : R.drawable.ic_action_maps_navigation));

    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        // Handle presses on the action bar items
        switch (menuItem.getItemId()) {
            case R.id.action_follow:
                parkedCarDelegate.setCameraFollowing(true);
                updateFollowButtonState();
                return true;
            case R.id.action_clear:
                removeCarLocation();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    /**
     * Interface for telling the containing activity that the car position has been deleted
     */
    public interface OnCarPositionDeletedListener {

        void onCarRemoved(String carId);

    }


}
