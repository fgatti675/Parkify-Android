package com.cahue.iweco.parkedcar;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.ParkedCarDelegate;
import com.cahue.iweco.R;
import com.cahue.iweco.cars.CarViewHolder;
import com.cahue.iweco.cars.database.CarDatabase;
import com.cahue.iweco.model.Car;
import com.cahue.iweco.util.Tracking;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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

    @Nullable
    private OnCarPositionDeletedListener mListener;
    private CarDatabase carDatabase;

    @Nullable
    private Car car;

    private ListenerRegistration listenerRegistration;

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

        DocumentReference carReference = FirebaseFirestore.getInstance().collection("cars").document(carId);
        listenerRegistration = carReference.addSnapshotListener((documentSnapshot, e) -> {
            if (documentSnapshot == null) return;
            car = Car.fromFirestore(documentSnapshot);
            updateLayout(car);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        listenerRegistration.remove();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateLayout(Car car) {

        if (!isAdded()) return;

        if (car == null) {
            mListener.onCarRemoved(carId);
            return;
        }

        carViewHolder.bind(getActivity(), car, userLocation, BluetoothAdapter.getDefaultAdapter());
        if (car.location != null)
            carViewHolder.toolbar.getMenu().findItem(R.id.action_share).setVisible(true);

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
        carViewHolder.toolbar.getMenu().findItem(R.id.action_share).setVisible(false);

        return view;
    }

    private void removeCarLocation() {
        if (mListener != null) {
            mListener.onCarRemoved(carId);
        }
        carDatabase.removeCarLocation(carId);
        parkedCarDelegate.removeCar();
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null && car != null) {
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

        if (car == null || car.location == null) return;

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


        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(getActivity());
        Bundle bundle = new Bundle();
        bundle.putString("car", carId);
        firebaseAnalytics.logEvent("share_car_location", bundle);


    }

    /**
     * Interface for telling the containing activity that the car position has been deleted
     */
    public interface OnCarPositionDeletedListener {

        void onCarRemoved(String carId);

    }


}
