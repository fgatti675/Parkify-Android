package com.cahue.iweco.parkedcar;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cahue.iweco.Constants;
import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.ParkedCarDelegate;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.CarsSync;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.Tracking;

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

    @NonNull
    private String carId;

    private Car car;

    @Nullable
    private OnCarPositionDeletedListener mListener;
    private CarDatabase carDatabase;

    @NonNull
    private final BroadcastReceiver carUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, @NonNull Intent intent) {
            String carId = intent.getExtras().getString(Constants.EXTRA_CAR_ID);
            if (carId.equals(CarDetailsFragment.this.carId)) {
                Log.d(TAG, "Received car update request" + carId);
                car = carDatabase.findCar(getActivity(), carId);
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
    @NonNull
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

        carDatabase = CarDatabase.getInstance();

        if (getArguments() != null) {
            carId = getArguments().getString(ARG_CAR_ID);
        }
        parkedCarDelegate = (ParkedCarDelegate) getFragmentManager().findFragmentByTag(ParkedCarDelegate.getFragmentTag(carId));

        car = carDatabase.findCar(getActivity(), carId);
    }

    @Override
    public void onResume() {

        super.onResume();

        Log.d(TAG, "Register receiver");
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(carUpdatedReceiver, new IntentFilter(Constants.INTENT_CAR_UPDATED));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(carUpdatedReceiver, new IntentFilter(Constants.INTENT_ADDRESS_UPDATE));

        updateLayout();

    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(carUpdatedReceiver);
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
    public View onCreateView(@NonNull LayoutInflater inflater,
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
    public void onAttach(@NonNull Activity activity) {
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
    public boolean onMenuItemClick(@NonNull MenuItem menuItem) {
        // Handle presses on the action bar items
        switch (menuItem.getItemId()) {
            case R.id.action_follow:
                parkedCarDelegate.setCameraFollowing(true);
                updateFollowButtonState();
                return true;
            case R.id.action_share:
                shareCarLocation();
                return true;
            case R.id.action_clear:
                removeCarLocation();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void shareCarLocation() {
        Double latitude = car.location.getLatitude();
        Double longitude = car.location.getLongitude();

        String uri = String.format("http://maps.google.com/maps?q=loc:%s,%s(%s)", latitude, longitude, Uri.encode(car.name));

        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        CharSequence relativeTimeSpanString = DateUtils.getRelativeTimeSpanString(car.time.getTime());
        String shareString = car.name != null ?
                getResources().getString(R.string.car_was_here_share, car.name, relativeTimeSpanString) :
                getResources().getString(R.string.car_was_here, relativeTimeSpanString);
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, shareString);
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareString + "\n" + uri);

        startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share)));

        Tracking.sendEvent(Tracking.CATEGORY_MAP, Tracking.ACTION_CAR_LOCATION_SHARED);

    }

    /**
     * Interface for telling the containing activity that the car position has been deleted
     */
    public interface OnCarPositionDeletedListener {

        void onCarRemoved(String carId);

    }


}
