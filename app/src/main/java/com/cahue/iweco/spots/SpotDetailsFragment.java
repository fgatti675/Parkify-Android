package com.cahue.iweco.spots;

import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.cahue.iweco.DetailsFragment;
import com.cahue.iweco.R;
import com.cahue.iweco.util.Util;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SpotDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SpotDetailsFragment extends DetailsFragment {

    // the fragment initialization parameters
    private static final String ARG_SPOT = "arg_spot";
    private static final String ARG_LOCATION = "arg_location";

    private Location userLocation;
    private ParkingSpot spot;

    private TextView timeAgo;
    private TextView distance;
    private ImageView rectangleImage;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param spot Parameter 1.
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    public static DetailsFragment newInstance(ParkingSpot spot, Location userLocation) {
        DetailsFragment fragment = new SpotDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        args.putParcelable(ARG_LOCATION, userLocation);
        fragment.setArguments(args);
        return fragment;
    }

    public SpotDetailsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            spot = getArguments().getParcelable(ARG_SPOT);
            userLocation = getArguments().getParcelable(ARG_LOCATION);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_spot_details, container, false);
        if (spot != null) {

            // Set time ago
            timeAgo = (TextView) view.findViewById(R.id.time);
            updateTimeAgo();

            // Update distance
            distance = (TextView) view.findViewById(R.id.distance_time);

            // Set rectangle color
            rectangleImage = (ImageView) view.findViewById(R.id.spot_image);

            Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
            view.startAnimation(fadeInAnimation);
        }
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateDistance();
        updateRectangle();
        updateTimeAgo();
    }

    @Override
    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        View view = getView();
        if (view != null) {
            updateDistance();
        }
    }

    private void updateRectangle() {
        GradientDrawable gradientDrawable = (GradientDrawable) rectangleImage.getDrawable();
        gradientDrawable.setStroke((int) Util.dpToPx(getActivity(), 8), getResources().getColor(spot.getMarkerType().colorId));
    }

    private void updateDistance() {
        if (userLocation != null) {
            Location spotLocation = new Location("spot");
            spotLocation.setLatitude(spot.position.latitude);
            spotLocation.setLongitude(spot.position.longitude);
            float distanceM = spotLocation.distanceTo(userLocation);
            distance.setText(String.format("%.1f km", distanceM / 1000));
        }
    }

    private void updateTimeAgo() {
        if (spot.future) {
            timeAgo.setText(R.string.aboutToLeave);
        } else {
            timeAgo.setText(DateUtils.getRelativeTimeSpanString(spot.time.getTime()));
        }
    }

}
