package com.bahpps.cahue;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bahpps.cahue.spots.ParkingSpot;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MarkerDetailsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MarkerDetailsFragment#create} factory method to
 * create an instance of this fragment.
 */
public class MarkerDetailsFragment extends Fragment {

    // the fragment initialization parameters
    private static final String ARG_SPOT = "arg_spot";
    private static final String ARG_LOCATION = "arg_location";

    private Location userLocation;
    private ParkingSpot spot;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param spot Parameter 1.
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    public static MarkerDetailsFragment create(ParkingSpot spot, Location userLocation) {
        MarkerDetailsFragment fragment = new MarkerDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
        args.putParcelable(ARG_LOCATION, userLocation);
        fragment.setArguments(args);
        return fragment;
    }

    public MarkerDetailsFragment() {
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
        View view = inflater.inflate(R.layout.fragment_marker_details, container, false);
        if (spot != null) {

            // Set time ago
            TextView timeAgo = (TextView) view.findViewById(R.id.time);
            timeAgo.setText(DateUtils.getRelativeTimeSpanString(spot.getTime().getTime()));

            // Update distance
            TextView distance = (TextView) view.findViewById(R.id.distance);
            updateDistance(distance);

            // Set rectangle color
            ImageView rectangleImage = (ImageView) view.findViewById(R.id.spot_image);
            GradientDrawable gradientDrawable = (GradientDrawable) rectangleImage.getDrawable();
            gradientDrawable.setStroke((int) dpToPx(8), getResources().getColor(spot.getMarkerType().colorId));

            Animation fadeInAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
            view.startAnimation(fadeInAnimation);
        }
        return view;
    }

    public float dpToPx(int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public void setUserLocation(Location userLocation) {
        this.userLocation = userLocation;
        TextView distance = (TextView) getView().findViewById(R.id.distance);
        updateDistance(distance);
    }

    private void updateDistance(TextView textView) {
        if (userLocation != null) {
            Location spotLocation = new Location("spot");
            spotLocation.setLatitude(spot.getPosition().latitude);
            spotLocation.setLongitude(spot.getPosition().longitude);
            float distanceM = spotLocation.distanceTo(userLocation);
            textView.setText(String.format("%.1f km", distanceM / 1000));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

}
