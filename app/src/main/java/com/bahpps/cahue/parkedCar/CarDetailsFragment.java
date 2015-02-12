package com.bahpps.cahue.parkedCar;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bahpps.cahue.DetailsFragment;
import com.bahpps.cahue.R;
import com.bahpps.cahue.cars.Car;
import com.bahpps.cahue.util.CarImages;


/**
 * Use the {@link CarDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CarDetailsFragment extends DetailsFragment implements Toolbar.OnMenuItemClickListener {

    // the fragment initialization parameter
    private static final String ARG_CAR = "car";

    private Location userLocation;
    private Car car;

    private OnCarPositionDeletedListener mListener;

    TextView name;
    TextView time;
    TextView distance;

    Toolbar toolbar;
    private ImageView image;

    ParkedCarDelegate parkedCarDelegate;

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

        //update image
        updateImage();

        // update time ago
        updateTimeTextView();

        // Update distance
        updateDistance();

        updateFollowButtonState();

    }


    private void updateImage() {
        if (car.color != null) {
            image.setImageDrawable(getResources().getDrawable(CarImages.getImageResourceId(car.color, getActivity())));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_car_details, container, false);

        // Set time ago
        name = (TextView) view.findViewById(R.id.name);
        name.setText(car.name != null ? car.name : getResources().getString(R.string.car));

        image = (ImageView) view.findViewById(R.id.car_image);

        time = (TextView) view.findViewById(R.id.time);

        distance = (TextView) view.findViewById(R.id.distance);

        Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
        view.startAnimation(fadeInAnimation);

        toolbar = (Toolbar) view.findViewById(R.id.car_toolbar);

        toolbar.inflateMenu(R.menu.parked_car_menu);
        toolbar.setOnMenuItemClickListener(this);
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

    private void updateTimeTextView() {
        // Set time ago
        time.setText(DateUtils.getRelativeTimeSpanString(car.time.getTime()));
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            updateDistance();
        }
    }

    private void updateDistance() {
        if (userLocation != null && car.location != null) {
            float distanceM = car.location.distanceTo(userLocation);
            distance.setText(String.format("%.1f km", distanceM / 1000));
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

        MenuItem item = toolbar.getMenu().findItem(R.id.action_follow);
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
