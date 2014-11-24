package com.bahpps.cahue;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.text.format.DateUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bahpps.cahue.spots.ParkingSpot;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MarkerDetailsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MarkerDetailsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MarkerDetailsFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_SPOT = "arg_spot";

    private static ParkingSpot spot;

    private OnFragmentInteractionListener mListener;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param spot Parameter 1.
     * @return A new instance of fragment MarkerDetailsFragment.
     */
    public static MarkerDetailsFragment newInstance(ParkingSpot spot) {
        MarkerDetailsFragment fragment = new MarkerDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_SPOT, spot);
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

            // Set rectangle color
            ImageView rectangleImage = (ImageView) view.findViewById(R.id.spot_image);
            GradientDrawable gradientDrawable = (GradientDrawable) rectangleImage.getDrawable();
            gradientDrawable.setStroke(20, getResources().getColor(spot.getMarkerType().colorId));
        }
        return view;
    }

    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getActivity().getResources().getDisplayMetrics();
        return dp * displayMetrics.densityDpi ;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
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
