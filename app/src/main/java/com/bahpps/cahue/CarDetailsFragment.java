package com.bahpps.cahue;


import android.app.Activity;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CarDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CarDetailsFragment extends DetailsFragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_CAR_LOCATION = "car_Location";
    private static final String ARG_PARKING_TIME = "parking_time";

    private Location userLocation;
    private Location carLocation;
    private Date parkingTime;

    private OnCarPositionDeletedListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CarDetailsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static CarDetailsFragment newInstance(Location carLocation, Date parkingTime) {
        CarDetailsFragment fragment = new CarDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_CAR_LOCATION, carLocation);
        args.putSerializable(ARG_PARKING_TIME, parkingTime);
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
            carLocation = getArguments().getParcelable(ARG_CAR_LOCATION);
            parkingTime = (Date) getArguments().getSerializable(ARG_PARKING_TIME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_car_details, container, false);
        if (carLocation != null) {

            // Set time ago
            TextView timeAgo = (TextView) view.findViewById(R.id.time);
            timeAgo.setText(DateUtils.getRelativeTimeSpanString(parkingTime.getTime()));

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
                        mListener.onCarPositionDeleted();
                    }
                }
            });

        }
        return view;
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if(view != null) {
            TextView distance = (TextView) view.findViewById(R.id.distance);
            updateDistance(distance);
        }
    }

    private void updateDistance(TextView textView) {
        if (userLocation != null) {
            float distanceM = carLocation.distanceTo(userLocation);
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnCarPositionDeletedListener {

        public void onCarPositionDeleted();

    }


}
