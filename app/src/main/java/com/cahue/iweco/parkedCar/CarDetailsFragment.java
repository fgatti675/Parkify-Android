package com.cahue.iweco.parkedCar;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.Car;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.database.CarDatabase;


/**
 * Use the {@link CarDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CarDetailsFragment extends DetailsFragment implements Toolbar.OnMenuItemClickListener {

    public static final String TAG = CarDetailsFragment.class.getSimpleName();

    // the fragment initialization parameter
    private static final String ARG_CAR = "car";

    private Location userLocation;
    private Car car;

    private OnCarPositionDeletedListener mListener;

    CarViewHolder carViewHolder;

    ParkedCarDelegate parkedCarDelegate;

    private BroadcastReceiver carUpdatedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Car receivedCar = (Car) intent.getExtras().get(CarDatabase.INTENT_CAR_EXTRA);
            if (car.equals(receivedCar)) {
                Log.d(TAG, "Received car " + receivedCar);
                car = receivedCar;
                update();
            }
        }

    };

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static CarDetailsFragment newInstance(Car car) {
        CarDetailsFragment fragment = new CarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CAR, car);
        fragment.setArguments(args);
        return fragment;
    }

    public CarDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments() != null) {
            car = getArguments().getParcelable(ARG_CAR);
        }
        parkedCarDelegate = (ParkedCarDelegate) getFragmentManager().findFragmentByTag(car.id);
    }

    @Override
    public void onResume() {

        super.onResume();

        Log.d(TAG, "Register receiver");
        getActivity().registerReceiver(carUpdatedReceiver, new IntentFilter(CarDatabase.INTENT_CAR_UPDATE));

        update();

    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(carUpdatedReceiver);
    }

    private void update() {

        carViewHolder.bind(getActivity(), car, userLocation, BluetoothAdapter.getDefaultAdapter());

        updateFollowButtonState();
    }


    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_car_details, container, false);

        carViewHolder = new CarViewHolder(view);

        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
        view.startAnimation(fadeInAnimation);

        carViewHolder.toolbar.inflateMenu(R.menu.parked_car_menu);
        carViewHolder.toolbar.setOnMenuItemClickListener(this);

        return view;
    }

    private void showClearDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.remove_car_confirm)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (mListener != null) {
                            mListener.onCarPositionDeleted(car);
                        }
                        parkedCarDelegate.removeCar();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        // Create the AlertDialog object and return it
        builder.create().show();
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            carViewHolder.updateDistance(userLocation, car.location);
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
    public void onCameraUpdate(boolean justFinishedAnimating) {
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
                parkedCarDelegate.setFollowing(true);
                updateFollowButtonState();
                return true;
            case R.id.action_clear:
                showClearDialog();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    /**
     * Interface for telling the containing activity that the car position has been deleted
     */
    public interface OnCarPositionDeletedListener {

        public void onCarPositionDeleted(Car car);

    }


}
