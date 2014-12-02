package com.bahpps.cahue;


import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.bahpps.cahue.parkedCar.Car;
import com.bahpps.cahue.parkedCar.ParkedCarDelegate;

import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CarDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CarDetailsFragment extends DetailsFragment {

    // the fragment initialization parameter
    private static final String ARG_CAR = "car";
    private static final String ARG_CAR_DELEGATE = "car_delegate";

    private Location userLocation;
    private Car car;
    private ParkedCarDelegate parkedCarDelegate;

    private OnCarPositionDeletedListener mListener;

    private ImageButton follow;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    public static CarDetailsFragment newInstance(Car car, ParkedCarDelegate parkedCarDelegate) {
        CarDetailsFragment fragment = new CarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CAR, car);
        args.putParcelable(ARG_CAR_DELEGATE, parkedCarDelegate);
        fragment.setArguments(args);
        return fragment;
    }

    public CarDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            car = getArguments().getParcelable(ARG_CAR);
            parkedCarDelegate = getArguments().getParcelable(ARG_CAR_DELEGATE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_car_details, container, false);
        if (car != null) {

            // Set time ago
            TextView name = (TextView) view.findViewById(R.id.name);
            name.setText(car.name);

            // Set time ago
            TextView timeAgo = (TextView) view.findViewById(R.id.time);
            timeAgo.setText(DateUtils.getRelativeTimeSpanString(car.time.getTime()));

            // Update distance
            TextView distance = (TextView) view.findViewById(R.id.distance);
            updateDistance(distance);

            Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
            view.startAnimation(fadeInAnimation);

            ImageButton clear = (ImageButton) view.findViewById(R.id.clear);
            clear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onCarPositionDeleted(car);
                    }
                }
            });

            follow = (ImageButton) view.findViewById(R.id.follow);
            follow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        mListener.onFollowingClicked(car);
                        updateFollowButtonState();
                    }

                }
            });

        }
        return view;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            TextView distance = (TextView) view.findViewById(R.id.distance);
            updateDistance(distance);
        }
    }

    private void updateDistance(TextView textView) {
        if (userLocation != null && car.location != null) {
            float distanceM = car.location.distanceTo(userLocation);
            textView.setText(String.format("%.1f km", distanceM / 1000));
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
        boolean enabled = parkedCarDelegate.getMode() == ParkedCarDelegate.Mode.FOLLOWING;
        follow.setSelected(enabled);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnCarPositionDeletedListener {

        public void onFollowingClicked(Car car);

        public void onCarPositionDeleted(Car car);

    }


}
